import type { Player, PlayerDetails, Punishment, Stats } from './types.ts'

const API_BASE = import.meta.env.VITE_API_BASE || ''

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Accept': 'application/json' }
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || `HTTP ${res.status}`)
  }
  return res.json() as Promise<T>
}

export async function fetchPlayers(): Promise<Player[]> {
  return get<Player[]>('/api/players')
}

export async function fetchPunishments(uuid: string): Promise<Punishment[]> {
  return get<Punishment[]>(`/api/players/${encodeURIComponent(uuid)}/punishments`)
}

export async function fetchStats(): Promise<Stats> {
  return get<Stats>('/api/stats')
}

export async function fetchPlayerDetails(identifier: string): Promise<PlayerDetails> {
  return get<PlayerDetails>(`/api/players/${encodeURIComponent(identifier)}`)
}