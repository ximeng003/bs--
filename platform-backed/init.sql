-- Create Database
CREATE DATABASE IF NOT EXISTS automated_testing_platform;
USE automated_testing_platform;

-- Disable foreign key checks to allow dropping tables in any order
SET FOREIGN_KEY_CHECKS = 0;

-- Drop Tables if exists
DROP TABLE IF EXISTS api_case_details; -- Found from error log
DROP TABLE IF EXISTS test_reports;
DROP TABLE IF EXISTS test_cases;
DROP TABLE IF EXISTS test_plans;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS environments;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Users Table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) DEFAULT 'user',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test Cases Table
CREATE TABLE test_cases (
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
CREATE TABLE test_plans (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'active',
    cron_expression VARCHAR(50),
    environment VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test Reports Table
CREATE TABLE test_reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plan_id INT,
    case_id INT, -- If run individually
    status VARCHAR(20), -- success, failed
    execution_time INT, -- in ms
    logs LONGTEXT,
    executed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    executed_by VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Environments Table
CREATE TABLE environments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    key_name VARCHAR(50) NOT NULL,
    base_url VARCHAR(200),
    database_name VARCHAR(100),
    active BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed Data: Users
INSERT INTO users (username, password, role) VALUES ('admin', '123456', 'admin');
INSERT INTO users (username, password, role) VALUES ('testuser', '123456', 'user');

-- Seed Data: Test Cases
INSERT INTO test_cases (name, description, type, status, priority, content, environment, last_result) VALUES 
('User Login API', 'Test user login flow', 'API', 'active', 'high', '{"url": "/api/login", "method": "POST"}', 'production', 'success'),
('Homepage Load', 'Verify homepage loads', 'WEB', 'active', 'medium', 'open("https://example.com")', 'staging', 'success'),
('App Payment', 'Test payment flow', 'APP', 'active', 'high', 'click("pay")', 'production', 'failed');

-- Seed Data: Environments
INSERT INTO environments (name, key_name, base_url, database_name, active) VALUES 
('开发环境', 'dev', 'https://dev.example.com', 'dev_db', 0),
('测试环境', 'staging', 'https://staging.example.com', 'staging_db', 1),
('生产环境', 'production', 'https://api.example.com', 'prod_db', 0);
