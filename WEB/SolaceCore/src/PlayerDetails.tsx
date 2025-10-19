import { useEffect, useMemo, useState } from 'react'
import { fetchPlayerDetails } from './api.ts'
import type { PlayerDetails, PunishmentDetail } from './types.ts'

export default function PlayerDetailsPage({ playerName }: { playerName: string }) {
  const [data, setData] = useState<PlayerDetails | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const payload = await fetchPlayerDetails(playerName)
        if (!cancelled) setData(payload)
      } catch (e: any) {
        if (!cancelled) setError(e?.message ?? 'Failed to load player details')
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [playerName])

  const aggregates = useMemo(() => {
    const punishments = data?.punishments ?? []
    const total = punishments.length
    const active = punishments.filter(p => p.isActive).length
    const bans = punishments.filter(p => /ban/i.test(p.type ?? '')).length
    const mutes = punishments.filter(p => /mute/i.test(p.type ?? '')).length
    return { total, active, bans, mutes }
  }, [data])

  const handleBack = () => {
    if (window.history.length > 1) {
      window.history.back()
    } else {
      window.location.href = '/'
    }
  }

  return (
    <div className="container detail-container">
      <header className="detail-header">
        <button type="button" className="btn btn-ghost" onClick={handleBack}>
          ← Back
        </button>
        <div className="detail-title">
          <p className="detail-eyebrow">Player</p>
          <h1>{data?.name ?? 'Player details'}</h1>
          {data && (
            <div className="detail-subtitle">
              <span className="muted">UUID:</span>
              <code>{data.uuid}</code>
            </div>
          )}
        </div>
      </header>

      {loading && <p>Loading…</p>}
      {error && <p className="error">Error: {error}</p>}

      {!loading && !error && data && (
        <>
          <section className="detail-grid">
            <article className="detail-card">
              <h2>Identity</h2>
              <dl>
                <div>
                  <dt>Player name</dt>
                  <dd>{data.name}</dd>
                </div>
                <div>
                  <dt>Last login</dt>
                  <dd>{formatDateTime(data.lastLogin)}</dd>
                </div>
              </dl>
            </article>
            <article className="detail-card">
              <h2>Summary</h2>
              <div className="detail-stats">
                <div>
                  <span className="detail-stat-label">Total</span>
                  <span className="detail-stat-value">{aggregates.total}</span>
                </div>
                <div>
                  <span className="detail-stat-label">Active</span>
                  <span className="detail-stat-value">{aggregates.active}</span>
                </div>
                <div>
                  <span className="detail-stat-label">Bans</span>
                  <span className="detail-stat-value">{aggregates.bans}</span>
                </div>
                <div>
                  <span className="detail-stat-label">Mutes</span>
                  <span className="detail-stat-value">{aggregates.mutes}</span>
                </div>
              </div>
            </article>
          </section>

          <section className="detail-table-wrap">
            <header className="detail-table-head">
              <h2>Complete punishment history</h2>
              <p className="muted">Includes every record for this player sorted by newest first.</p>
            </header>
            {data.punishments.length === 0 ? (
              <p className="muted">No punishments on record.</p>
            ) : (
              <div className="detail-table-scroll">
                <table className="detail-table">
                  <thead>
                    <tr>
                      <th scope="col">Type</th>
                      <th scope="col">Reason</th>
                      <th scope="col">Operator</th>
                      <th scope="col">Start</th>
                      <th scope="col">End</th>
                      <th scope="col">Duration</th>
                      <th scope="col">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.punishments.map((entry: PunishmentDetail) => (
                      <tr key={entry.id}>
                        <td data-label="Type">
                          <span className="badge">{entry.type}</span>
                        </td>
                        <td data-label="Reason">
                          <span className="reason-cell">{entry.reason}</span>
                        </td>
                        <td data-label="Operator">{entry.operator || '—'}</td>
                        <td data-label="Start">
                          <span>{formatDateTime(entry.start)}</span>
                          <small className="muted">{formatRelative(entry.start)}</small>
                        </td>
                        <td data-label="End">
                          <span>{formatDateTime(entry.end)}</span>
                          <small className="muted">{entry.end ? formatRelative(entry.end) : ''}</small>
                        </td>
                        <td data-label="Duration">{formatDuration(entry.duration)}</td>
                        <td data-label="Status">
                          {renderStatusChip(entry)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
    </div>
  )
}

function formatDateTime(value: string | null) {
  if (!value) return '—'
  try {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return String(value)
    return date.toLocaleString(undefined, { hour12: false })
  } catch {
    return String(value)
  }
}

function formatRelative(value: string | null) {
  if (!value) return ''
  try {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return ''
    const diff = Date.now() - date.getTime()
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

function formatDuration(value: number | null) {
  if (value === null || value === undefined) return '—'
  if (value <= 0) return 'Instant'
  const seconds = Math.round(value)
  const parts: string[] = []
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  if (days) parts.push(`${days}d`)
  if (hours) parts.push(`${hours}h`)
  if (minutes) parts.push(`${minutes}m`)
  if (!days && !hours && secs) parts.push(`${secs}s`)
  return parts.join(' ') || `${seconds}s`
}

function renderStatusChip(entry: PunishmentDetail) {
  const status = getStatusInfo(entry)
  if (!status) return null
  return (
    <span className={`status-chip ${status.className}`}>
      {status.label}
    </span>
  )
}

function getStatusInfo(entry: PunishmentDetail) {
  const type = (entry.type || '').toLowerCase()
  if (type.includes('kick')) {
    return { label: 'Resolved', className: 'status-expired' }
  }
  if (entry.isActive) {
    return { label: 'Active', className: 'status-active' }
  }
  return { label: 'Expired', className: 'status-expired' }
}
