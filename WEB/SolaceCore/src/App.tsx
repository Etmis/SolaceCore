import { useEffect, useMemo, useState } from 'react'
import { fetchPlayers, fetchStats } from './api.ts'
import type { Player, Stats } from './types.ts'
import PlayerCard from './components/PlayerCard.tsx'
import PlayerModal from './components/PlayerModal.tsx'
import StatsBar from './components/StatsBar.tsx'

export default function App() {
  const [players, setPlayers] = useState<Player[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState<Player | null>(null)
  const [stats, setStats] = useState<Stats | null>(null)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const [pls, st] = await Promise.all([fetchPlayers(), fetchStats()])
        if (!cancelled) {
          setPlayers(pls)
          setStats(st)
        }
      } catch (e: any) {
        if (!cancelled) setError(e?.message ?? 'Nepodařilo se načíst data')
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return players
    return players.filter(p => p.name.toLowerCase().includes(q))
  }, [players, query])

  if (loading) {
    return (
      <div className="container">
        <header className="header">
          <h1>Hráči</h1>
        </header>
        <p>Načítání…</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="container">
        <header className="header">
          <h1>Hráči</h1>
        </header>
        <p className="error">Chyba: {error}</p>
      </div>
    )
  }

  return (
    <div className="container">
      <header className="header">
        <h1>Hráči</h1>
        <input
          className="search"
          type="search"
          placeholder="Hledat podle jména…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </header>

      <StatsBar stats={stats ?? undefined} />

      <main className="grid">
        {filtered.map(p => (
          <PlayerCard key={p.uuid} player={p} onClick={() => setSelected(p)} />
        ))}
        {filtered.length === 0 && (
          <p className="muted">Žádní hráči pro zadané hledání.</p>
        )}
      </main>

      {selected && (
        <PlayerModal player={selected} onClose={() => setSelected(null)} />
      )}

      <footer className="footer">
        <span>SolaceCore</span>
      </footer>
    </div>
  )
}