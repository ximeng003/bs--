DELETE FROM test_reports WHERE executed_by = 'xm';
DELETE FROM test_plans WHERE created_by = (SELECT id FROM users WHERE username = 'xm');
DELETE FROM test_cases WHERE created_by = (SELECT id FROM users WHERE username = 'xm');
