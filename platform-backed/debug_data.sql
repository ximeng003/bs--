SELECT id, username FROM users;
SELECT id, name, created_by FROM test_plans;
SELECT id, name, type, created_by FROM test_cases;
SELECT id, name, description, content, environment FROM test_cases WHERE id IN (1,2,3);
