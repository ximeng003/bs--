$baseUrl = "http://localhost:18080/api"

function Get-TestCases($user) {
    $headers = @{ "X-User-Name" = $user }
    try {
        $response = Invoke-WebRequest -Method Get -Uri "$baseUrl/testcases" -Headers $headers -UseBasicParsing
        $json = $response.Content | ConvertFrom-Json
        return $json.data.total
    } catch {
        Write-Error "Failed to get test cases for $user : $_"
        return -1
    }
}

function Create-TestCase($user, $name) {
    $headers = @{ "X-User-Name" = $user }
    $body = @{
        name = $name
        type = "API"
        status = "active"
        priority = "low"
        description = "Created by $user"
    } | ConvertTo-Json -Compress
    
    try {
        $response = Invoke-WebRequest -Method Post -Uri "$baseUrl/testcases" -Headers $headers -ContentType "application/json" -Body $body -UseBasicParsing
        return $true
    } catch {
        Write-Error "Failed to create test case for $user : $_"
        return $false
    }
}

Write-Host "Verifying Isolation..."

# 1. Check Admin
$adminCount = Get-TestCases "admin"
Write-Host "Admin Test Cases: $adminCount"

# 2. Check xm (should be 0)
$xmCount = Get-TestCases "xm"
Write-Host "XM Test Cases: $xmCount"

if ($xmCount -eq 0) {
    Write-Host "SUCCESS: XM has 0 cases."
} else {
    Write-Host "FAILURE: XM has $xmCount cases (expected 0)."
}

# 3. Create Case for xm
Create-TestCase "xm" "xm-case-isolation-test"
$xmCountAfter = Get-TestCases "xm"
Write-Host "XM Test Cases After Create: $xmCountAfter"

if ($xmCountAfter -eq ($xmCount + 1)) {
    Write-Host "SUCCESS: XM case count incremented."
} else {
    Write-Host "FAILURE: XM case count mismatch."
}

# 4. Check Admin again (should be same as before, admin shouldn't see xm's case)
$adminCountAfter = Get-TestCases "admin"
Write-Host "Admin Test Cases After XM Create: $adminCountAfter"

if ($adminCountAfter -eq $adminCount) {
    Write-Host "SUCCESS: Admin count unchanged (Isolation working)."
} else {
    Write-Host "FAILURE: Admin count changed (Isolation broken or Admin sees all)."
}
