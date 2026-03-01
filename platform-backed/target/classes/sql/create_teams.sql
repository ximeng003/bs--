CREATE TABLE IF NOT EXISTS `teams` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT 'Team Name',
  `description` varchar(500) DEFAULT NULL COMMENT 'Description',
  `created_by` int(11) NOT NULL COMMENT 'Creator User ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Team Table';

CREATE TABLE IF NOT EXISTS `team_members` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `team_id` int(11) NOT NULL COMMENT 'Team ID',
  `user_id` int(11) NOT NULL COMMENT 'User ID',
  `role` varchar(50) NOT NULL DEFAULT 'member' COMMENT 'Role: admin, member',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_user` (`team_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Team Members Table';

-- Add team_id to projects if not exists
SET @dbname = DATABASE();
SET @tablename = "projects";
SET @columnname = "team_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  "ALTER TABLE projects ADD COLUMN team_id int(11) DEFAULT NULL COMMENT 'Team ID' AFTER id;"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;