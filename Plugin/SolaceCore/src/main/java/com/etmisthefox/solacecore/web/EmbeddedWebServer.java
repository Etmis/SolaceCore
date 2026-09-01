package com.etmisthefox.solacecore.web;

import com.etmisthefox.solacecore.SolaceCore;
import com.etmisthefox.solacecore.database.Database;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.JarURLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Hosts the production web UI and API inside the Paper process. */
public final class EmbeddedWebServer {
    private final SolaceCore plugin;
    private final Database database;
    private final Path externalWebDirectory;
    private final Gson gson = new Gson();
    private final String jwtSecret;
    private HttpServer server;
    private ExecutorService executor;

    public EmbeddedWebServer(SolaceCore plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.externalWebDirectory = plugin.getDataFolder().toPath().resolve("web");
        this.jwtSecret = plugin.getConfig().getString("web.secret", "change-this-secret-in-production");
    }

    public void start() throws IOException {
        Files.createDirectories(externalWebDirectory);
        copyBundledWebOnFirstStart();
        String host = plugin.getConfig().getString("web.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("web.port", 3001);
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/api", this::handleApi);
        server.createContext("/", this::handleFrontend);
        executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "SolaceCore-Web");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.start();
        plugin.getLogger().info("Embedded web server listening on http://" + host + ":" + port);
    }

