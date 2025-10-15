import 'dotenv/config'
import express from 'express'
import mysql from 'mysql2/promise'
import cors from 'cors'

// Basic config from env
const PORT = parseInt(process.env.PORT || '3001', 10)
const DB_HOST = process.env.DB_HOST || '127.0.0.1'
const DB_PORT = parseInt(process.env.DB_PORT || '3306', 10)
const DB_USER = process.env.DB_USER || 'root'
const DB_PASSWORD = process.env.DB_PASSWORD || ''
const DB_NAME = process.env.DB_NAME || 'solacecore'

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

app.listen(PORT, () => {
  // eslint-disable-next-line no-console
  console.log(`API server listening on http://localhost:${PORT}`)
})
