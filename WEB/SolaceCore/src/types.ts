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

export type Stats = {
  bansToday?: number
  totalBans?: number
  totalPunishments?: number
}