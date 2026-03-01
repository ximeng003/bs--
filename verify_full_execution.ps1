$baseUrl = "http://localhost:18080/api"

function Get-TestPlans($user) {
    $headers = @{ "X-User-Name" = $user }
    try {
        $response = Invoke-WebRequest -Method Get -Uri "$baseUrl/plans" -Headers $headers -UseBasicParsing
        $json = $response.Content | ConvertFrom-Json
        return $json.data
    } catch {
        Write-Error "Failed to get test plans for $user : $_"
        return $null
    }
}

function Create-TestPlan($user, $name, $caseIds) {
    $headers = @{ "X-User-Name" = $user }
    $body = @{
        name = $name
        description = "Created by $user"
        status = "active"
        environment = "dev"
        testCaseIds = $caseIds
    } | ConvertTo-Json -Compress
    
    try {
        $response = Invoke-WebRequest -Method Post -Uri "$baseUrl/plans" -Headers $headers -ContentType "application/json" -Body $body -UseBasicParsing
        return $true
    } catch {
        Write-Error "Failed to create test plan for $user : $_"
        return $false
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
        $json = $response.Content | ConvertFrom-Json
        return $json.data # Returns ID
    } catch {
        Write-Error "Failed to create test case for $user : $_"
        return $null
    }
}

function Execute-TestPlan($user, $planId) {
    $headers = @{ "X-User-Name" = $user }
    try {
        $response = Invoke-WebRequest -Method Post -Uri "$baseUrl/plans/$planId/execute" -Headers $headers -UseBasicParsing
        return $true
    } catch {
        Write-Error "Failed to execute test plan $planId for $user : $_"
        return $false
    }
}

function Get-TestReports($user) {
    $headers = @{ "X-User-Name" = $user }
    try {
        $response = Invoke-WebRequest -Method Get -Uri "$baseUrl/reports" -Headers $headers -UseBasicParsing
        $json = $response.Content | ConvertFrom-Json
        return $json.data.total
    } catch {
        Write-Error "Failed to get test reports for $user : $_"
        return -1
    }
}

Write-Host "Verifying Full Isolation (Case -> Plan -> Report)..."

# 1. Create Case for XM
$caseId = Create-TestCase "xm" "xm-case-for-plan"
if ($caseId) {
    Write-Host "Created Case ID: $caseId"
} else {
    Write-Host "Failed to create case"
    exit
}

# 2. Create Plan for XM with Case
if (Create-TestPlan "xm" "xm-plan-executable" "$caseId") {
    Write-Host "Created Plan 'xm-plan-executable'"
} else {
    Write-Host "Failed to create plan"
    exit
}

# 3. Get Plan ID
$plans = Get-TestPlans "xm"
$xmPlan = $plans.records | Where-Object { $_.name -eq "xm-plan-executable" } | Select-Object -First 1
if ($xmPlan) {
    Write-Host "Found Plan ID: $($xmPlan.id)"
} else {
    Write-Host "Failed to find created plan"
    exit
}

# 4. Execute Plan as XM
Write-Host "Executing Plan..."
if (Execute-TestPlan "xm" $xmPlan.id) {
    Write-Host "Plan executed successfully."
} else {
    Write-Host "Plan execution failed."
    # exit # Don't exit, maybe report was created anyway? No, if execution fails usually no report or partial.
}

# 5. Check Reports for XM
$xmReports = Get-TestReports "xm"
Write-Host "XM Reports: $xmReports"

if ($xmReports -gt 0) {
    Write-Host "SUCCESS: XM sees reports."
} else {
    Write-Host "FAILURE: XM sees 0 reports."
}

# 6. Check Reports for Admin
$adminReports = Get-TestReports "admin"
Write-Host "Admin Reports: $adminReports"

if ($adminReports -gt 0) {
    Write-Host "SUCCESS: Admin sees reports (Admin has global view)." -ForegroundColor Green
} else {
    Write-Host "FAILURE: Admin sees 0 reports! (Should see XM's report)" -ForegroundColor Red
}
