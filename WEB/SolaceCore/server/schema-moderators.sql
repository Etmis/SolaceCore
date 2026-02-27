-- Tabulka pro moderátory
CREATE TABLE IF NOT EXISTS moderators (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  is_active BOOLEAN DEFAULT TRUE,
  INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabulka pro role
CREATE TABLE IF NOT EXISTS roles (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL,
  permissions JSON NOT NULL COMMENT 'JSON objekt s oprávněními: {"ban": true, "unban": true, "warn": true, "kick": true, "manageRoles": true}',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabulka pro přiřazení rolí moderátorům (M:N vztah)
CREATE TABLE IF NOT EXISTS moderator_roles (
  moderator_id INT NOT NULL,
  role_id INT NOT NULL,
  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (moderator_id, role_id),
  FOREIGN KEY (moderator_id) REFERENCES moderators(id) ON DELETE CASCADE,
  FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabulka pro zaznamenání moderátorských akcí
CREATE TABLE IF NOT EXISTS mod_actions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  moderator_id INT NOT NULL,
  action_type ENUM('ban', 'tempban', 'unban', 'warn', 'kick', 'mute', 'unmute') NOT NULL,
  target_player VARCHAR(50) NOT NULL,
  reason TEXT,
  duration BIGINT NULL COMMENT 'Trvání v milisekundách (pro tempban, tempmute)',
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (moderator_id) REFERENCES moderators(id) ON DELETE CASCADE,
  INDEX idx_moderator (moderator_id),
  INDEX idx_target (target_player),
  INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Výchozí role pro administrátory (všechna oprávnění)
INSERT INTO roles (name, permissions) VALUES 
('Admin', '{"ban": true, "unban": true, "warn": true, "kick": true, "mute": true, "unmute": true, "manageRoles": true, "viewActions": true}'),
('Moderator', '{"ban": true, "unban": false, "warn": true, "kick": true, "mute": true, "unmute": true, "manageRoles": false, "viewActions": true}'),
('Helper', '{"ban": false, "unban": false, "warn": true, "kick": false, "mute": false, "unmute": false, "manageRoles": false, "viewActions": false}')
ON DUPLICATE KEY UPDATE name=name;

-- Příklad vytvoření testovacího moderátora (heslo: "admin123")
-- Hash vygenerován pomocí: node server/generate-password.js admin123
INSERT INTO moderators (username, password_hash) VALUES 
('admin', '$2a$10$5CV47OmyVDWdXzdD.TrMxO4b3s//1v3XKEnxxb.cfk5vrlIgDQMZa')
ON DUPLICATE KEY UPDATE username=username;

-- Přiřadit Admin roli testovacímu moderátorovi
INSERT INTO moderator_roles (moderator_id, role_id)
SELECT m.id, r.id FROM moderators m, roles r 
WHERE m.username = 'admin' AND r.name = 'Admin'
ON DUPLICATE KEY UPDATE moderator_id=moderator_id;
