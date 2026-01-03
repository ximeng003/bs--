-- Database Initialization Script for Automated Testing Platform

-- Create Database
CREATE DATABASE IF NOT EXISTS automated_testing_platform;
USE automated_testing_platform;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `role` varchar(20) DEFAULT 'user' COMMENT 'admin, user, viewer',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User Management';

-- ----------------------------
-- Table structure for environments
-- ----------------------------
DROP TABLE IF EXISTS `environments`;
CREATE TABLE `environments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT 'Display Name',
  `key_name` varchar(50) NOT NULL COMMENT 'Unique Key: dev, staging, prod',
  `base_url` varchar(255) DEFAULT NULL,
  `database_config` text COMMENT 'JSON string for DB connection info',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_key_name` (`key_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Test Environments';

-- ----------------------------
-- Table structure for test_cases
-- ----------------------------
DROP TABLE IF EXISTS `test_cases`;
CREATE TABLE `test_cases` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` text,
  `type` varchar(20) NOT NULL COMMENT 'API, WEB, APP',
  `status` varchar(20) DEFAULT 'active' COMMENT 'active, inactive, draft',
  `priority` varchar(20) DEFAULT 'medium' COMMENT 'high, medium, low',
  `created_by` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Test Cases Metadata';

-- ----------------------------
-- Table structure for api_case_details
-- ----------------------------
DROP TABLE IF EXISTS `api_case_details`;
CREATE TABLE `api_case_details` (
  `case_id` int(11) NOT NULL,
  `method` varchar(10) NOT NULL COMMENT 'GET, POST, PUT, DELETE, etc.',
  `url_path` varchar(255) NOT NULL,
  `headers` text COMMENT 'JSON object',
  `params` text COMMENT 'JSON object for query params',
  `body_type` varchar(20) DEFAULT 'json' COMMENT 'json, form-data, x-www-form-urlencoded, raw',
  `body_content` text COMMENT 'Request body content',
  `expected_response` text COMMENT 'JSON object defining validation rules',
  PRIMARY KEY (`case_id`),
  CONSTRAINT `fk_api_case` FOREIGN KEY (`case_id`) REFERENCES `test_cases` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API Test Case Specifics';

-- ----------------------------
-- Table structure for script_case_details
-- ----------------------------
DROP TABLE IF EXISTS `script_case_details`;
CREATE TABLE `script_case_details` (
  `case_id` int(11) NOT NULL,
  `script_type` varchar(20) DEFAULT 'python' COMMENT 'python, javascript, keyword',
  `content` text COMMENT 'Script code or JSON steps',
  `timeout` int(11) DEFAULT 30000 COMMENT 'Timeout in milliseconds',
  PRIMARY KEY (`case_id`),
  CONSTRAINT `fk_script_case` FOREIGN KEY (`case_id`) REFERENCES `test_cases` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Web/App Script Test Case Specifics';

-- ----------------------------
-- Table structure for test_plans
-- ----------------------------
DROP TABLE IF EXISTS `test_plans`;
CREATE TABLE `test_plans` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` text,
  `cron_expression` varchar(50) DEFAULT NULL COMMENT 'Cron schedule string',
  `status` varchar(20) DEFAULT 'active' COMMENT 'active, inactive',
  `default_environment_id` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_plan_env` (`default_environment_id`),
  CONSTRAINT `fk_plan_env` FOREIGN KEY (`default_environment_id`) REFERENCES `environments` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Test Plans / Suites';

-- ----------------------------
-- Table structure for test_plan_cases
-- ----------------------------
DROP TABLE IF EXISTS `test_plan_cases`;
CREATE TABLE `test_plan_cases` (
  `plan_id` int(11) NOT NULL,
  `case_id` int(11) NOT NULL,
  `execution_order` int(11) DEFAULT 0,
  PRIMARY KEY (`plan_id`,`case_id`),
  KEY `fk_plan_case_case` (`case_id`),
  CONSTRAINT `fk_plan_case_case` FOREIGN KEY (`case_id`) REFERENCES `test_cases` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_case_plan` FOREIGN KEY (`plan_id`) REFERENCES `test_plans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Many-to-Many Plan to Case';

-- ----------------------------
-- Table structure for executions
-- ----------------------------
DROP TABLE IF EXISTS `executions`;
CREATE TABLE `executions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `plan_id` int(11) DEFAULT NULL,
  `case_id` int(11) DEFAULT NULL COMMENT 'If run as single case',
  `type` varchar(20) NOT NULL COMMENT 'plan, single',
  `status` varchar(20) NOT NULL COMMENT 'running, success, failed, pending',
  `start_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `end_time` datetime DEFAULT NULL,
  `duration` int(11) DEFAULT NULL COMMENT 'Duration in ms',
  `environment_snapshot` varchar(50) DEFAULT NULL COMMENT 'Env name at time of run',
  `executor_name` varchar(50) DEFAULT NULL COMMENT 'User or System',
  `total_cases` int(11) DEFAULT 0,
  `passed_cases` int(11) DEFAULT 0,
  `failed_cases` int(11) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Execution History Log';

-- ----------------------------
-- Table structure for execution_results
-- ----------------------------
DROP TABLE IF EXISTS `execution_results`;
CREATE TABLE `execution_results` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `execution_id` int(11) NOT NULL,
  `case_id` int(11) NOT NULL,
  `status` varchar(20) NOT NULL COMMENT 'success, failed, skipped',
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `logs` longtext COMMENT 'Detailed execution logs/steps JSON',
  `error_message` text,
  `screenshot_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_exec_res_exec` (`execution_id`),
  KEY `fk_exec_res_case` (`case_id`),
  CONSTRAINT `fk_exec_res_case` FOREIGN KEY (`case_id`) REFERENCES `test_cases` (`id`),
  CONSTRAINT `fk_exec_res_exec` FOREIGN KEY (`execution_id`) REFERENCES `executions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Detailed Result per Case';

-- ----------------------------
-- Table structure for global_variables
-- ----------------------------
DROP TABLE IF EXISTS `global_variables`;
CREATE TABLE `global_variables` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `key_name` varchar(50) NOT NULL,
  `value` text,
  `description` varchar(255) DEFAULT NULL,
  `scope` varchar(20) DEFAULT 'global' COMMENT 'global, project',
  `is_secret` tinyint(1) DEFAULT 0,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_var_key` (`key_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Global Configuration Variables';

-- ----------------------------
-- Table structure for api_keys
-- ----------------------------
DROP TABLE IF EXISTS `api_keys`;
CREATE TABLE `api_keys` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `key_value` varchar(64) NOT NULL COMMENT 'Should be hashed in production',
  `permissions` text COMMENT 'JSON array of permissions',
  `last_used_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API Keys for External Access';

-- ----------------------------
-- Seed Data
-- ----------------------------

-- Users
INSERT INTO `users` (`username`, `password_hash`, `email`, `role`) VALUES
('admin', '$2a$10$XXXXXXXXXXXXXXXXXXXXXX', 'admin@example.com', 'admin'),
('tester', '$2a$10$XXXXXXXXXXXXXXXXXXXXXX', 'tester@example.com', 'user');

-- Environments
INSERT INTO `environments` (`name`, `key_name`, `base_url`, `database_config`) VALUES
('开发环境', 'dev', 'https://dev.example.com', '{"host":"dev-db","port":3306}'),
('测试环境', 'staging', 'https://staging.example.com', '{"host":"stage-db","port":3306}'),
('生产环境', 'production', 'https://api.example.com', '{"host":"prod-db","port":3306}');

-- Global Variables
INSERT INTO `global_variables` (`key_name`, `value`, `description`, `scope`) VALUES
('API_TOKEN', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...', 'API认证令牌', 'global'),
('DEFAULT_TIMEOUT', '30000', '默认超时时间(ms)', 'global');

-- Test Cases
INSERT INTO `test_cases` (`name`, `description`, `type`, `status`, `priority`) VALUES
('用户登录接口测试', '测试用户登录接口的正常流程和异常情况', 'API', 'active', 'high'),
('Web首页功能测试', '测试Web首页的关键功能和交互', 'WEB', 'active', 'high'),
('APP支付流程测试', '测试APP端的支付流程', 'APP', 'active', 'critical');

-- API Case Details
INSERT INTO `api_case_details` (`case_id`, `method`, `url_path`, `headers`, `body_type`, `body_content`) VALUES
(1, 'POST', '/api/v1/login', '{"Content-Type": "application/json"}', 'json', '{"username": "test", "password": "password"}');

-- Test Plans
INSERT INTO `test_plans` (`name`, `description`, `cron_expression`, `status`, `default_environment_id`) VALUES
('每日回归测试', '包含核心功能的全量回归测试', '0 2 * * *', 'active', 3);

-- Plan Cases
INSERT INTO `test_plan_cases` (`plan_id`, `case_id`, `execution_order`) VALUES
(1, 1, 1),
(1, 2, 2),
(1, 3, 3);

SET FOREIGN_KEY_CHECKS = 1;
