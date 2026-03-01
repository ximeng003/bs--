CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT 'User ID',
    username VARCHAR(255) COMMENT 'Username',
    module VARCHAR(50) COMMENT 'Module',
    operation VARCHAR(50) COMMENT 'Operation Type',
    target VARCHAR(255) COMMENT 'Target Object',
    details TEXT COMMENT 'Details',
    ip_address VARCHAR(50) COMMENT 'IP Address',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
