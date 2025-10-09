package com.etmisthefox.solacecore.models;

import java.time.LocalDateTime;

public final class Punishment {

    private int id;
    private String playerName; // Jmeno hrace (FK na players.name)
    private String reason;
    private String operator;
    private String punishmentType;
    private LocalDateTime start;
    private LocalDateTime end;
    private Long duration;
    private boolean isActive;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getPunishmentType() {
        return punishmentType;
    }

    public void setPunishmentType(String punishmentType) {
        this.punishmentType = punishmentType;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public Punishment(int id, String playerName, String reason, String operator, String punishmentType, LocalDateTime start, LocalDateTime end, Long duration, boolean isActive) {
        this.id = id;
        this.playerName = playerName;
        this.reason = reason;
        this.operator = operator;
        this.punishmentType = punishmentType;
        this.start = start;
        this.end = end;
        this.duration = duration;
        this.isActive = isActive;
    }
}
