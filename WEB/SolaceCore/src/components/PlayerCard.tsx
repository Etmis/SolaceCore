import type { Player } from '../types.ts'

const skinUrl = (player: Player) =>
  `/api/skins/${encodeURIComponent(player.name || player.uuid)}/bust`

export default function PlayerCard({
  player,
  onClick
}: {
  player: Player
  onClick?: () => void
}) {
  return (
  <button className="card" onClick={onClick} title={`${player.name} (${player.uuid})`} aria-label={`View player ${player.name}`}>
      <div className="card-image">
        <img
          src={skinUrl(player)}
          alt={`Player skin ${player.name}`}
          loading="lazy"
          onError={(e) => {
            // simple fallback
            (e.currentTarget as HTMLImageElement).src =
              'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="200" height="260"><rect width="100%" height="100%" fill="%23eee"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%23999" font-family="sans-serif">Skin unavailable</text></svg>'
          }}
        />
      </div>
      <div className="card-body">
        <div className="player-name">{player.name}</div>
        <div className="player-uuid">{player.uuid}</div>
      </div>
    </button>
  )
}