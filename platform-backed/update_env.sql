-- Environments Table
CREATE TABLE IF NOT EXISTS environments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    key_name VARCHAR(50) NOT NULL,
    base_url VARCHAR(200),
    database_name VARCHAR(100),
    active BOOLEAN DEFAULT FALSE
);

-- Seed Data: Environments
INSERT INTO environments (name, key_name, base_url, database_name, active) VALUES 
('开发环境', 'dev', 'https://dev.example.com', 'dev_db', 0),
('测试环境', 'staging', 'https://staging.example.com', 'staging_db', 1),
('生产环境', 'production', 'https://api.example.com', 'prod_db', 0);
