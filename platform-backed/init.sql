-- Create Database
CREATE DATABASE IF NOT EXISTS automated_testing_platform;
USE automated_testing_platform;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) DEFAULT 'user',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test Cases Table
CREATE TABLE IF NOT EXISTS test_cases (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL, -- API, WEB, APP
    status VARCHAR(20) DEFAULT 'active', -- active, inactive, draft
    priority VARCHAR(20) DEFAULT 'medium', -- high, medium, low
    content LONGTEXT, -- Stores JSON for API case or Script text
    environment VARCHAR(50),
    last_run DATETIME,
    last_result VARCHAR(20),
    created_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test Plans Table
CREATE TABLE IF NOT EXISTS test_plans (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'active',
    cron_expression VARCHAR(50),
    environment VARCHAR(50),
    test_case_ids TEXT,
    created_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test Reports Table
CREATE TABLE IF NOT EXISTS test_reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plan_id INT,
    plan_run_no INT, -- 第几次执行同一个计划，从 1 开始
    case_id INT, -- If run individually
    status VARCHAR(20), -- success, failed
    execution_time INT, -- in ms
    logs LONGTEXT,
    executed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    executed_by VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Environments Table
CREATE TABLE IF NOT EXISTS environments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    key_name VARCHAR(50) NOT NULL,
    base_url VARCHAR(200),
    database_name VARCHAR(100),
    active BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed Data: Users
INSERT IGNORE INTO users (username, password, role) VALUES ('admin', '123456', 'admin');

-- Seed Data: Environments
INSERT INTO environments (name, key_name, base_url, database_name, active)
SELECT 'Dev Env', 'dev', 'https://dev.example.com', 'dev_db', 0
WHERE NOT EXISTS (SELECT 1 FROM environments);
INSERT INTO environments (name, key_name, base_url, database_name, active)
SELECT 'Test Env', 'staging', 'https://staging.example.com', 'staging_db', 1
WHERE NOT EXISTS (SELECT 1 FROM environments WHERE key_name = 'staging');
INSERT INTO environments (name, key_name, base_url, database_name, active)
SELECT 'Prod Env', 'production', 'https://api.example.com', 'prod_db', 0
WHERE NOT EXISTS (SELECT 1 FROM environments WHERE key_name = 'production');
