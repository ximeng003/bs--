UPDATE test_plans SET created_by = (SELECT id FROM users WHERE username = 'admin') WHERE created_by IS NULL;
UPDATE test_cases SET created_by = (SELECT id FROM users WHERE username = 'admin') WHERE created_by IS NULL;
