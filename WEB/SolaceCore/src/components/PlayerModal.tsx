import { useEffect, useState } from 'react'
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
        if (!cancelled) setError(e?.message ?? 'Nepodařilo se načíst prohřešky')
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [player.uuid])

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <header className="modal-header">
          <h2>Historie prohřešků — {player.name}</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Zavřít">✕</button>
        </header>
        <div className="modal-content">
          {loading && <p>Načítání…</p>}
          {error && <p className="error">Chyba: {error}</p>}
          {!loading && !error && (
            <>
              {items && items.length > 0 ? (
                <ul className="list">
                  {items.map((p, idx) => (
                    <li key={idx} className="list-item">
                      <div className="list-item-row">
                        <span className="badge">{p.type}</span>
                        <span className="muted">{formatDate(p.date)}</span>
                      </div>
                      <div className="list-item-row">
                        <span className="reason">{p.reason}</span>
                        <span className="operator">Operátor: {p.operator}</span>
                      </div>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="muted">Žádné prohřešky.</p>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}

function formatDate(input: string | number | Date) {
  try {
    const d = new Date(input)
    if (isNaN(d.getTime())) return String(input)
    return d.toLocaleString('cs-CZ')
  } catch {
    return String(input)
  }
}