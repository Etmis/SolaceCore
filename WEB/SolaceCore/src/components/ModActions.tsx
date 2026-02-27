import { useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { banPlayer, tempBanPlayer, unbanPlayer, warnPlayer, kickPlayer, mutePlayer, unmutePlayer } from '../api'
import { FaBan, FaExclamationTriangle, FaDoorOpen, FaVolumeOff, FaVolumeMute } from 'react-icons/fa'

interface ModActionsProps {
  playerName: string
  onActionComplete: () => void
}

export default function ModActions({ playerName, onActionComplete }: ModActionsProps) {
  const { hasPermission } = useAuth()
  const [reason, setReason] = useState('')
  const [duration, setDuration] = useState('3600000') // 1 hour default
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null)

  const handleAction = async (action: () => Promise<any>, actionName: string) => {
    if (!reason.trim() && actionName !== 'unban' && actionName !== 'unmute') {
      setMessage({ type: 'error', text: 'Reason is required' })
      return
    }

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
        <label htmlFor="reason">Reason</label>
        <input
          id="reason"
          type="text"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="Enter reason for action"
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
          <>
            <button
              className="btn btn-danger"
              onClick={() => handleAction(() => banPlayer(playerName, reason), 'Ban')}
              disabled={loading}
            >
              <FaBan /> Ban
            </button>

            <button
              className="btn btn-warning"
              onClick={() => handleAction(() => tempBanPlayer(playerName, parseInt(duration), reason), 'TempBan')}
              disabled={loading}
            >
              <FaBan /> TempBan
            </button>
          </>
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
            onClick={() => handleAction(() => warnPlayer(playerName, reason), 'Warn')}
            disabled={loading}
          >
            <FaExclamationTriangle /> Warn
          </button>
        )}

        {hasPermission('kick') && (
          <button
            className="btn btn-secondary"
            onClick={() => handleAction(() => kickPlayer(playerName, reason), 'Kick')}
            disabled={loading}
          >
            <FaDoorOpen /> Kick
          </button>
        )}

        {hasPermission('mute') && (
          <button
            className="btn btn-secondary"
            onClick={() => handleAction(() => mutePlayer(playerName, reason, parseInt(duration)), 'Mute')}
            disabled={loading}
          >
            <FaVolumeMute /> Mute
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
      </div>

      {(hasPermission('ban') || hasPermission('mute')) && (
        <div className="form-group">
          <label htmlFor="duration">Duration</label>
          <select
            id="duration"
            value={duration}
            onChange={(e) => setDuration(e.target.value)}
            disabled={loading}
          >
            <option value="300000">5 minutes</option>
            <option value="1800000">30 minutes</option>
            <option value="3600000">1 hour</option>
            <option value="86400000">1 day</option>
            <option value="604800000">7 days</option>
            <option value="2592000000">30 days</option>
          </select>
        </div>
      )}
    </div>
  )
}
