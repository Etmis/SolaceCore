import 'dotenv/config' 
import express from 'express'
import mysql from 'mysql2/promise'
import cors from 'cors'
import path from 'node:path'
import fs from 'node:fs/promises'
import fssync from 'node:fs'
import https from 'node:https'
import { fileURLToPath } from 'node:url'
import bcrypt from 'bcryptjs'
import jwt from 'jsonwebtoken'
import WebSocket from 'ws'

// Basic config from env
const PORT = parseInt(process.env.PORT || '3001', 10)
const WS_HOST = process.env.WS_HOST || '127.0.0.1'
const WS_PORT = parseInt(process.env.WS_PORT || '8082', 10)
const DB_HOST = process.env.DB_HOST || '127.0.0.1'
const DB_PORT = parseInt(process.env.DB_PORT || '3306', 10)
const DB_USER = process.env.DB_USER || 'root'
const DB_PASSWORD = process.env.DB_PASSWORD || ''
const DB_NAME = process.env.DB_NAME || 'solacecore'
const JWT_SECRET = process.env.JWT_SECRET || 'change-this-secret-in-production'
const SKIN_TTL_DAYS = parseInt(process.env.SKIN_TTL_DAYS || '30', 10)
const SKIN_TTL_MS = SKIN_TTL_DAYS * 24 * 60 * 60 * 1000
const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const SERVER_ROOT = __dirname
const CACHE_ROOT = path.join(SERVER_ROOT, 'cache', 'skins')
const DEFAULT_STEVE_PATH = path.join(SERVER_ROOT, 'assets', 'steve.png')
const FRONTEND_DIST = process.env.FRONTEND_DIST || path.join(SERVER_ROOT, '..', 'dist')
const SPA_INDEX_PATH = path.join(FRONTEND_DIST, 'index.html')

// WebSocket klient pro komunikaci s Minecraft serverem
let minecraftWs = null
const wsUrl = `ws://${WS_HOST}:${WS_PORT}`

function connectToMinecraft() {
  try {
    minecraftWs = new WebSocket(wsUrl)
    
    minecraftWs.on('open', () => {
      console.log(`Connected to Minecraft server at ${wsUrl}`)
    })
    
    minecraftWs.on('error', (error) => {
      console.error('WebSocket error:', error.message)
    })
    
    minecraftWs.on('close', () => {
      console.log('Disconnected from Minecraft server, reconnecting in 5s...')
      setTimeout(connectToMinecraft, 5000)
    })

    minecraftWs.on('message', (data) => {
      try {
        const message = JSON.parse(data)
        // Zpracovat zprávy ze serveru (notifikace o akcích atd.)
        console.log('Message from Minecraft:', message)
      } catch (e) {
        console.error('Error parsing message from Minecraft:', e)
      }
    })
  } catch (e) {
    console.error('Failed to connect to Minecraft WebSocket:', e.message)
    setTimeout(connectToMinecraft, 5000)
  }
}

function sendToMinecraft(message) {
  if (minecraftWs && minecraftWs.readyState === WebSocket.OPEN) {
    minecraftWs.send(JSON.stringify(message))
  } else {
    console.warn('WebSocket to Minecraft is not connected')
  }
}

// Create a MySQL/MariaDB pool
const pool = mysql.createPool({
  host: DB_HOST,
  port: DB_PORT,
  user: DB_USER,
  password: DB_PASSWORD,
  database: DB_NAME,
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
  // Return Date objects; we'll convert in JS
  dateStrings: false,
})

const app = express()

// Middleware for parsing JSON
app.use(express.json())

// If you access API from a different origin without Vite proxy, enable CORS
if (process.env.ENABLE_CORS === '1') {
  app.use(cors())
}

// Serve built frontend files when available
app.use(express.static(FRONTEND_DIST, { index: false }))

// ========================================
// AUTH MIDDLEWARE
// ========================================

