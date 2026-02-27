import { useEffect, useMemo, useState } from 'react'
import { fetchPlayers, fetchStats } from './api.ts'
import type { Player, Stats } from './types.ts'
import PlayerCard from './components/PlayerCard.tsx'
import PlayerModal from './components/PlayerModal.tsx'
import StatsBar from './components/StatsBar.tsx'
import Login from './components/Login.tsx'
import RoleManagement from './components/RoleManagement.tsx'
import { useAuth } from './contexts/AuthContext'
import { initializeMinecraftWebSocket } from './services/MinecraftWebSocket.ts'
import { FaSearch, FaHeart, FaUserShield, FaSignInAlt, FaSignOutAlt } from "react-icons/fa";
import { FiMoon, FiSun } from "react-icons/fi";

export default function App() {
  const [players, setPlayers] = useState<Player[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState<Player | null>(null)
  const [stats, setStats] = useState<Stats | null>(null)
  const [showLogin, setShowLogin] = useState(false)
  const [showRoleManagement, setShowRoleManagement] = useState(false)
  const { moderator, logout } = useAuth()
  const [cookieAccepted, setCookieAccepted] = useState<boolean>(() => {
    if (typeof window === 'undefined') return false
    return localStorage.getItem('cookieConsent') === 'accepted'
  })
  const [showCookie, setShowCookie] = useState<boolean>(() => {
    if (typeof window === 'undefined') return false
    return localStorage.getItem('cookieConsent') !== 'accepted'
  })
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

  // Inicializuj WebSocket připojení k Minecraft serveru
  useEffect(() => {
    initializeMinecraftWebSocket()
  }, [])

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
        if (!cancelled) setError(e?.message ?? 'Failed to load data')
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
          <h1>Players</h1>
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
          <h1>Players</h1>
        </header>
        <p className="error">Error: {error}</p>
      </div>
    )
  }

  return (
    <div className="container">
      <header className="header">
        <h1>Players</h1>
        <div className="header-actions">
          <div className="search-wrap">
            <FaSearch className="search-icon" size={18} aria-hidden="true" />
            <input
              className="search"
              type="search"
              placeholder="Search players…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              aria-label="Search players"
            />
          </div>
          
          {moderator && (
            <>
              <button
                type="button"
                className="btn btn-sm"
                onClick={() => setShowRoleManagement(true)}
                title="Role Management"
              >
                <FaUserShield /> Roles
              </button>
              <button
                type="button"
                className="btn btn-sm btn-ghost"
                onClick={logout}
                title={`Logout ${moderator.username}`}
              >
                <FaSignOutAlt /> Logout
              </button>
            </>
          )}
          
          {!moderator && (
            <button
              type="button"
              className="btn btn-sm"
              onClick={() => setShowLogin(true)}
              title="Moderator Login"
            >
              <FaSignInAlt /> Login
            </button>
          )}
          
          <button
            type="button"
            className="theme-toggle"
            data-theme={theme}
            onClick={() => setTheme(prev => prev === 'dark' ? 'light' : 'dark')}
            aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
            aria-pressed={theme === 'dark'}
            title={theme === 'dark' ? 'Light theme' : 'Dark theme'}
          >
            <span className="theme-toggle-inner" aria-hidden="true">
              <FiSun className="theme-icon theme-icon--sun" aria-hidden="true" focusable="false" />
              <FiMoon className="theme-icon theme-icon--moon" aria-hidden="true" focusable="false" />
              <span className="theme-toggle-spark theme-toggle-spark--1" />
              <span className="theme-toggle-spark theme-toggle-spark--2" />
            </span>
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
            <div className="empty-title">Nothing found</div>
            <div className="empty-desc">Try adjusting your query or clear the search.</div>
          </div>
        )}
      </main>

      {selected && (
        <PlayerModal player={selected} onClose={() => setSelected(null)} />
      )}
      
      {showLogin && (
        <Login onClose={() => setShowLogin(false)} />
      )}
      
      {showRoleManagement && (
        <div className="modal-overlay" onClick={() => setShowRoleManagement(false)}>
          <div className="modal modal-large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Role Management</h2>
              <button className="close-btn" onClick={() => setShowRoleManagement(false)}>×</button>
            </div>
            <div className="modal-content">
              <RoleManagement />
            </div>
          </div>
        </div>
      )}

      <footer className="footer">
        <div className="footer-brand">SolaceCore</div>
        <div className="footer-links">
          <a className="link" href="/privacy">Privacy Policy</a>
          <a className="link" href="/terms">Terms of Service</a>
          <button className="link" onClick={() => setShowCookie(true)}>Cookies settings</button>
        </div>
        <div className="footer-meta">
          <span>© 2025</span>
          <span>•</span>
          <span>Made with <span className="heart"><FaHeart /></span> by</span>
          <a className="link" href="https://EtmisTheFox.com" target="_blank" rel="noreferrer">EtmisTheFox</a>
        </div>
      </footer>

      {showCookie && (
        <div className="cookie-backdrop" role="dialog" aria-modal="true" aria-labelledby="cookie-title">
          <div className="cookie-modal">
            <div className="cookie-header">
              <h2 id="cookie-title">Privacy and cookies</h2>
            </div>
            <div className="cookie-content">
              <p>This site does not use non‑essential cookies. We only store technical preferences (theme, consent) in LocalStorage.</p>
              <p>See our <a href="/privacy" target="_blank" rel="noreferrer">Privacy Policy</a> for details.</p>
            </div>
            <div className="cookie-actions">
              <button className="btn" onClick={() => { localStorage.setItem('cookieConsent', 'accepted'); setCookieAccepted(true); setShowCookie(false) }}>Accept</button>
              <button className="btn btn-ghost" onClick={() => setShowCookie(false)}>Close</button>
            </div>
          </div>
        </div>
      )}
      {!showCookie && !cookieAccepted && (
        <div className="cookie-bar" role="region" aria-label="Privacy">
          <span>This site stores only necessary technical data (theme, consent). <a href="/privacy" className="link">Learn more</a></span>
          <div style={{display:'flex', gap:8}}>
            <button className="btn btn-sm" onClick={() => { localStorage.setItem('cookieConsent', 'accepted'); setCookieAccepted(true); }}>OK</button>
            <button className="btn btn-sm btn-ghost" onClick={() => setShowCookie(true)}>Settings</button>
          </div>
        </div>
      )}
    </div>
  )
}