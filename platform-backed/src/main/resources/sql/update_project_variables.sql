USE automated_testing_platform;

CREATE TABLE IF NOT EXISTS `project_variables` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) NOT NULL,
  `key_name` varchar(100) NOT NULL,
  `value` text,
  `description` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_key` (`project_id`, `key_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
