package com.etmisthefox.solacecore.database;

import com.etmisthefox.solacecore.SolaceCore;
import com.etmisthefox.solacecore.models.Punishment;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class Database {

    private final SolaceCore plugin;
    private Connection connection;
    private final Logger log;

    public Database(SolaceCore plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public Connection getConnection() throws SQLException {

        if (connection != null) {
            return connection;
        }

        FileConfiguration fc = plugin.getConfig();

        String url = "jdbc:mysql://" + fc.getString("database.ip_address") + "/" + fc.getString("database.database_name");
        String user = fc.getString("database.user");
        String password = fc.getString("database.password");
        connection = DriverManager.getConnection(url, user, password);
        log.info("Connected to the database.");
        return connection;
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
                       INDEX (`uuid`),
                       INDEX (`ipAddress`)
                    );
                    """;
            playersTableStatement.execute(playersTableSQL);
            log.info("Players table successfully created.");
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
            log.info("Punishments table successfully created (FK player_name -> players.name).");
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
            log.info("Operators table successfully created.");
        }

        // Tabulka action_logs
        try (Statement actionLogsTableStatement = getConnection().createStatement()) {
            String actionLogsTableSQL = """
                    CREATE TABLE IF NOT EXISTS `action_logs` (
                       `id` INT NOT NULL AUTO_INCREMENT,
                       `action_type` VARCHAR(50) NOT NULL,
                       `operator` VARCHAR(16) DEFAULT NULL,
                       `target_player` VARCHAR(16) DEFAULT NULL,
                       `reason` VARCHAR(255) DEFAULT NULL,
                       `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP,
                       `source` VARCHAR(20) DEFAULT 'ingame',
                       PRIMARY KEY (`id`),
                       INDEX (`operator`),
                       INDEX (`target_player`),
                       INDEX (`timestamp`)
                    );
                    """;
            actionLogsTableStatement.execute(actionLogsTableSQL);
            log.info("Action logs table successfully created.");
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
        String query = "SELECT * FROM punishments WHERE player_name = ?";
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
        String query = "UPDATE punishments SET isActive = FALSE, end = ?, duration = TIMESTAMPDIFF(SECOND, start, ?) WHERE player_name = ? AND punishmentType = ? AND isActive = TRUE";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            statement.setTimestamp(1, now);
            statement.setTimestamp(2, now);
            statement.setString(3, name);
            statement.setString(4, punishmentType);
            statement.executeUpdate();
        }
    }

    public void closeConnection() {
        if (this.connection != null) {
            try {
                this.connection.close();
                log.info("Database connection closed.");
            } catch (SQLException e) {
                log.severe("Failed to close database connection: " + e.getMessage());
            }
        }
    }

    public List<Punishment> getActivePunishmentsByIp(String hostAddress) throws SQLException {
        String query = "SELECT p.* FROM punishments p JOIN players pl ON pl.name = p.player_name WHERE pl.ipAddress = ? AND p.isActive = TRUE AND p.punishmentType IN ('ipban', 'tempipban')";
        List<Punishment> punishments = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, hostAddress);
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

    // Deactivate expired temporary punishments for given player
    private void expirePunishmentsForName(String name) throws SQLException {
        String sql = """
                UPDATE punishments
                SET isActive = FALSE,
                    end = COALESCE(end, NOW()),
                    duration = CASE WHEN start IS NOT NULL THEN TIMESTAMPDIFF(SECOND, start, NOW()) ELSE duration END
                WHERE player_name = ?
                  AND isActive = TRUE
                  AND (
                      (end IS NOT NULL AND end <= NOW())
                      OR (duration IS NOT NULL AND start IS NOT NULL AND DATE_ADD(start, INTERVAL duration SECOND) <= NOW())
                  )
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }

    public List<Punishment> getActivePunishmentsByName(String name) throws SQLException {
        // First, expire any time-limited punishments that already passed
        //expirePunishmentsForName(name);

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

    public void logAction(String actionType, String operator, String targetPlayer, String reason, String source) throws SQLException {
        String sql = "INSERT INTO action_logs(action_type, operator, target_player, reason, source) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, actionType);
            statement.setString(2, operator);
            statement.setString(3, targetPlayer);
            statement.setString(4, reason);
            statement.setString(5, source);
            statement.executeUpdate();
        }
    }
}
