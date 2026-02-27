export type Player = {
  uuid: string
  name: string
}

export type Punishment = {
  type: string
  reason: string
  date: string // ISO string nebo timestamp
  operator: string
  isActive?: boolean
}

export type PunishmentDetail = {
  id: number
  type: string
  reason: string
  operator: string
  start: string | null
  end: string | null
  duration: number | null
  isActive: boolean
}

export type PlayerDetails = Player & {
  lastLogin: string | null
  punishments: PunishmentDetail[]
}

export type Stats = {
  bansToday?: number
  totalBans?: number
  totalPunishments?: number
}

export type Moderator = {
  id: number
  username: string
  permissions: Record<string, boolean>
}

export type Role = {
  id: number
  name: string
  permissions: Record<string, boolean>
  createdAt?: string
}

export type ModAction = {
  id: number
  moderator_id: number
  moderator_username: string
  action_type: string
  target_player: string
  reason: string | null
  duration: number | null
  timestamp: string
}

export type LoginResponse = {
  token: string
  moderator: {
    id: number
    username: string
  }
}