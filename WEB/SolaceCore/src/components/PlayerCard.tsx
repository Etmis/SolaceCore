import type { Player } from '../types.ts'
import { getPlayerSkinUrl } from '../api.ts'

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
          src={getPlayerSkinUrl(player.name)}
          alt={`Player skin ${player.name}`}
          loading="lazy"
          onError={(e) => {
            e.currentTarget.onerror = null
            e.currentTarget.src = '/steve.png'
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