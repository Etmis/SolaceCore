package com.etmisthefox.solacecore.database;

import com.etmisthefox.solacecore.SolaceCore;
import com.etmisthefox.solacecore.models.Punishment;
import org.bukkit.entity.Player;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class Database {

    private final SolaceCore plugin;

    private Connection connection;

    public Database(SolaceCore plugin) {
        this.plugin = plugin;
    }

    public Connection getConnection() throws SQLException {

        if (connection != null) {
            return connection;
        }

        String url = "jdbc:mysql://" + this.plugin.getConfig().getString("database.ip_address") + "/" + this.plugin.getConfig().getString("database.database_name");
        String user = this.plugin.getConfig().getString("database.user");
        String password = this.plugin.getConfig().getString("database.password");

        //String url = "jdbc:mysql://78.80.158.3/xban";
        //String user = "xban";
        //String password = "963852741";

        this.connection = DriverManager.getConnection(url, user, password);
        plugin.getLogger().info("Connected to the database.");
        return this.connection;
    }

    public void initializeDatabase() throws SQLException {
        // Tabulka players
        try (Statement playersTableStatement = getConnection().createStatement()) {
            String playersTableSQL = """
                    CREATE TABLE IF NOT EXISTS `players` (
                       `name` VARCHAR(16) NOT NULL,
                       `uuid` VARCHAR(36) UNIQUE,
                       `ipAddress` VARCHAR(45) DEFAULT NULL,
                       `lastLogin` DATETIME DEFAULT NULL,
                       PRIMARY KEY (`name`),
                       INDEX (`uuid`)
                    );
                    """;
            playersTableStatement.execute(playersTableSQL);
            plugin.getLogger().info("Players table successfully created.");
        }

        // Tabulka punishments
        try (Statement punishmentsTableStatement = getConnection().createStatement()) {
            String punishmentsTableSQL = """
                    CREATE TABLE IF NOT EXISTS `punishments` (
                       `id` INT NOT NULL AUTO_INCREMENT,
                       `player_name` VARCHAR(16) NOT NULL,
                       `reason` VARCHAR(255) DEFAULT NULL,
                       `operator` VARCHAR(16) DEFAULT NULL,
                       `punishmentType` VARCHAR(16) DEFAULT NULL,
                       `start` DATETIME DEFAULT NULL,
                       `end` DATETIME DEFAULT NULL,
                       `duration` BIGINT DEFAULT NULL,
                       `isActive` BOOLEAN DEFAULT NULL,
                       PRIMARY KEY (`id`),
                       INDEX (`player_name`),
                       CONSTRAINT `fk_punishments_player_name` FOREIGN KEY (`player_name`) REFERENCES `players`(`name`) ON DELETE CASCADE ON UPDATE CASCADE
                    );
                    """;
            punishmentsTableStatement.execute(punishmentsTableSQL);
            plugin.getLogger().info("Punishments table successfully created (FK player_name -> players.name).");
        }

        // Tabulka operators
        try (Statement operatorsTableStatement = getConnection().createStatement()) {
            String operatorsTableSQL = """
                    CREATE TABLE IF NOT EXISTS `operators` (
                        `id` INT NOT NULL AUTO_INCREMENT,
                        `role` VARCHAR(16) DEFAULT NULL,
                        PRIMARY KEY (`id`)
                    );
                    """;
            operatorsTableStatement.execute(operatorsTableSQL);
            plugin.getLogger().info("Operators table successfully created.");
        }
    }

    public void addPlayer(String playerName, String uuid, String ipAddress) throws SQLException {
        String sql = "INSERT INTO players(name, uuid, ipAddress, lastLogin) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE ipAddress = VALUES(ipAddress), lastLogin = VALUES(lastLogin)";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, playerName);
            statement.setString(2, uuid);
            statement.setString(3, ipAddress);
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
        }
    }

    public void createPunishment(Punishment punishment) throws SQLException {

        String sql = "INSERT INTO punishments(player_name, reason, operator, punishmentType, start, end, duration, isActive) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, punishment.getPlayerName());
            statement.setString(2, punishment.getReason());
            statement.setString(3, punishment.getOperator());
            statement.setString(4, punishment.getPunishmentType());
            statement.setTimestamp(5, punishment.getStart() != null ? Timestamp.valueOf(punishment.getStart()) : null);
            statement.setTimestamp(6, punishment.getEnd() != null ? Timestamp.valueOf(punishment.getEnd()) : null);
            if (punishment.getDuration() != null) {
                statement.setLong(7, punishment.getDuration());
            } else {
                statement.setNull(7, Types.BIGINT);
            }
            statement.setBoolean(8, punishment.getIsActive());
            statement.executeUpdate();
        }
    }

    public List<Punishment> getPunishmentsByName(String name) throws SQLException {
        String query = "SELECT * FROM punishments WHERE player_name = ? AND isActive = TRUE";
        List<Punishment> punishments = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, name);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    int id = results.getInt("id");
                    String playerName = results.getString("player_name");
                    String reason = results.getString("reason");
                    String operator = results.getString("operator");
                    String punishmentType = results.getString("punishmentType");
                    Timestamp startTimestamp = results.getTimestamp("start");
                    LocalDateTime start = (startTimestamp != null) ? startTimestamp.toLocalDateTime() : null;
                    Timestamp endTimestamp = results.getTimestamp("end");
                    LocalDateTime end = (endTimestamp != null) ? endTimestamp.toLocalDateTime() : null;
                    Long duration = results.getObject("duration") != null ? results.getLong("duration") : null;
                    boolean isActive = results.getBoolean("isActive");
                    punishments.add(new Punishment(id, playerName, reason, operator, punishmentType, start, end, duration, isActive));
                }
            }
        }
        return punishments;
    }

    public void unpunishPlayer(String name, String punishmentType) throws SQLException {
        String query = "UPDATE punishments SET isActive = FALSE, end = ? WHERE player_name = ? AND punishmentType = ? AND isActive = TRUE";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(2, name);
            statement.setString(3, punishmentType);
            statement.executeUpdate();
        }
    }

    public void closeConnection() {
        if (this.connection != null) {
            try {
                this.connection.close();
                plugin.getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
            }
        }
    }
}