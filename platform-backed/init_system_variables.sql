CREATE TABLE IF NOT EXISTS `system_variables` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `key_name` varchar(100) NOT NULL COMMENT 'Variable Key',
  `value` text NOT NULL COMMENT 'Variable Value',
  `description` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`key_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System Global Variables';

INSERT IGNORE INTO `system_variables` (`key_name`, `value`, `description`) VALUES 
('GLOBAL_BASE_URL', 'http://localhost:8080', 'Default Base URL'),
('GLOBAL_TIMEOUT', '5000', 'Default Timeout (ms)');
