import 'dotenv/config' 
import express from 'express'
import mysql from 'mysql2/promise'
import cors from 'cors'
import path from 'node:path'
import fs from 'node:fs/promises'
import fssync from 'node:fs'
import https from 'node:https'

// Basic config from env
const PORT = parseInt(process.env.PORT || '3001', 10)
const DB_HOST = process.env.DB_HOST || '127.0.0.1'
const DB_PORT = parseInt(process.env.DB_PORT || '3306', 10)
const DB_USER = process.env.DB_USER || 'root'
const DB_PASSWORD = process.env.DB_PASSWORD || ''
const DB_NAME = process.env.DB_NAME || 'solacecore'
const SKIN_TTL_DAYS = parseInt(process.env.SKIN_TTL_DAYS || '30', 10)
const SKIN_TTL_MS = SKIN_TTL_DAYS * 24 * 60 * 60 * 1000
const CACHE_ROOT = path.resolve(process.cwd(), 'server', 'cache', 'skins')

// Create a MySQL/MariaDB pool
const pool = mysql.createPool({
  host: "127.0.0.1",
  port: "3306",
  user: "root",
  password: "",
  database: "solacecore",
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
  // Return Date objects; we'll convert in JS
  dateStrings: false,
})

const app = express()

// If you access API from a different origin without Vite proxy, enable CORS
if (process.env.ENABLE_CORS === '1') {
  app.use(cors())
}

app.get('/api/health', async (_req, res) => {
  try {
    const [rows] = await pool.query('SELECT 1 AS ok')
    res.json({ ok: true, db: rows?.[0]?.ok === 1 })
  } catch (e) {
    res.status(500).json({ ok: false, error: String(e?.message || e) })
  }
})

// GET /api/players -> [{ uuid, name }]
app.get('/api/players', async (_req, res) => {
  try {
    const [rows] = await pool.query(
      'SELECT uuid, name FROM players ORDER BY name ASC'
    )
    res.json(rows)
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// GET /api/players/:uuid/punishments
app.get('/api/players/:uuid/punishments', async (req, res) => {
  const { uuid } = req.params
  try {
    const [playerRows] = await pool.query(
      'SELECT name FROM players WHERE uuid = ? LIMIT 1',
      [uuid]
    )
    const player = Array.isArray(playerRows) ? playerRows[0] : null
    if (!player) {
      return res.status(404).json({ error: 'Hráč nenalezen' })
    }
    const playerName = player.name

    const [punRows] = await pool.query(
      'SELECT reason, operator, punishmentType, start FROM punishments WHERE player_name = ? ORDER BY start DESC',
      [playerName]
    )

    const items = (punRows || []).map((r) => ({
      type: r.punishmentType,
      reason: r.reason ?? 'No reason specified',
      date: r.start instanceof Date ? r.start.toISOString() : r.start,
      operator: r.operator ?? '-',
    }))

    res.json(items)
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// GET /api/stats -> { bansToday, totalBans, totalPunishments }
app.get('/api/stats', async (_req, res) => {
  try {
    const [[{ totalPunishments }]] = await pool.query(
      'SELECT COUNT(*) AS totalPunishments FROM punishments'
    )

    const [[{ totalBans }]] = await pool.query(
      "SELECT COUNT(*) AS totalBans FROM punishments WHERE punishmentType IN ('ban','tempban')"
    )

    const [[{ bansToday }]] = await pool.query(
      "SELECT COUNT(*) AS bansToday FROM punishments WHERE punishmentType IN ('ban','tempban') AND DATE(`start`) = CURDATE()"
    )

    res.json({
      bansToday: Number(bansToday) || 0,
      totalBans: Number(totalBans) || 0,
      totalPunishments: Number(totalPunishments) || 0,
    })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// Simple sanitizer for file paths
function sanitizeId(id) {
  return String(id).replace(/[^a-zA-Z0-9_-]/g, '_')
}

async function ensureDir(dir) {
  if (!fssync.existsSync(dir)) {
    await fs.mkdir(dir, { recursive: true })
  }
}

async function fetchBuffer(url) {
  if (typeof fetch === 'function') {
    const r = await fetch(url)
    if (!r.ok) {
      const err = new Error(`HTTP ${r.status}`)
      err.status = r.status
      throw err
    }
    const ab = await r.arrayBuffer()
    return Buffer.from(ab)
  }
  // Fallback for older Node.js without global fetch
  return await new Promise((resolve, reject) => {
    https.get(url, (res) => {
      if (res.statusCode && res.statusCode >= 400) {
        reject(new Error(`HTTP ${res.statusCode}`))
        res.resume()
        return
      }
      const chunks = []
      res.on('data', (c) => chunks.push(c))
      res.on('end', () => resolve(Buffer.concat(chunks)))
    }).on('error', reject)
  })
}

// Track ongoing refreshes to prevent duplicate upstream requests per id
const inflight = new Map()

// GET /api/skins/:id/bust -> PNG image (cached for 30 days on disk)
app.get('/api/skins/:id/bust', async (req, res) => {
  const { id } = req.params
  const force = req.query.force === '1' || req.query.force === 'true'
  const safeId = sanitizeId(id)
  const dir = path.join(CACHE_ROOT, safeId)
  const filePath = path.join(dir, 'bust.png')

  try {
    await ensureDir(dir)

    // Serve from cache if fresh and not forced
    if (!force && fssync.existsSync(filePath)) {
      try {
        const st = await fs.stat(filePath)
        const age = Date.now() - st.mtimeMs
        if (age < SKIN_TTL_MS && st.size > 0) {
          res.setHeader('Content-Type', 'image/png')
          res.setHeader('Cache-Control', 'public, max-age=86400') // 1 day browser cache
          return res.sendFile(filePath)
        }
      } catch {}
    }

    // Fetch from upstream and (re)cache with in-flight dedupe
    const doRefresh = async () => {
      const upstream = `https://starlightskins.lunareclipse.studio/render/ultimate/${encodeURIComponent(id)}/bust`
      let buf
      try {
        buf = await fetchBuffer(upstream)
      } catch (err) {
        // If we have a stale file, serve it as a fallback
        if (fssync.existsSync(filePath)) {
          res.setHeader('Content-Type', 'image/png')
          res.setHeader('Cache-Control', 'public, max-age=3600') // 1h for stale
          return res.sendFile(filePath)
        }
        throw err
      }
      await fs.writeFile(filePath, buf)
    }

    if (inflight.has(safeId)) {
      try {
        await inflight.get(safeId)
      } catch {}
    } else {
      const p = doRefresh()
      inflight.set(safeId, p)
      try {
        await p
      } finally {
        inflight.delete(safeId)
      }
    }

    res.setHeader('Content-Type', 'image/png')
    res.setHeader('Cache-Control', 'public, max-age=86400')
    return res.sendFile(filePath)
  } catch (e) {
    // On error, try serving existing cache as best effort
    if (fssync.existsSync(filePath)) {
      res.setHeader('Content-Type', 'image/png')
      res.setHeader('Cache-Control', 'public, max-age=3600')
      return res.sendFile(filePath)
    }
    return res.status(500).json({ error: 'Failed to get skin', detail: String(e?.message || e) })
  }
})

app.listen(PORT, () => {
  // eslint-disable-next-line no-console
  console.log(`API server listening on http://localhost:${PORT}`)
})
