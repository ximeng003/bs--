USE automated_testing_platform;

CREATE TABLE IF NOT EXISTS `project_api_keys` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `api_key` varchar(64) NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_key` (`api_key`),
  KEY `idx_project_user` (`project_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
