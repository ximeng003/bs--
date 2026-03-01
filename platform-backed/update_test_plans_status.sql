ALTER TABLE test_plans ADD COLUMN last_run_status VARCHAR(50) DEFAULT NULL COMMENT 'Last execution status';
ALTER TABLE test_plans ADD COLUMN last_run_time DATETIME DEFAULT NULL COMMENT 'Last execution time';
