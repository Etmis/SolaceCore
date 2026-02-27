import type { Player, PlayerDetails, Punishment, Stats, LoginResponse, Moderator, Role, ModAction } from './types.ts'
import { getMinecraftWebSocket } from './services/MinecraftWebSocket.ts'

const API_BASE = (import.meta.env.VITE_API_BASE || '').replace(/\/$/, '')

function resolvePath(path: string) {
  return `${API_BASE}${path}`
}

/**
 * Pošle akci na Minecraft server přes WebSocket
 */
function notifyMinecraftServer(action: string, data: any): void {
  try {
    const ws = getMinecraftWebSocket()
    if (ws.isConnected()) {
      switch (action) {
        case 'ban':
          ws.ban(data.playerName, data.reason || 'No reason specified')
          break
        case 'tempban':
          ws.tempBan(data.playerName, data.duration || 3600, data.reason || 'No reason specified')
          break
        case 'unban':
          ws.unban(data.playerName)
          break
        case 'kick':
          ws.kick(data.playerName, data.reason || 'No reason specified')
          break
        case 'warn':
          ws.warn(data.playerName, data.reason || 'No reason specified')
          break
        case 'mute':
          ws.mute(data.playerName, data.reason || 'No reason specified', data.duration || 0)
          break
        case 'unmute':
          ws.unmute(data.playerName)
          break
      }
    } else {
      console.warn('[API] Minecraft server not connected - action not sent to server')
    }
  } catch (error) {
    console.error('[API] Failed to notify Minecraft server:', error)
  }
}

function getAuthToken(): string | null {
  return localStorage.getItem('moderator_token')
}

async function get<T>(path: string, withAuth = false): Promise<T> {
  const headers: Record<string, string> = { 'Accept': 'application/json' }
  
  if (withAuth) {
    const token = getAuthToken()
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
  }

  const res = await fetch(resolvePath(path), { headers })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `HTTP ${res.status}`)
  }
  return res.json() as Promise<T>
}

async function post<T>(path: string, body: any, withAuth = false): Promise<T> {
  const headers: Record<string, string> = { 
    'Accept': 'application/json',
    'Content-Type': 'application/json'
  }
  
  if (withAuth) {
    const token = getAuthToken()
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
  }

  const res = await fetch(resolvePath(path), {
    method: 'POST',
    headers,
    body: JSON.stringify(body)
  })
  
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `HTTP ${res.status}`)
  }
  return res.json() as Promise<T>
}

async function put<T>(path: string, body: any, withAuth = false): Promise<T> {
  const headers: Record<string, string> = { 
    'Accept': 'application/json',
    'Content-Type': 'application/json'
  }
  
  if (withAuth) {
    const token = getAuthToken()
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
  }

  const res = await fetch(resolvePath(path), {
    method: 'PUT',
    headers,
    body: JSON.stringify(body)
  })
  
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `HTTP ${res.status}`)
  }
  return res.json() as Promise<T>
}

async function del<T>(path: string, withAuth = false): Promise<T> {
  const headers: Record<string, string> = { 'Accept': 'application/json' }
  
  if (withAuth) {
    const token = getAuthToken()
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
  }

  const res = await fetch(resolvePath(path), {
    method: 'DELETE',
    headers
  })
  
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `HTTP ${res.status}`)
  }
  return res.json() as Promise<T>
}

export async function fetchPlayers(): Promise<Player[]> {
  return get<Player[]>("/api/players")
}

export async function fetchPunishments(uuid: string): Promise<Punishment[]> {
  return get<Punishment[]>(`/api/players/${encodeURIComponent(uuid)}/punishments`)
}

export async function fetchStats(): Promise<Stats> {
  return get<Stats>("/api/stats")
}

export async function fetchPlayerDetails(identifier: string): Promise<PlayerDetails> {
  return get<PlayerDetails>(`/api/players/${encodeURIComponent(identifier)}`)
}

export function getPlayerSkinUrl(identifier: string): string {
  return resolvePath(`/api/skins/${encodeURIComponent(identifier)}/bust`)
}

// ========================================
// AUTH API
// ========================================

export async function login(username: string, password: string): Promise<LoginResponse> {
  return post<LoginResponse>('/api/auth/login', { username, password })
}

export async function getCurrentModerator(): Promise<Moderator> {
  return get<Moderator>('/api/auth/me', true)
}

export function logout(): void {
  localStorage.removeItem('moderator_token')
}

export function setAuthToken(token: string): void {
  localStorage.setItem('moderator_token', token)
}

export function isAuthenticated(): boolean {
  return !!getAuthToken()
}

// ========================================
// MODERATOR ACTIONS API
// ========================================

export async function banPlayer(playerName: string, reason?: string): Promise<{ success: boolean; message: string }> {
  notifyMinecraftServer('ban', { playerName, reason })
  return post('/api/mod/ban', { playerName, reason }, true)
}

export async function tempBanPlayer(playerName: string, duration: number, reason?: string): Promise<{ success: boolean; message: string }> {
  notifyMinecraftServer('tempban', { playerName, duration, reason })
  return post('/api/mod/tempban', { playerName, duration, reason }, true)
}

export async function unbanPlayer(playerName: string): Promise<{ success: boolean; message: string }> {
  notifyMinecraftServer('unban', { playerName })
  return post('/api/mod/unban', { playerName }, true)
}

export async function warnPlayer(playerName: string, reason?: string): Promise<{ success: boolean; message: string }> {
  notifyMinecraftServer('warn', { playerName, reason })
  return post('/api/mod/warn', { playerName, reason }, true)
}

export async function kickPlayer(playerName: string, reason?: string): Promise<{ success: boolean; message: string }> {
  notifyMinecraftServer('kick', { playerName, reason })
  return post('/api/mod/kick', { playerName, reason }, true)
}

export async function mutePlayer(playerName: string, reason?: string, duration?: number): Promise<{ success: boolean; message: string }> {
  notifyMinecraftServer('mute', { playerName, reason, duration })
  return post('/api/mod/mute', { playerName, reason, duration }, true)
}

export async function unmutePlayer(playerName: string): Promise<{ success: boolean; message: string }> {
  notifyMinecraftServer('unmute', { playerName })
  return post('/api/mod/unmute', { playerName }, true)
}

export async function getModActions(): Promise<ModAction[]> {
  return get<ModAction[]>('/api/mod/actions', true)
}

// ========================================
// ROLE MANAGEMENT API
// ========================================

export async function getRoles(): Promise<Role[]> {
  return get<Role[]>('/api/roles', true)
}

export async function createRole(name: string, permissions: Record<string, boolean>): Promise<{ success: boolean; id: number; message: string }> {
  return post('/api/roles', { name, permissions }, true)
}

export async function updateRole(id: number, name?: string, permissions?: Record<string, boolean>): Promise<{ success: boolean; message: string }> {
  return put(`/api/roles/${id}`, { name, permissions }, true)
}

export async function deleteRole(id: number): Promise<{ success: boolean; message: string }> {
  return del(`/api/roles/${id}`, true)
}

export async function getModerators(): Promise<any[]> {
  return get<any[]>('/api/moderators', true)
}

export async function getModeratorRoles(modId: number): Promise<Role[]> {
  return get<Role[]>(`/api/moderators/${modId}/roles`, true)
}

export async function assignRole(modId: number, roleId: number): Promise<{ success: boolean; message: string }> {
  return post(`/api/moderators/${modId}/roles`, { roleId }, true)
}

export async function removeRole(modId: number, roleId: number): Promise<{ success: boolean; message: string }> {
  return del(`/api/moderators/${modId}/roles/${roleId}`, true)
}