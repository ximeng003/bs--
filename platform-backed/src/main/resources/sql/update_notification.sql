ALTER TABLE users ADD COLUMN notification_webhook VARCHAR(255) DEFAULT NULL COMMENT 'Webhook通知地址';
ALTER TABLE users ADD COLUMN enable_notification BOOLEAN DEFAULT FALSE COMMENT '是否开启通知';
