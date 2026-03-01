DELETE FROM test_cases
WHERE (name = 'User Login API'
    AND description = 'Test user login flow'
    AND content = '{"url": "/api/login", "method": "POST"}'
    AND environment = 'production')
   OR (name = 'Homepage Load'
    AND description = 'Verify homepage loads'
    AND content = 'open("https://example.com")'
    AND environment = 'staging')
   OR (name = 'App Payment'
    AND description = 'Test payment flow'
    AND content = 'click("pay")'
    AND environment = 'production');