// Middleware pro ověření JWT tokenu
async function authenticateModerator(req, res, next) {
  const authHeader = req.headers.authorization
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'No token provided' })
  }

  const token = authHeader.substring(7)
  try {
    const decoded = jwt.verify(token, JWT_SECRET)
    // Načíst moderátora z databáze
    const [modRows] = await pool.query(
      'SELECT id, username, is_active FROM moderators WHERE id = ? LIMIT 1',
      [decoded.id]
    )
    
    const moderator = Array.isArray(modRows) ? modRows[0] : null
    if (!moderator || !moderator.is_active) {
      return res.status(401).json({ error: 'Invalid token or inactive user' })
    }

    // Načíst oprávnění moderátora ze všech jeho rolí
    const [roleRows] = await pool.query(
      `SELECT r.permissions FROM roles r
       INNER JOIN moderator_roles mr ON r.id = mr.role_id
       WHERE mr.moderator_id = ?`,
      [moderator.id]
    )

    // Sloučit oprávnění ze všech rolí
    const permissions = {}
    for (const row of roleRows) {
      const rolePerms = typeof row.permissions === 'string' 
        ? JSON.parse(row.permissions) 
        : row.permissions
      Object.assign(permissions, rolePerms)
    }

    req.moderator = {
      id: moderator.id,
      username: moderator.username,
      permissions
    }
    next()
  } catch (err) {
    return res.status(401).json({ error: 'Invalid token' })
  }
}

// Middleware pro kontrolu oprávnění
function requirePermission(permission) {
  return (req, res, next) => {
    if (!req.moderator.permissions[permission]) {
      return res.status(403).json({ error: 'Insufficient permissions' })
    }
    next()
  }
}

// ========================================
// AUTH ENDPOINTS
// ========================================

