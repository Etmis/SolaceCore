-- Testovací skript pro ověření moderátorského systému

-- 1. Zobrazit všechny moderátory
SELECT id, username, is_active, roles FROM moderators;

-- 2. Zobrazit všechny role
SELECT id, name, permissions FROM roles;

-- 3. Zobrazit přiřazení rolí moderátorům (z JSON pole moderators.roles)
SELECT 
    m.username,
    m.roles,
    GROUP_CONCAT(r.name ORDER BY r.name SEPARATOR ', ') AS role_names
FROM moderators m
LEFT JOIN roles r ON JSON_CONTAINS(m.roles, CAST(r.id AS JSON), '$')
GROUP BY m.id, m.username, m.roles
ORDER BY m.username;

-- 4. Zobrazit poslední moderátorské akce (odvozeno z punishments)
SELECT 
    p.id,
    COALESCE(p.operator, 'CONSOLE') AS moderator,
    p.punishmentType AS action_type,
    p.player_name AS target_player,
    p.reason,
    p.start AS timestamp
FROM punishments p
ORDER BY p.start DESC
LIMIT 20;

-- 5. Zobrazit aktivní tresty
SELECT 
    p.id,
    p.player_name,
    p.punishmentType,
    p.reason,
    p.operator,
    p.start,
    p.end,
    p.isActive
FROM punishments p
WHERE p.isActive = 1
ORDER BY p.start DESC;

-- 6. Přidat nového moderátora (změňte username a použijte hash z generate-password.js)
-- INSERT INTO moderators (username, password_hash) VALUES ('newmod', 'HASH_HERE');

-- 7. Přiřadit roli moderátorovi (přidejte role ID do JSON pole)
-- UPDATE moderators m
-- JOIN roles r ON r.name = 'Moderator'
-- SET m.roles = JSON_ARRAY_APPEND(COALESCE(m.roles, JSON_ARRAY()), '$', r.id)
-- WHERE m.username = 'newmod' AND NOT JSON_CONTAINS(COALESCE(m.roles, JSON_ARRAY()), CAST(r.id AS JSON), '$');

-- 8. Vytvořit vlastní roli
-- INSERT INTO roles (name, permissions) VALUES 
-- ('CustomRole', '{"ban": true, "warn": true, "kick": false, "manageRoles": false}');
