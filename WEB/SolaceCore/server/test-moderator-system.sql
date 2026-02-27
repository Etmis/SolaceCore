-- Testovací skript pro ověření moderátorského systému

-- 1. Zobrazit všechny moderátory
SELECT id, username, is_active, created_at FROM moderators;

-- 2. Zobrazit všechny role
SELECT id, name, permissions FROM roles;

-- 3. Zobrazit přiřazení rolí moderátorům
SELECT 
    m.username,
    r.name as role_name,
    mr.assigned_at
FROM moderator_roles mr
JOIN moderators m ON mr.moderator_id = m.id
JOIN roles r ON mr.role_id = r.id
ORDER BY m.username, r.name;

-- 4. Zobrazit poslední moderátorské akce
SELECT 
    ma.id,
    m.username as moderator,
    ma.action_type,
    ma.target_player,
    ma.reason,
    ma.timestamp
FROM mod_actions ma
JOIN moderators m ON ma.moderator_id = m.id
ORDER BY ma.timestamp DESC
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

-- 7. Přiřadit roli moderátorovi
-- INSERT INTO moderator_roles (moderator_id, role_id) 
-- VALUES ((SELECT id FROM moderators WHERE username = 'newmod'), 
--         (SELECT id FROM roles WHERE name = 'Moderator'));

-- 8. Vytvořit vlastní roli
-- INSERT INTO roles (name, permissions) VALUES 
-- ('CustomRole', '{"ban": true, "warn": true, "kick": false, "manageRoles": false}');