// POST /api/auth/login - Přihlášení moderátora
app.post('/api/auth/login', async (req, res) => {
  const { username, password } = req.body
  
  if (!username || !password) {
    return res.status(400).json({ error: 'Username and password required' })
  }

  try {
    const [modRows] = await pool.query(
      'SELECT id, username, password_hash, is_active FROM moderators WHERE username = ? LIMIT 1',
      [username]
    )

    const moderator = Array.isArray(modRows) ? modRows[0] : null
    if (!moderator) {
      return res.status(401).json({ error: 'Invalid credentials' })
    }

    if (!moderator.is_active) {
      return res.status(401).json({ error: 'Account is inactive' })
    }

    // Ověření hesla
    const isValid = await bcrypt.compare(password, moderator.password_hash)
    if (!isValid) {
      return res.status(401).json({ error: 'Invalid credentials' })
    }

    // Vytvoření JWT tokenu
    const token = jwt.sign(
      { id: moderator.id, username: moderator.username },
      JWT_SECRET,
      { expiresIn: '24h' }
    )

    res.json({
      token,
      moderator: {
        id: moderator.id,
        username: moderator.username
      }
    })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// GET /api/auth/me - Získání informací o přihlášeném moderátorovi
app.get('/api/auth/me', authenticateModerator, async (req, res) => {
  res.json({
    id: req.moderator.id,
    username: req.moderator.username,
    permissions: req.moderator.permissions
  })
})

// ========================================
// MODERATOR ACTION ENDPOINTS
// ========================================

// POST /api/mod/ban - Permanentní ban hráče
app.post('/api/mod/ban', authenticateModerator, requirePermission('ban'), async (req, res) => {
  const { playerName, reason } = req.body
  
  if (!playerName) {
    return res.status(400).json({ error: 'Player name required' })
  }

  try {
    // Vložit do tabulky punishments
    await pool.query(
      `INSERT INTO punishments (player_name, reason, operator, punishmentType, start, isActive) 
       VALUES (?, ?, ?, 'ban', NOW(), 1)`,
      [playerName, reason || 'No reason specified', req.moderator.username]
    )

    // Zaznamenat akci
    await pool.query(
      `INSERT INTO mod_actions (moderator_id, action_type, target_player, reason) 
       VALUES (?, 'ban', ?, ?)`,
      [req.moderator.id, playerName, reason || 'No reason specified']
    )

    // Poslat akci na Minecraft server přes WebSocket
    sendToMinecraft({
      action: 'ban',
      playerName: playerName,
      reason: reason || 'No reason specified',
      moderator: req.moderator.username
    })

    res.json({ success: true, message: `Player ${playerName} has been banned` })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// POST /api/mod/tempban - Dočasný ban hráče
app.post('/api/mod/tempban', authenticateModerator, requirePermission('ban'), async (req, res) => {
  const { playerName, reason, duration } = req.body
  
  if (!playerName || !duration) {
    return res.status(400).json({ error: 'Player name and duration required' })
  }

  try {
    const durationMs = parseInt(duration, 10)
    const endDate = new Date(Date.now() + durationMs)

    await pool.query(
      `INSERT INTO punishments (player_name, reason, operator, punishmentType, start, \`end\`, duration, isActive) 
       VALUES (?, ?, ?, 'tempban', NOW(), ?, ?, 1)`,
      [playerName, reason || 'No reason specified', req.moderator.username, endDate, durationMs]
    )

    await pool.query(
      `INSERT INTO mod_actions (moderator_id, action_type, target_player, reason, duration) 
       VALUES (?, 'tempban', ?, ?, ?)`,
      [req.moderator.id, playerName, reason || 'No reason specified', durationMs]
    )

    // Poslat akci na Minecraft server přes WebSocket
    sendToMinecraft({
      action: 'tempban',
      playerName: playerName,
      reason: reason || 'No reason specified',
      duration: durationMs,
      moderator: req.moderator.username
    })

    res.json({ success: true, message: `Player ${playerName} has been temp-banned` })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// POST /api/mod/unban - Odbanování hráče
app.post('/api/mod/unban', authenticateModerator, requirePermission('unban'), async (req, res) => {
  const { playerName } = req.body
  
  if (!playerName) {
    return res.status(400).json({ error: 'Player name required' })
  }

  try {
    // Deaktivovat všechny aktivní bany
    await pool.query(
      `UPDATE punishments SET isActive = 0 
       WHERE player_name = ? AND punishmentType IN ('ban', 'tempban') AND isActive = 1`,
      [playerName]
    )

    await pool.query(
      `INSERT INTO mod_actions (moderator_id, action_type, target_player, reason) 
       VALUES (?, 'unban', ?, 'Unbanned')`,
      [req.moderator.id, playerName]
    )

    // Poslat akci na Minecraft server přes WebSocket
    sendToMinecraft({
      action: 'unban',
      playerName: playerName,
      moderator: req.moderator.username
    })

    res.json({ success: true, message: `Player ${playerName} has been unbanned` })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// POST /api/mod/warn - Varování hráče
app.post('/api/mod/warn', authenticateModerator, requirePermission('warn'), async (req, res) => {
  const { playerName, reason } = req.body
  
  if (!playerName) {
    return res.status(400).json({ error: 'Player name required' })
  }

  try {
    await pool.query(
      `INSERT INTO punishments (player_name, reason, operator, punishmentType, start, isActive) 
       VALUES (?, ?, ?, 'warn', NOW(), 1)`,
      [playerName, reason || 'No reason specified', req.moderator.username]
    )

    await pool.query(
      `INSERT INTO mod_actions (moderator_id, action_type, target_player, reason) 
       VALUES (?, 'warn', ?, ?)`,
      [req.moderator.id, playerName, reason || 'No reason specified']
    )

    // Poslat akci na Minecraft server přes WebSocket
    sendToMinecraft({
      action: 'warn',
      playerName: playerName,
      reason: reason || 'No reason specified',
      moderator: req.moderator.username
    })

    res.json({ success: true, message: `Player ${playerName} has been warned` })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// POST /api/mod/kick - Vykopnutí hráče
app.post('/api/mod/kick', authenticateModerator, requirePermission('kick'), async (req, res) => {
  const { playerName, reason } = req.body
  
  if (!playerName) {
    return res.status(400).json({ error: 'Player name required' })
  }

  try {
    await pool.query(
      `INSERT INTO punishments (player_name, reason, operator, punishmentType, start, isActive) 
       VALUES (?, ?, ?, 'kick', NOW(), 0)`,
      [playerName, reason || 'No reason specified', req.moderator.username]
    )

    await pool.query(
      `INSERT INTO mod_actions (moderator_id, action_type, target_player, reason) 
       VALUES (?, 'kick', ?, ?)`,
      [req.moderator.id, playerName, reason || 'No reason specified']
    )

    // Poslat akci na Minecraft server přes WebSocket
    sendToMinecraft({
      action: 'kick',
      playerName: playerName,
      reason: reason || 'No reason specified',
      moderator: req.moderator.username
    })

    res.json({ success: true, message: `Player ${playerName} has been kicked` })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// POST /api/mod/mute - Umlčení hráče
app.post('/api/mod/mute', authenticateModerator, requirePermission('mute'), async (req, res) => {
  const { playerName, reason, duration } = req.body
  
  if (!playerName) {
    return res.status(400).json({ error: 'Player name required' })
  }

  try {
    if (duration) {
      const durationMs = parseInt(duration, 10)
      const endDate = new Date(Date.now() + durationMs)
      
      await pool.query(
        `INSERT INTO punishments (player_name, reason, operator, punishmentType, start, \`end\`, duration, isActive) 
         VALUES (?, ?, ?, 'mute', NOW(), ?, ?, 1)`,
        [playerName, reason || 'No reason specified', req.moderator.username, endDate, durationMs]
      )
    } else {
      await pool.query(
        `INSERT INTO punishments (player_name, reason, operator, punishmentType, start, isActive) 
         VALUES (?, ?, ?, 'mute', NOW(), 1)`,
        [playerName, reason || 'No reason specified', req.moderator.username]
      )
    }

    await pool.query(
      `INSERT INTO mod_actions (moderator_id, action_type, target_player, reason, duration) 
       VALUES (?, 'mute', ?, ?, ?)`,
      [req.moderator.id, playerName, reason || 'No reason specified', duration || null]
    )

    // Poslat akci na Minecraft server přes WebSocket
    sendToMinecraft({
      action: 'mute',
      playerName: playerName,
      reason: reason || 'No reason specified',
      duration: duration ? parseInt(duration, 10) : null,
      moderator: req.moderator.username
    })

    res.json({ success: true, message: `Player ${playerName} has been muted` })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// POST /api/mod/unmute - Zrušení umlčení hráče
app.post('/api/mod/unmute', authenticateModerator, requirePermission('unmute'), async (req, res) => {
  const { playerName } = req.body
  
  if (!playerName) {
    return res.status(400).json({ error: 'Player name required' })
  }

  try {
    await pool.query(
      `UPDATE punishments SET isActive = 0 
       WHERE player_name = ? AND punishmentType = 'mute' AND isActive = 1`,
      [playerName]
    )

    await pool.query(
      `INSERT INTO mod_actions (moderator_id, action_type, target_player, reason) 
       VALUES (?, 'unmute', ?, 'Unmuted')`,
      [req.moderator.id, playerName]
    )

    // Poslat akci na Minecraft server přes WebSocket
    sendToMinecraft({
      action: 'unmute',
      playerName: playerName,
      moderator: req.moderator.username
    })

    res.json({ success: true, message: `Player ${playerName} has been unmuted` })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// ========================================
// ROLE MANAGEMENT ENDPOINTS
// ========================================

// GET /api/roles - Seznam všech rolí
app.get('/api/roles', authenticateModerator, async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT id, name, permissions, created_at FROM roles ORDER BY name ASC')
    
    const roles = rows.map(r => ({
      id: r.id,
      name: r.name,
      permissions: typeof r.permissions === 'string' ? JSON.parse(r.permissions) : r.permissions,
      createdAt: r.created_at
    }))

    res.json(roles)
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// POST /api/roles - Vytvoření nové role
app.post('/api/roles', authenticateModerator, requirePermission('manageRoles'), async (req, res) => {
  const { name, permissions } = req.body
  
  if (!name || !permissions) {
    return res.status(400).json({ error: 'Name and permissions required' })
  }

  try {
    const [result] = await pool.query(
      'INSERT INTO roles (name, permissions) VALUES (?, ?)',
      [name, JSON.stringify(permissions)]
    )

    res.json({ success: true, id: result.insertId, message: `Role ${name} created` })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// PUT /api/roles/:id - Úprava oprávnění role
app.put('/api/roles/:id', authenticateModerator, requirePermission('manageRoles'), async (req, res) => {
  const { id } = req.params
  const { name, permissions } = req.body
  
  if (!name && !permissions) {
    return res.status(400).json({ error: 'Name or permissions required' })
  }

  try {
    const updates = []
    const values = []
    
    if (name) {
      updates.push('name = ?')
      values.push(name)
    }
    
    if (permissions) {
      updates.push('permissions = ?')
      values.push(JSON.stringify(permissions))
    }
    
    values.push(id)
    
    await pool.query(
      `UPDATE roles SET ${updates.join(', ')} WHERE id = ?`,
      values
    )

    res.json({ success: true, message: 'Role updated' })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// DELETE /api/roles/:id - Smazání role
app.delete('/api/roles/:id', authenticateModerator, requirePermission('manageRoles'), async (req, res) => {
  const { id } = req.params

  try {
    await pool.query('DELETE FROM roles WHERE id = ?', [id])
    res.json({ success: true, message: 'Role deleted' })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// GET /api/moderators - Seznam moderátorů
app.get('/api/moderators', authenticateModerator, requirePermission('manageRoles'), async (req, res) => {
  try {
    const [rows] = await pool.query(
      'SELECT id, username, created_at, is_active FROM moderators ORDER BY username ASC'
    )
    res.json(rows)
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// GET /api/moderators/:id/roles - Role moderátora
app.get('/api/moderators/:id/roles', authenticateModerator, requirePermission('manageRoles'), async (req, res) => {
  const { id } = req.params

  try {
    const [rows] = await pool.query(
      `SELECT r.id, r.name, r.permissions FROM roles r
       INNER JOIN moderator_roles mr ON r.id = mr.role_id
       WHERE mr.moderator_id = ?`,
      [id]
    )

    const roles = rows.map(r => ({
      id: r.id,
      name: r.name,
      permissions: typeof r.permissions === 'string' ? JSON.parse(r.permissions) : r.permissions
    }))

    res.json(roles)
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// POST /api/moderators/:id/roles - Přiřazení role moderátorovi
app.post('/api/moderators/:id/roles', authenticateModerator, requirePermission('manageRoles'), async (req, res) => {
  const { id } = req.params
  const { roleId } = req.body
  
  if (!roleId) {
    return res.status(400).json({ error: 'Role ID required' })
  }

  try {
    await pool.query(
      'INSERT INTO moderator_roles (moderator_id, role_id) VALUES (?, ?)',
      [id, roleId]
    )

    res.json({ success: true, message: 'Role assigned' })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// DELETE /api/moderators/:id/roles/:roleId - Odebrání role moderátorovi
app.delete('/api/moderators/:id/roles/:roleId', authenticateModerator, requirePermission('manageRoles'), async (req, res) => {
  const { id, roleId } = req.params

  try {
    await pool.query(
      'DELETE FROM moderator_roles WHERE moderator_id = ? AND role_id = ?',
      [id, roleId]
    )

    res.json({ success: true, message: 'Role removed' })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// GET /api/mod/actions - Historie moderátorských akcí
app.get('/api/mod/actions', authenticateModerator, requirePermission('viewActions'), async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT ma.*, m.username as moderator_username 
       FROM mod_actions ma
       INNER JOIN moderators m ON ma.moderator_id = m.id
       ORDER BY ma.timestamp DESC
       LIMIT 100`
    )

    res.json(rows)
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// ========================================
// EXISTING ENDPOINTS
// ========================================


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
      return res.status(404).json({ error: 'Player not found' })
    }
    const playerName = player.name

      const [punRows] = await pool.query(
        'SELECT reason, operator, punishmentType, start, isActive FROM punishments WHERE player_name = ? AND isActive = 1 ORDER BY start DESC',
        [playerName]
      )

      const items = (punRows || []).map((r) => ({
        type: r.punishmentType,
        reason: r.reason ?? 'No reason specified',
        date: r.start instanceof Date ? r.start.toISOString() : r.start,
        operator: r.operator ?? '-',
        isActive: r.isActive === 1 || r.isActive === true,
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

function toIso(value) {
  if (!value) return null
  if (value instanceof Date) return value.toISOString()
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? null : d.toISOString()
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
      const upstream = `https://starlightskins.lunareclipse.studio/render/default/${encodeURIComponent(id)}/bust`
      try {
        const buf = await fetchBuffer(upstream)
        await fs.writeFile(filePath, buf)
      } catch (err) {
        // Upstream failed. If we already have a file (even stale), keep it and let unified send below serve it.
        if (fssync.existsSync(filePath)) {
          return
        }
        // Otherwise, try default Steve fallback written to cache path
        try {
          const fallback = await fs.readFile(DEFAULT_STEVE_PATH)
          await fs.writeFile(filePath, fallback)
        } catch (e2) {
          // No fallback available, rethrow original error
          throw err
        }
      }
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

app.get('/api/players/:id', async (req, res) => {
  const { id } = req.params
  try {
    const [playerRows] = await pool.query(
      'SELECT uuid, name, lastLogin FROM players WHERE uuid = ? OR name = ? LIMIT 1',
      [id, id]
    )

    const player = Array.isArray(playerRows) ? playerRows[0] : null
    if (!player) {
      return res.status(404).json({ error: 'Player not found' })
    }

    const [punRows] = await pool.query(
      'SELECT id, reason, operator, punishmentType, start, `end`, duration, isActive FROM punishments WHERE player_name = ? ORDER BY start DESC',
      [player.name]
    )

    const punishments = (punRows || []).map((row) => ({
      id: row.id,
      type: row.punishmentType,
      reason: row.reason ?? 'No reason specified',
      operator: row.operator ?? '-',
      start: toIso(row.start),
      end: toIso(row.end),
      duration: row.duration === null || row.duration === undefined ? null : Number(row.duration),
      isActive: row.isActive === 1 || row.isActive === true,
    }))

    res.json({
      uuid: player.uuid,
      name: player.name,
      lastLogin: toIso(player.lastLogin),
      punishments,
    })
  } catch (e) {
    res.status(500).json({ error: String(e?.message || e) })
  }
})

// SPA fallback so client-side routing works
app.get('*', (req, res, next) => {
  if (req.path.startsWith('/api/')) {
    return next()
  }
  res.sendFile(SPA_INDEX_PATH, (err) => {
    if (err) {
      next(err)
    }
  })
})

app.listen(PORT, () => {
  // eslint-disable-next-line no-console
  console.log(`API server listening on http://localhost:${PORT}`)
  
  // Připojit se k Minecraft WebSocket serveru
  connectToMinecraft()
})
