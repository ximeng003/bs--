
USE automated_testing_platform;

CREATE TABLE IF NOT EXISTS login_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    ip_address VARCHAR(50),
    location VARCHAR(255),
    device VARCHAR(255),
    status VARCHAR(50),
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_variables (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    key_name VARCHAR(255) NOT NULL,
    value TEXT,
    description VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_key (user_id, key_name)
);
