$baseUrl = "http://localhost:18080/api"

function Get-TestPlans($user) {
    $headers = @{ "X-User-Name" = $user }
    try {
        $response = Invoke-WebRequest -Method Get -Uri "$baseUrl/plans" -Headers $headers -UseBasicParsing
        $json = $response.Content | ConvertFrom-Json
        return $json.data.total
    } catch {
        Write-Error "Failed to get test plans for $user : $_"
        return -1
    }
}

function Create-TestPlan($user, $name) {
    $headers = @{ "X-User-Name" = $user }
    $body = @{
        name = $name
        description = "Created by $user"
        status = "active"
        environment = "dev"
        testCaseIds = "" # Empty for now, just testing creation
    } | ConvertTo-Json -Compress
    
    try {
        $response = Invoke-WebRequest -Method Post -Uri "$baseUrl/plans" -Headers $headers -ContentType "application/json" -Body $body -UseBasicParsing
        return $true
    } catch {
        Write-Error "Failed to create test plan for $user : $_"
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

function Get-DashboardStats($user) {
    $headers = @{ "X-User-Name" = $user }
    try {
        $response = Invoke-WebRequest -Method Get -Uri "$baseUrl/dashboard/stats" -Headers $headers -UseBasicParsing
        $json = $response.Content | ConvertFrom-Json
        return $json.data
    } catch {
        Write-Error "Failed to get dashboard stats for $user : $_"
        return $null
    }
}

Write-Host "Verifying Plan & Report Isolation..."

# --- Dashboard (New User Should Be 0) ---
Write-Host "`n--- Checking Dashboard Stats ---"
$xmStats = Get-DashboardStats "xm"
if ($xmStats -ne $null) {
    Write-Host ("XM Dashboard: totalCases={0}, totalExecutions={1}" -f $xmStats.totalCases, $xmStats.totalExecutions)
    if ($xmStats.totalCases -eq 0 -and $xmStats.totalExecutions -eq 0) {
        Write-Host "SUCCESS: XM dashboard shows 0 initially."
    } else {
        Write-Host ("FAILURE: XM dashboard not 0 (cases={0}, exec={1})." -f $xmStats.totalCases, $xmStats.totalExecutions)
    }
}

# --- Test Plans ---
Write-Host "`n--- Checking Test Plans ---"
$adminPlans = Get-TestPlans "admin"
Write-Host "Admin Plans: $adminPlans"

$xmPlans = Get-TestPlans "xm"
Write-Host "XM Plans: $xmPlans"

if ($xmPlans -eq 0) {
    Write-Host "SUCCESS: XM has 0 plans initially."
} else {
    Write-Host "FAILURE: XM has $xmPlans plans (expected 0)."
}

if (Create-TestPlan "xm" "xm-plan-isolation-test") {
    $xmPlansAfter = Get-TestPlans "xm"
    Write-Host "XM Plans After Create: $xmPlansAfter"
    
    if ($xmPlansAfter -eq ($xmPlans + 1)) {
        Write-Host "SUCCESS: XM plan count incremented."
    } else {
        Write-Host "FAILURE: XM plan count mismatch."
    }
    
    $adminPlansAfter = Get-TestPlans "admin"
    Write-Host "Admin Plans After XM Create: $adminPlansAfter"
    
    if ($adminPlansAfter -eq ($adminPlans + 1)) {
        Write-Host "SUCCESS: Admin sees XM plan (global view)."
    } else {
        Write-Host "FAILURE: Admin plan count mismatch."
    }
}

# --- Test Reports ---
# Note: Creating a report requires executing a plan.
# For simplicity, we just check if list is empty for new user and non-empty for admin (if admin has reports)
# Or we can try to list reports.
Write-Host "`n--- Checking Test Reports ---"
$adminReports = Get-TestReports "admin"
Write-Host "Admin Reports: $adminReports"

$xmReports = Get-TestReports "xm"
Write-Host "XM Reports: $xmReports"

if ($xmReports -eq 0) {
    Write-Host "SUCCESS: XM has 0 reports initially."
} else {
    Write-Host "FAILURE: XM has $xmReports reports (expected 0)."
}
