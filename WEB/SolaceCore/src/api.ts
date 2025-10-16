import type { Player, PlayerDetails, Punishment, Stats } from './types.ts'

const API_BASE = (import.meta.env.VITE_API_BASE || '').replace(/\/$/, '')

function resolvePath(path: string) {
  return `${API_BASE}${path}`
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(resolvePath(path), {
    headers: { 'Accept': 'application/json' }
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `HTTP ${res.status}`)
  }
  return res.json() as Promise<T>
}

export async function fetchPlayers(): Promise<Player[]> {
  return get<Player[]>('http://localhost:3001/api/players')
}

export async function fetchPunishments(uuid: string): Promise<Punishment[]> {
  return get<Punishment[]>(`http://localhost:3001/api/players/${encodeURIComponent(uuid)}/punishments`)
}

export async function fetchStats(): Promise<Stats> {
  return get<Stats>('http://localhost:3001/api/stats')
}

export async function fetchPlayerDetails(identifier: string): Promise<PlayerDetails> {
  return get<PlayerDetails>(`http://localhost:3001/api/players/${encodeURIComponent(identifier)}`)
}

export function getPlayerSkinUrl(identifier: string): string {
  return resolvePath(`http://localhost:3001/api/skins/${encodeURIComponent(identifier)}/bust`)
}