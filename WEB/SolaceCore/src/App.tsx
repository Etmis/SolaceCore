import { useEffect, useMemo, useState } from 'react'
import { fetchPlayers, fetchStats } from './api.ts'
import type { Player, Stats } from './types.ts'
import PlayerCard from './components/PlayerCard.tsx'
import PlayerModal from './components/PlayerModal.tsx'
import StatsBar from './components/StatsBar.tsx'
import { FaSearch } from "react-icons/fa";

export default function App() {
  const [players, setPlayers] = useState<Player[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState<Player | null>(null)
  const [stats, setStats] = useState<Stats | null>(null)
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    if (typeof window === 'undefined') return 'dark'
    const saved = localStorage.getItem('theme') as 'dark' | 'light' | null
    if (saved) return saved
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  })

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem('theme', theme)
  }, [theme])

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
          <div className="search skeleton" style={{height: 44}} />
        </header>
        <section className="stats">
          <div className="stat skeleton" style={{height: 72}} />
          <div className="stat skeleton" style={{height: 72}} />
          <div className="stat skeleton" style={{height: 72}} />
        </section>
        <main className="grid">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="card">
              <div className="card-image skeleton" />
              <div className="card-body">
                <div className="skeleton" style={{height: 16, width: '60%', borderRadius: 6}} />
                <div className="skeleton" style={{height: 12, width: '90%', borderRadius: 6}} />
              </div>
            </div>
          ))}
        </main>
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
        <div style={{display:'flex', gap:16, alignItems:'center'}}>
          <div className="search-wrap">
            <FaSearch className="search-icon" size={18} aria-hidden="true" />
            <input
              className="search"
              type="search"
              placeholder="Hledat hráče…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              aria-label="Hledat hráče"
            />
          </div>
          <button
            className="theme-toggle"
            onClick={() => setTheme(prev => prev === 'dark' ? 'light' : 'dark')}
            aria-label={theme === 'dark' ? 'Přepnout na světlý motiv' : 'Přepnout na tmavý motiv'}
            aria-pressed={theme === 'dark'}
            title={theme === 'dark' ? 'Světlý motiv' : 'Tmavý motiv'}
          >
            {theme === 'dark' ? '☀️' : '🌙'}
          </button>
        </div>
      </header>

      <StatsBar stats={stats ?? undefined} />

      <main className="grid">
        {filtered.map(p => (
          <PlayerCard key={p.uuid} player={p} onClick={() => setSelected(p)} />
        ))}
        {filtered.length === 0 && (
          <div className="empty">
            <div className="empty-icon" aria-hidden="true">🔍</div>
            <div className="empty-title">Nic nenalezeno</div>
            <div className="empty-desc">Zkuste upravit dotaz nebo vymazat hledání.</div>
          </div>
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