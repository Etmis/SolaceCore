/**
 * WebSocket service pro komunikaci s Minecraft serverem
 * Posílá moderátorské akce na server
 */

export class MinecraftWebSocket {
  private ws: WebSocket | null = null
  private url: string
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000
  private listeners: Map<string, ((data: any) => void)[]> = new Map()

  constructor(url?: string) {
    this.url = url || this.getWebSocketUrl()
  }

  private getWebSocketUrl(): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.hostname
    const port = import.meta.env.VITE_MINECRAFT_WS_PORT || '8082'
    return `${protocol}//${host}:${port}`
  }

  public connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.url)

        this.ws.onopen = () => {
          console.log('[MinecraftWS] Connected to server')
          this.reconnectAttempts = 0
          this.emit('connected')
          resolve()
        }

        this.ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data)
            console.log('[MinecraftWS] Received:', data)
            this.emit('message', data)

            if (data.type) {
              this.emit(data.type, data)
            }
          } catch (e) {
            console.error('[MinecraftWS] Failed to parse message:', e)
          }
        }

        this.ws.onerror = (error) => {
          console.error('[MinecraftWS] Error:', error)
          this.emit('error', error)
          reject(error)
        }

        this.ws.onclose = () => {
          console.warn('[MinecraftWS] Disconnected')
          this.emit('disconnected')
          this.attemptReconnect()
        }
      } catch (error) {
        console.error('[MinecraftWS] Connection failed:', error)
        reject(error)
      }
    })
  }

  private attemptReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`[MinecraftWS] Reconnecting... (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
      setTimeout(() => {
        this.connect().catch((e) => {
          console.error('[MinecraftWS] Reconnection failed:', e)
        })
      }, this.reconnectDelay)
    } else {
      console.error('[MinecraftWS] Max reconnection attempts reached')
      this.emit('reconnection_failed')
    }
  }

  public disconnect(): void {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  public isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }

  public ban(playerName: string, reason: string): void {
    this.sendCommand('ban', { playerName, reason })
  }

  public tempBan(playerName: string, duration: number, reason: string): void {
    this.sendCommand('tempban', { playerName, duration, reason })
  }

  public unban(playerName: string): void {
    this.sendCommand('unban', { playerName })
  }

  public kick(playerName: string, reason: string): void {
    this.sendCommand('kick', { playerName, reason })
  }

  public warn(playerName: string, reason: string): void {
    this.sendCommand('warn', { playerName, reason })
  }

  public mute(playerName: string, reason: string, duration: number = 0): void {
    this.sendCommand('mute', { playerName, reason, duration })
  }

  public unmute(playerName: string): void {
    this.sendCommand('unmute', { playerName })
  }

  private sendCommand(action: string, data: any): void {
    const message = {
      action,
      ...data
    }

    if (this.isConnected()) {
      this.ws!.send(JSON.stringify(message))
      console.log('[MinecraftWS] Sent command:', action, data)
    } else {
      console.warn('[MinecraftWS] Not connected, cannot send command:', action)
      this.emit('send_failed', { action, data })
    }
  }

  public on(event: string, callback: (data: any) => void): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event)!.push(callback)

    // Return unsubscribe function
    return () => {
      const callbacks = this.listeners.get(event)
      if (callbacks) {
        const index = callbacks.indexOf(callback)
        if (index > -1) {
          callbacks.splice(index, 1)
        }
      }
    }
  }

  private emit(event: string, data?: any): void {
    const callbacks = this.listeners.get(event)
    if (callbacks) {
      callbacks.forEach(callback => callback(data))
    }
  }
}

// Singleton instance
let instance: MinecraftWebSocket | null = null

export function getMinecraftWebSocket(): MinecraftWebSocket {
  if (!instance) {
    instance = new MinecraftWebSocket()
  }
  return instance
}

export function initializeMinecraftWebSocket(): Promise<void> {
  const ws = getMinecraftWebSocket()
  return ws.connect().catch((error) => {
    console.warn('[MinecraftWS] Failed to connect on initialization:', error)
    // Pokračujeme i když se připojení nezdaří - pokusíme se znovu později
  })
}
