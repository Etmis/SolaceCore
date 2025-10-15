import type { Stats } from '../types.ts'

export default function StatsBar({ stats }: { stats?: Stats }) {
  if (!stats) {
    return (
      <section className="stats">
        <div className="stat"><span className="stat-label">Bany dnes</span><span className="stat-value">—</span></div>
        <div className="stat"><span className="stat-label">Bany celkem</span><span className="stat-value">—</span></div>
        <div className="stat"><span className="stat-label">Prohřešky celkem</span><span className="stat-value">—</span></div>
      </section>
    )
  }

  return (
    <section className="stats">
      <div className="stat">
        <span className="stat-label">Bany dnes</span>
        <span className="stat-value">{stats.bansToday ?? 0}</span>
      </div>
      <div className="stat">
        <span className="stat-label">Bany celkem</span>
        <span className="stat-value">{stats.totalBans ?? 0}</span>
      </div>
      <div className="stat">
        <span className="stat-label">Prohřešky celkem</span>
        <span className="stat-value">{stats.totalPunishments ?? 0}</span>
      </div>
    </section>
  )
}