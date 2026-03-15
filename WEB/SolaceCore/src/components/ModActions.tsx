import { useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { banPlayer, tempBanPlayer, unbanPlayer, warnPlayer, kickPlayer, mutePlayer, tempMutePlayer, unmutePlayer, ipBanPlayer, tempIpBanPlayer } from '../api'
import { FaBan, FaExclamationTriangle, FaDoorOpen, FaVolumeOff, FaVolumeMute } from 'react-icons/fa'

interface ModActionsProps {
  playerName: string
  onActionComplete: () => void
}

export default function ModActions({ playerName, onActionComplete }: ModActionsProps) {
  const { hasPermission } = useAuth()
  const [reason, setReason] = useState('')
  const [duration, setDuration] = useState('3600') // 1 hour default (seconds)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null)

  const handleAction = async (action: () => Promise<any>, actionName: string) => {
    setLoading(true)
    setMessage(null)

    try {
      const response = await action()
      setMessage({ type: 'success', text: response.message || `${actionName} successful` })
      setReason('')
      setTimeout(() => {
        onActionComplete()
      }, 1500)
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || `${actionName} failed` })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mod-actions">
      <h3>Moderator Actions</h3>

      <div className="form-group">
        <label htmlFor="reason">Reason (optional)</label>
        <input
          id="reason"
          type="text"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="Reason"
          disabled={loading}
        />
      </div>

      {message && (
        <div className={`message ${message.type}`}>
          {message.text}
        </div>
      )}

      <div className="mod-actions-buttons">
        {hasPermission('ban') && (
          <button
            className="btn btn-danger"
            onClick={() => handleAction(() => banPlayer(playerName, reason.trim() || undefined), 'Ban')}
            disabled={loading}
          >
            <FaBan /> Ban
          </button>
        )}

        {hasPermission('tempban') && (
          <button
            className="btn btn-warning"
            onClick={() => handleAction(() => {
              const durationSeconds = Number.parseInt(duration, 10)
              if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) {
                throw new Error('Duration must be a positive number of seconds')
              }
              return tempBanPlayer(playerName, durationSeconds, reason.trim() || undefined)
            }, 'TempBan')}
            disabled={loading}
          >
            <FaBan /> TempBan
          </button>
        )}

        {hasPermission('unban') && (
          <button
            className="btn btn-success"
            onClick={() => handleAction(() => unbanPlayer(playerName), 'Unban')}
            disabled={loading}
          >
            <FaBan /> Unban
          </button>
        )}

        {hasPermission('warn') && (
          <button
            className="btn btn-warning"
            onClick={() => handleAction(() => warnPlayer(playerName, reason.trim() || undefined), 'Warn')}
            disabled={loading}
          >
            <FaExclamationTriangle /> Warn
          </button>
        )}

        {hasPermission('kick') && (
          <button
            className="btn btn-secondary"
            onClick={() => handleAction(() => kickPlayer(playerName, reason.trim() || undefined), 'Kick')}
            disabled={loading}
          >
            <FaDoorOpen /> Kick
          </button>
        )}

        {hasPermission('mute') && (
          <button
            className="btn btn-secondary"
            onClick={() => handleAction(() => mutePlayer(playerName, reason.trim() || undefined, undefined), 'Mute')}
            disabled={loading}
          >
            <FaVolumeMute /> Mute
          </button>
        )}

        {hasPermission('tempmute') && (
          <button
            className="btn btn-warning"
            onClick={() => handleAction(() => {
              const durationSeconds = Number.parseInt(duration, 10)
              if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) {
                throw new Error('Duration must be a positive number of seconds')
              }
              return tempMutePlayer(playerName, durationSeconds, reason.trim() || undefined)
            }, 'TempMute')}
            disabled={loading}
          >
            <FaVolumeMute /> TempMute
          </button>
        )}

        {hasPermission('unmute') && (
          <button
            className="btn btn-success"
            onClick={() => handleAction(() => unmutePlayer(playerName), 'Unmute')}
            disabled={loading}
          >
            <FaVolumeOff /> Unmute
          </button>
        )}

        {hasPermission('ipban') && (
          <button
            className="btn btn-danger"
            onClick={() => handleAction(() => ipBanPlayer(playerName, reason.trim() || undefined), 'IP Ban')}
            disabled={loading}
          >
            <FaBan /> IP Ban
          </button>
        )}

        {hasPermission('tempipban') && (
          <button
            className="btn btn-warning"
            onClick={() => handleAction(() => {
              const durationSeconds = Number.parseInt(duration, 10)
              if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) {
                throw new Error('Duration must be a positive number of seconds')
              }
              return tempIpBanPlayer(playerName, durationSeconds, reason.trim() || undefined)
            }, 'Temp IP Ban')}
            disabled={loading}
          >
            <FaBan /> Temp IP Ban
          </button>
        )}
      </div>

      {(hasPermission('ban') || hasPermission('tempban') || hasPermission('mute') || hasPermission('tempmute') || hasPermission('ipban') || hasPermission('tempipban')) && (
        <div className="form-group">
          <label htmlFor="duration">Duration in seconds</label>
          <input
            id="duration"
            type="number"
            min="1"
            step="1"
            value={duration}
            onChange={(e) => setDuration(e.target.value)}
            placeholder="e.g. 3600"
            disabled={loading}
          />
          <small>Examples: 300 (5m), 3600 (1h), 86400 (1d).</small>
        </div>
      )}
    </div>
  )
}