    private void copyBundledWebOnFirstStart() throws IOException {
        Path indexFile = externalWebDirectory.resolve("index.html");
        if (Files.exists(indexFile)) return;

        var resource = EmbeddedWebServer.class.getResource("/web");
        if (resource == null) return;

        if ("file".equals(resource.getProtocol())) {
            Path bundledDirectory = Path.of(resource.getPath());
            try (var files = Files.walk(bundledDirectory)) {
                files.filter(Files::isRegularFile).forEach(file -> copyBundledFile(bundledDirectory, file));
            }
            return;
        }

        if ("jar".equals(resource.getProtocol())) {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            try (JarFile jar = connection.getJarFile()) {
                String prefix = connection.getEntryName() + "/";
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().startsWith(prefix)) {
                        Path target = externalWebDirectory.resolve(entry.getName().substring(prefix.length())).normalize();
                        Files.createDirectories(target.getParent());
                        try (InputStream input = jar.getInputStream(entry)) {
                            Files.copy(input, target);
                        }
                    }
                }
            }
        }
    }

    private void copyBundledFile(Path bundledDirectory, Path source) {
        try {
            Path relative = bundledDirectory.relativize(source);
            Path target = externalWebDirectory.resolve(relative.toString());
            Files.createDirectories(target.getParent());
            Files.copy(source, target);
        } catch (IOException error) {
            throw new RuntimeException("Failed to copy bundled web file", error);
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
        if (executor != null) executor.shutdownNow();
    }

    private void handleApi(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if ("/api/health".equals(path)) {
                sendJson(exchange, 200, Map.of("ok", true, "db", database.getConnection().isValid(2)));
                return;
            }
            if ("/api/auth/login".equals(path) && method(exchange, "POST")) {
                JsonObject body = body(exchange);
                String username = string(body, "username");
                String password = string(body, "password");
                if (username == null || password == null) { sendError(exchange, 400, "Username and password required"); return; }
                try (var statement = database.getConnection().prepareStatement("SELECT id, username, password_hash, is_active FROM moderators WHERE username = ? LIMIT 1")) {
                    statement.setString(1, username);
                    try (var rows = statement.executeQuery()) {
                        if (!rows.next() || !rows.getBoolean("is_active") || !BCrypt.check(password, rows.getString("password_hash"))) { sendError(exchange, 401, "Invalid credentials"); return; }
                        int id = rows.getInt("id");
                        sendJson(exchange, 200, Map.of("token", token(id, username), "moderator", Map.of("id", id, "username", username)));
                    }
                }
                return;
            }
            if ("/api/players".equals(path) && method(exchange, "GET")) { queryRows(exchange, "SELECT uuid, name FROM players ORDER BY name ASC", null); return; }
            if ("/api/stats".equals(path) && method(exchange, "GET")) {
                var result = new HashMap<String, Object>();
                result.put("totalPunishments", scalar("SELECT COUNT(*) FROM punishments"));
                result.put("totalBans", scalar("SELECT COUNT(*) FROM punishments WHERE punishmentType IN ('ban','tempban')"));
                result.put("bansToday", scalar("SELECT COUNT(*) FROM punishments WHERE punishmentType IN ('ban','tempban') AND DATE(`start`) = CURDATE()"));
                sendJson(exchange, 200, result); return;
            }
            if (path.startsWith("/api/players/") && path.endsWith("/punishments") && method(exchange, "GET")) { punishmentEndpoint(exchange, path.substring(13, path.length() - 12)); return; }
            if (path.startsWith("/api/players/") && method(exchange, "GET")) { playerEndpoint(exchange, path.substring(13)); return; }
            if (path.startsWith("/api/skins/") && method(exchange, "GET")) { skinEndpoint(exchange, path); return; }
            User user = authenticate(exchange);
            if (user == null) return;
            if ("/api/auth/me".equals(path)) { sendJson(exchange, 200, user); return; }
            if (path.startsWith("/api/mod/") && method(exchange, "POST")) { moderationEndpoint(exchange, path.substring(9), user); return; }
            if ("/api/mod/actions".equals(path)) { if (!requirePermission(user, "viewActions", exchange)) return; queryRows(exchange, "SELECT id, 0 AS moderator_id, COALESCE(operator, 'CONSOLE') AS moderator_username, punishmentType AS action_type, player_name AS target_player, reason, duration, start AS timestamp FROM punishments ORDER BY start DESC LIMIT 100", null); return; }
            if (path.startsWith("/api/roles")) { rolesEndpoint(exchange, path, user); return; }
            if (path.startsWith("/api/moderators")) { moderatorsEndpoint(exchange, path, user); return; }
            sendError(exchange, 404, "API endpoint not found");
        } catch (Exception error) {
            plugin.getLogger().warning("Web request failed: " + error.getMessage());
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void skinEndpoint(HttpExchange exchange, String path) throws IOException {
        String[] skinPath = path.split("/");
        if (skinPath.length < 5) { sendError(exchange, 400, "Skin id required"); return; }
        String playerName = URLDecoder.decode(skinPath[3], StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Location", "https://visage.surgeplay.com/bust/" + URLEncoder.encode(playerName, StandardCharsets.UTF_8));
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void punishmentEndpoint(HttpExchange exchange, String encoded) throws Exception {
        String id = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        try (var statement = database.getConnection().prepareStatement("SELECT name FROM players WHERE uuid = ? LIMIT 1")) {
            statement.setString(1, id);
            try (var player = statement.executeQuery()) {
                if (!player.next()) { sendError(exchange, 404, "Player not found"); return; }
                try (var punishments = database.getConnection().prepareStatement("SELECT reason, operator, punishmentType, start, isActive FROM punishments WHERE player_name = ? AND isActive = 1 ORDER BY start DESC")) {
                    punishments.setString(1, player.getString("name"));
                    try (var rows = punishments.executeQuery()) {
                        var result = new java.util.ArrayList<Map<String, Object>>();
                        while (rows.next()) {
                            var item = new HashMap<String, Object>();
                            item.put("type", rows.getString("punishmentType")); item.put("reason", rows.getString("reason")); item.put("date", rows.getTimestamp("start")); item.put("operator", rows.getString("operator")); item.put("isActive", rows.getBoolean("isActive")); result.add(item);
                        }
                        sendJson(exchange, 200, result);
                    }
                }
            }
        }
    }

    private void playerEndpoint(HttpExchange exchange, String encoded) throws Exception {
        String id = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        try (var statement = database.getConnection().prepareStatement("SELECT uuid, name, lastLogin FROM players WHERE uuid = ? OR name = ? LIMIT 1")) {
            statement.setString(1, id); statement.setString(2, id);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) { sendError(exchange, 404, "Player not found"); return; }
                String name = rows.getString("name");
                Map<String, Object> player = new HashMap<>();
                player.put("uuid", rows.getString("uuid")); player.put("name", name); player.put("lastLogin", rows.getTimestamp("lastLogin"));
                try (var punishments = database.getConnection().prepareStatement("SELECT id, reason, operator, punishmentType, start, `end`, duration, isActive FROM punishments WHERE player_name = ? ORDER BY start DESC")) {
                    punishments.setString(1, name); try (var items = punishments.executeQuery()) {
                        var list = new java.util.ArrayList<Map<String, Object>>();
                        while (items.next()) {
                            var item = new HashMap<String, Object>();
                            item.put("id", items.getInt("id")); item.put("type", items.getString("punishmentType")); item.put("reason", items.getString("reason")); item.put("operator", items.getString("operator")); item.put("start", items.getTimestamp("start")); item.put("end", items.getTimestamp("end")); item.put("duration", items.getObject("duration")); item.put("isActive", items.getBoolean("isActive")); list.add(item);
                        }
                        player.put("punishments", list);
                    }
                }
                sendJson(exchange, 200, player);
            }
        }
    }

    private void moderationEndpoint(HttpExchange exchange, String action, User user) throws Exception {
        String permission = action.equals("unipban") ? "ipban" : action;
        if (!requirePermission(user, permission, exchange)) return;
        JsonObject body = body(exchange); String player = string(body, "playerName");
        if (player == null || player.isBlank()) { sendError(exchange, 400, "Player name required"); return; }
        String command = action + " " + player;
        if ("tempban".equals(action) || "tempmute".equals(action) || "tempipban".equals(action)) command += " " + number(body, "duration");
        if (body.has("reason") && !body.get("reason").isJsonNull()) command += " " + body.get("reason").getAsString();
            String finalCommand = command;
            runOnMainThread(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        sendJson(exchange, 200, Map.of("success", true, "message", "Action queued"));
    }

    private void rolesEndpoint(HttpExchange exchange, String path, User user) throws Exception {
        if (!requirePermission(user, "manageRoles", exchange)) return;
        if ("/api/roles".equals(path) && method(exchange, "GET")) { queryRows(exchange, "SELECT id, name, permissions FROM roles ORDER BY name ASC", null); return; }
        if ("/api/roles".equals(path) && method(exchange, "POST")) {
            JsonObject body = body(exchange); try (var statement = database.getConnection().prepareStatement("INSERT INTO roles (name, permissions) VALUES (?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS)) { statement.setString(1, string(body, "name")); statement.setString(2, body.get("permissions").toString()); statement.executeUpdate(); try (var keys = statement.getGeneratedKeys()) { keys.next(); sendJson(exchange, 200, Map.of("success", true, "id", keys.getInt(1), "message", "Role created")); } } return;
        }
        int id = Integer.parseInt(path.substring("/api/roles/".length()));
        if (method(exchange, "PUT")) { JsonObject body = body(exchange); try (var statement = database.getConnection().prepareStatement("UPDATE roles SET name = COALESCE(?, name), permissions = COALESCE(?, permissions) WHERE id = ?")) { statement.setString(1, string(body, "name")); statement.setString(2, body.has("permissions") ? body.get("permissions").toString() : null); statement.setInt(3, id); statement.executeUpdate(); } sendJson(exchange, 200, Map.of("success", true, "message", "Role updated")); return; }
        if (method(exchange, "DELETE")) { try (var statement = database.getConnection().prepareStatement("DELETE FROM roles WHERE id = ?")) { statement.setInt(1, id); statement.executeUpdate(); } sendJson(exchange, 200, Map.of("success", true, "message", "Role deleted")); }
    }

    private void moderatorsEndpoint(HttpExchange exchange, String path, User user) throws Exception {
        if (!requirePermission(user, "manageRoles", exchange)) return;
        if ("/api/moderators".equals(path) && method(exchange, "GET")) { queryRows(exchange, "SELECT id, username, is_active, roles FROM moderators ORDER BY username ASC", null); return; }
        if ("/api/moderators".equals(path) && method(exchange, "POST")) {
            JsonObject body = body(exchange); String username = string(body, "username"); String password = string(body, "password");
            if (username == null || password == null || username.length() < 3 || password.length() < 6) { sendError(exchange, 400, "Valid username and password required"); return; }
            try (var statement = database.getConnection().prepareStatement("INSERT INTO moderators (username, password_hash, is_active, roles) VALUES (?, ?, TRUE, '[]')", java.sql.Statement.RETURN_GENERATED_KEYS)) { statement.setString(1, username); statement.setString(2, org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt(10))); statement.executeUpdate(); try (var keys = statement.getGeneratedKeys()) { keys.next(); sendJson(exchange, 201, Map.of("id", keys.getInt(1), "username", username, "is_active", true, "roles", java.util.List.of())); } } return;
        }
        String suffix = path.substring("/api/moderators/".length());
        String[] parts = suffix.split("/"); int id = Integer.parseInt(parts[0]);
        if (parts.length == 1 && method(exchange, "PUT")) { JsonObject body = body(exchange); if (body.has("is_active")) updateModerator(id, "is_active", body.get("is_active").getAsBoolean()); if (body.has("password")) updateModerator(id, "password_hash", org.mindrot.jbcrypt.BCrypt.hashpw(body.get("password").getAsString(), org.mindrot.jbcrypt.BCrypt.gensalt(10))); sendJson(exchange, 200, Map.of("success", true)); return; }
        if (parts.length == 1 && method(exchange, "DELETE")) { if (id == user.id()) { sendError(exchange, 403, "Cannot delete your own account"); return; } updateModerator(id, "__delete", null); sendJson(exchange, 200, Map.of("success", true, "message", "Moderator deleted")); return; }
        if (parts.length == 2 && "roles".equals(parts[1]) && method(exchange, "GET")) { queryRows(exchange, "SELECT r.id, r.name, r.permissions FROM roles r JOIN moderators m ON JSON_CONTAINS(m.roles, JSON_ARRAY(r.id), '$') WHERE m.id = ?", String.valueOf(id)); return; }
        if (parts.length == 2 && "roles".equals(parts[1]) && method(exchange, "POST")) { JsonObject body = body(exchange); updateRoles(id, number(body, "roleId"), true); sendJson(exchange, 200, Map.of("success", true, "message", "Role assigned")); return; }
        if (parts.length == 3 && "roles".equals(parts[1]) && method(exchange, "DELETE")) { updateRoles(id, Long.parseLong(parts[2]), false); sendJson(exchange, 200, Map.of("success", true, "message", "Role removed")); }
    }

    private void updateModerator(int id, String field, Object value) throws Exception { String sql = "__delete".equals(field) ? "DELETE FROM moderators WHERE id = ?" : "UPDATE moderators SET " + field + " = ? WHERE id = ?"; try (var statement = database.getConnection().prepareStatement(sql)) { if (!"__delete".equals(field)) statement.setObject(1, value); statement.setInt("__delete".equals(field) ? 1 : 2, id); statement.executeUpdate(); } }
    private void updateRoles(int moderatorId, long roleId, boolean add) throws Exception { try (var statement = database.getConnection().prepareStatement("SELECT roles FROM moderators WHERE id = ?")) { statement.setInt(1, moderatorId); try (var rows = statement.executeQuery()) { if (!rows.next()) return; var roles = new java.util.ArrayList<Long>(); if (rows.getString(1) != null) for (var item : JsonParser.parseString(rows.getString(1)).getAsJsonArray()) roles.add(item.getAsLong()); roles.removeIf(value -> value == roleId); if (add) roles.add(roleId); try (var update = database.getConnection().prepareStatement("UPDATE moderators SET roles = ? WHERE id = ?")) { update.setString(1, gson.toJson(roles)); update.setInt(2, moderatorId); update.executeUpdate(); } } } }

    private void handleFrontend(HttpExchange exchange) throws IOException {
        String resource = exchange.getRequestURI().getPath();
        if (resource.equals("/") || !resource.contains(".")) resource = "/web/index.html";
        else resource = "/web" + resource;
        String relativePath = resource.substring("/web/".length());
        Path externalFile = externalWebDirectory.resolve(relativePath).normalize();
        if (!externalFile.startsWith(externalWebDirectory) || Files.isDirectory(externalFile)) {
            sendError(exchange, 404, "Not found");
            return;
        }
        byte[] data = null;
        if (Files.isRegularFile(externalFile)) {
            data = Files.readAllBytes(externalFile);
        } else {
            try (InputStream input = EmbeddedWebServer.class.getResourceAsStream(resource)) {
                if (input != null) data = input.readAllBytes();
            }
        }
        if (data == null) { sendError(exchange, 404, "Not found"); return; }
        Headers headers = exchange.getResponseHeaders(); headers.set("Content-Type", contentType(resource));
        exchange.sendResponseHeaders(200, data.length); try (OutputStream output = exchange.getResponseBody()) { output.write(data); }
    }

    private User authenticate(HttpExchange exchange) throws Exception {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { sendError(exchange, 401, "No token provided"); return null; }
        String[] tokenParts = header.substring(7).split("\\.");
        if (tokenParts.length != 3 || !MessageDigest.isEqual(hmac(tokenParts[0] + "." + tokenParts[1]), Base64.getUrlDecoder().decode(tokenParts[2]))) { sendError(exchange, 401, "Invalid token"); return null; }
        JsonObject payload = JsonParser.parseString(new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8)).getAsJsonObject();
        if (payload.get("exp").getAsLong() < Instant.now().getEpochSecond()) { sendError(exchange, 401, "Token expired"); return null; }
        int id = payload.get("id").getAsInt();
        try (var statement = database.getConnection().prepareStatement("SELECT id, username, is_active, roles FROM moderators WHERE id = ? LIMIT 1")) {
            statement.setInt(1, id);
            try (var rows = statement.executeQuery()) {
                if (!rows.next() || !rows.getBoolean("is_active")) { sendError(exchange, 401, "Invalid token"); return null; }
                Map<String, Boolean> permissions = new HashMap<>();
                if (rows.getString("roles") != null) {
                    try (var roles = database.getConnection().prepareStatement("SELECT permissions FROM roles WHERE JSON_CONTAINS(?, JSON_ARRAY(id), '$')")) {
                        roles.setString(1, rows.getString("roles"));
                        try (var roleRows = roles.executeQuery()) {
                            while (roleRows.next()) {
                                var rolePermissions = JsonParser.parseString(roleRows.getString("permissions")).getAsJsonObject();
                                for (var entry : rolePermissions.entrySet()) permissions.put(entry.getKey(), entry.getValue().getAsBoolean());
                            }
                        }
                    }
                }
                return new User(id, rows.getString("username"), permissions);
            }
        }
    }

    private String token(int id, String username) throws Exception { long expiry = Instant.now().getEpochSecond() + 86400; String header = enc("{\"alg\":\"HS256\",\"typ\":\"JWT\"}"); String payload = enc("{\"id\":" + id + ",\"username\":\"" + username.replace("\"", "") + "\",\"exp\":" + expiry + "}"); String data = header + "." + payload; return data + "." + enc(hmac(data)); }
    private byte[] hmac(String value) throws Exception { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return mac.doFinal(value.getBytes(StandardCharsets.UTF_8)); }
    private String enc(String value) { return enc(value.getBytes(StandardCharsets.UTF_8)); }
    private String enc(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private long scalar(String sql) throws Exception { try (var statement = database.getConnection().createStatement(); var rows = statement.executeQuery(sql)) { rows.next(); return rows.getLong(1); } }
    private void queryRows(HttpExchange exchange, String sql, String value) throws Exception { try (var statement = database.getConnection().prepareStatement(sql)) { if (value != null) statement.setString(1, value); try (var rows = statement.executeQuery()) { var result = new java.util.ArrayList<Map<String, Object>>(); var metadata = rows.getMetaData(); while (rows.next()) { var row = new HashMap<String, Object>(); for (int i = 1; i <= metadata.getColumnCount(); i++) { String label = metadata.getColumnLabel(i); Object item = rows.getObject(i); if (("permissions".equals(label) || "roles".equals(label)) && item instanceof String json) item = JsonParser.parseString(json); row.put(label, item); } result.add(row); } sendJson(exchange, 200, result); } } }
    private JsonObject body(HttpExchange exchange) throws IOException { return JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject(); }
    private String string(JsonObject body, String key) { return body.has(key) && !body.get(key).isJsonNull() ? body.get(key).getAsString() : null; }
    private long number(JsonObject body, String key) { return body.has(key) ? body.get(key).getAsLong() : 0; }
    private boolean method(HttpExchange exchange, String expected) { return expected.equalsIgnoreCase(exchange.getRequestMethod()); }
    private String contentType(String path) { if (path.endsWith(".js")) return "application/javascript"; if (path.endsWith(".css")) return "text/css"; if (path.endsWith(".png")) return "image/png"; return "text/html; charset=utf-8"; }
    private void runOnMainThread(Runnable action) { CompletableFuture<Void> future = new CompletableFuture<>(); Bukkit.getScheduler().runTask(plugin, () -> { try { action.run(); future.complete(null); } catch (Throwable error) { future.completeExceptionally(error); } }); future.join(); }
    private void sendError(HttpExchange exchange, int code, String message) throws IOException { sendJson(exchange, code, Map.of("error", message)); }
    private void sendJson(HttpExchange exchange, int code, Object value) throws IOException { byte[] data = gson.toJson(value).getBytes(StandardCharsets.UTF_8); exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8"); exchange.sendResponseHeaders(code, data.length); try (OutputStream output = exchange.getResponseBody()) { output.write(data); } }
    private boolean requirePermission(User user, String permission, HttpExchange exchange) throws IOException { if (!Boolean.TRUE.equals(user.permissions().get(permission))) { sendError(exchange, 403, "Insufficient permissions"); return false; } return true; }
    private record User(int id, String username, Map<String, Boolean> permissions) {}

    private static final class BCrypt {
        private BCrypt() {}
        static boolean check(String password, String hash) { try { return org.mindrot.jbcrypt.BCrypt.checkpw(password, hash); } catch (Exception ignored) { return false; } }
    }
}