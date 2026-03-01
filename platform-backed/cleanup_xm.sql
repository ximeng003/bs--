DELETE FROM test_cases WHERE created_by = (SELECT id FROM users WHERE username = 'xm');
