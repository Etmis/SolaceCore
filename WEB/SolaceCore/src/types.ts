export type Player = {
  uuid: string
  name: string
}

export type Punishment = {
  type: string
  reason: string
  date: string // ISO string nebo timestamp
  operator: string
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