import { useEffect, useMemo, useState } from 'react'
import { fetchPunishments } from '../api.ts'
import type { Player, Punishment } from '../types.ts'

export default function PlayerModal({
  player,
  onClose
}: {
  player: Player
  onClose: () => void
}) {
  const [items, setItems] = useState<Punishment[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const data = await fetchPunishments(player.uuid)
        if (!cancelled) setItems(data)
      } catch (e: any) {
        if (!cancelled) setError(e?.message ?? 'Failed to load punishments')
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [player.uuid])

  const summaries = useMemo(() => {
    if (!items) return []
    return items.slice(0, 4)
  }, [items])

  const hasMore = (items?.length ?? 0) > summaries.length

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="player-modal-title" onClick={(e) => e.stopPropagation()}>
        <header className="modal-header">
          <div className="modal-title-group">
            <p className="modal-eyebrow">Recent punishments</p>
            <h2 id="player-modal-title">{player.name}</h2>
          </div>
          <button className="icon-btn" onClick={onClose} aria-label="Close">✕</button>
        </header>
        <div className="modal-content">
          {loading && <p>Loading…</p>}
          {error && <p className="error">Error: {error}</p>}
          {!loading && !error && (
            items && items.length > 0 ? (
              <>
                <div className="modal-intro">
                  <div className="modal-pill">
                    <span className="modal-pill-value">{items.length}</span>
                    <span className="modal-pill-label">total records</span>
                  </div>
                  <p className="muted">Showing the most recent {summaries.length} actions.</p>
                </div>
                <div className="punishment-cards" role="list">
                  {summaries.map((p, idx) => (
                    <article key={idx} className="punishment-card" role="listitem">
                      <header className="punishment-card-header">
                        <span className="badge">{p.type}</span>
                        <time className="muted" dateTime={toIsoString(p.date) ?? undefined}>{formatDateWithRelative(p.date)}</time>
                      </header>
                      <p className="punishment-reason">{p.reason}</p>
                      <dl className="punishment-meta">
                        <div>
                          <dt>Operator</dt>
                          <dd>{p.operator || '—'}</dd>
                        </div>
                      </dl>
                    </article>
                  ))}
                </div>
                {hasMore && <p className="muted">Open details to explore the full history ({items.length - summaries.length} more entries).</p>}
              </>
            ) : (
              <p className="muted">No punishments.</p>
            )
          )}
        </div>
        <footer className="modal-footer">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Close</button>
          <button
            type="button"
            className="btn"
            onClick={() => { window.location.href = `/players/${encodeURIComponent(player.name)}` }}
          >
            Open details
          </button>
        </footer>
      </div>
    </div>
  )
}

function formatDate(input: string | number | Date) {
  try {
    const d = new Date(input)
    if (isNaN(d.getTime())) return String(input)
    return d.toLocaleString(undefined, { hour12: false })
  } catch {
    return String(input)
  }
}

function toIsoString(input: string | number | Date) {
  try {
    const d = new Date(input)
    if (Number.isNaN(d.getTime())) return null
    return d.toISOString()
  } catch {
    return null
  }
}

function formatRelative(input: string | number | Date) {
  try {
    const d = new Date(input)
    if (Number.isNaN(d.getTime())) return ''
    const diff = Date.now() - d.getTime()
    if (!Number.isFinite(diff)) return ''
    const absMs = Math.abs(diff)
    const minutes = Math.round(absMs / 60000)
    if (minutes < 1) return 'just now'
    if (minutes === 1) return diff >= 0 ? '1 minute ago' : 'in 1 minute'
    if (minutes < 60) return diff >= 0 ? `${minutes} minutes ago` : `in ${minutes} minutes`
    const hours = Math.round(minutes / 60)
    if (hours === 1) return diff >= 0 ? '1 hour ago' : 'in 1 hour'
    if (hours < 24) return diff >= 0 ? `${hours} hours ago` : `in ${hours} hours`
    const days = Math.round(hours / 24)
    if (days === 1) return diff >= 0 ? 'yesterday' : 'tomorrow'
    if (days < 7) return diff >= 0 ? `${days} days ago` : `in ${days} days`
    const weeks = Math.round(days / 7)
    if (weeks === 1) return diff >= 0 ? 'last week' : 'next week'
    if (weeks < 5) return diff >= 0 ? `${weeks} weeks ago` : `in ${weeks} weeks`
    const months = Math.round(days / 30)
    if (months === 1) return diff >= 0 ? 'last month' : 'next month'
    if (months < 12) return diff >= 0 ? `${months} months ago` : `in ${months} months`
    const years = Math.round(days / 365)
    return diff >= 0 ? `${years} ${years === 1 ? 'year' : 'years'} ago` : `in ${years} ${years === 1 ? 'year' : 'years'}`
  } catch {
    return ''
  }
}

function formatDateWithRelative(input: string | number | Date) {
  const base = formatDate(input)
  const rel = formatRelative(input)
  return rel ? `${base} · ${rel}` : base
}