-- Tabulka pro moderátory
CREATE TABLE IF NOT EXISTS moderators (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  roles JSON NULL COMMENT 'Array of role IDs',
  is_active BOOLEAN DEFAULT TRUE,
  INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabulka pro role
CREATE TABLE IF NOT EXISTS roles (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL,
  permissions JSON NOT NULL COMMENT 'JSON objekt s oprávněními: {"ban": true, "unban": true, "warn": true, "kick": true, "manageRoles": true}',
  INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS moderator_roles;
DROP TABLE IF EXISTS mod_actions;

-- Výchozí role pro administrátory (všechna oprávnění)
INSERT INTO roles (name, permissions) VALUES 
('Admin', '{"ban": true, "tempban": true, "unban": true, "warn": true, "kick": true, "mute": true, "tempmute": true, "unmute": true, "ipban": true, "tempipban": true, "manageRoles": true, "viewActions": true}'),
('Moderator', '{"ban": true, "tempban": true, "unban": false, "warn": true, "kick": true, "mute": true, "tempmute": true, "unmute": true, "ipban": false, "tempipban": false, "manageRoles": false, "viewActions": true}'),
('Helper', '{"ban": false, "tempban": false, "unban": false, "warn": true, "kick": false, "mute": false, "tempmute": false, "unmute": false, "ipban": false, "tempipban": false, "manageRoles": false, "viewActions": false}')
ON DUPLICATE KEY UPDATE name=name;

-- První moderátor se vytváří během setup.js podle jména a hesla, které zadá uživatel.
-- Tato tabulka nesmí obsahovat hardcoded superadmina.

