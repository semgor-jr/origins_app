# Тест непубличных функций Origin App

Write-Host "Testing Private Functions" -ForegroundColor Green
Write-Host "========================" -ForegroundColor Green

$baseUrl = "http://localhost:8080"
$testResults = @()

# Глобальные переменные для аутентификации
$global:authToken = $null
$global:userId = $null

# Функция для тестирования эндпоинта
function Test-Endpoint {
    param($Method, $Url, $Description, $Body = $null, $Headers = @{})
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            TimeoutSec = 30
        }
        
        if ($Body) {
            $params.Body = $Body
            $params.ContentType = "application/json"
        }
        
        if ($Headers.Count -gt 0) {
            $params.Headers = $Headers
        }
        
        $response = Invoke-WebRequest @params
        Write-Host "SUCCESS: $Description - Status: $($response.StatusCode)" -ForegroundColor Green
        return @{ Status = "SUCCESS"; Code = $response.StatusCode; Message = "OK"; Content = $response.Content }
    }
    catch {
        Write-Host "ERROR: $Description - $($_.Exception.Message)" -ForegroundColor Red
        return @{ Status = "FAILED"; Message = $_.Exception.Message }
    }
}

# Функция для аутентификации
function Test-Authentication {
    Write-Host "`nTesting Authentication..." -ForegroundColor Yellow
    
    # Попытка входа с тестовым пользователем
    $loginBody = @{
        email = "test@example.com"
        password = "password123"
    } | ConvertTo-Json
    
    $result = Test-Endpoint -Method "POST" -Url "$baseUrl/auth/login" -Description "User Login" -Body $loginBody
    
    if ($result.Status -eq "SUCCESS") {
        try {
            $loginData = $result.Content | ConvertFrom-Json
            $global:authToken = $loginData.token
            $global:userId = $loginData.userId
            Write-Host "SUCCESS: Authentication successful!" -ForegroundColor Green
            Write-Host "Token: $($global:authToken.Substring(0, 20))..." -ForegroundColor Gray
            return $true
        }
        catch {
            Write-Host "ERROR: Failed to parse login response: $($_.Exception.Message)" -ForegroundColor Red
            return $false
        }
    } else {
        Write-Host "ERROR: Authentication failed!" -ForegroundColor Red
        return $false
    }
}

# Функция для тестирования профиля пользователя
function Test-UserProfile {
    Write-Host "`nTesting User Profile..." -ForegroundColor Yellow
    
    if (-not $global:authToken) {
        Write-Host "ERROR: No authentication token available" -ForegroundColor Red
        return @{ Status = "FAILED"; Message = "No authentication token" }
    }
    
    $headers = @{
        "Authorization" = "Bearer $global:authToken"
    }
    
    $result = Test-Endpoint -Method "GET" -Url "$baseUrl/me" -Description "Get User Profile" -Headers $headers
    
    if ($result.Status -eq "SUCCESS") {
        try {
            $profileData = $result.Content | ConvertFrom-Json
            Write-Host "Profile Data:" -ForegroundColor White
            Write-Host "  Name: $($profileData.name)" -ForegroundColor Gray
            Write-Host "  Email: $($profileData.contact)" -ForegroundColor Gray
            Write-Host "  Email Verified: $($profileData.isEmailVerified)" -ForegroundColor Gray
            Write-Host "  Autosomal Data: $($profileData.autosomalData -ne $null)" -ForegroundColor Gray
            Write-Host "  Y Haplogroup: $($profileData.yHaplogroup -ne $null)" -ForegroundColor Gray
            Write-Host "  MT Haplogroup: $($profileData.mtHaplogroup -ne $null)" -ForegroundColor Gray
        }
        catch {
            Write-Host "WARNING: Failed to parse profile data: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
    
    return $result
}

# Функция для тестирования создания отчета
function Test-CreateReport {
    Write-Host "`nTesting Report Creation..." -ForegroundColor Yellow
    
    if (-not $global:authToken) {
        Write-Host "ERROR: No authentication token available" -ForegroundColor Red
        return @{ Status = "FAILED"; Message = "No authentication token" }
    }
    
    $headers = @{
        "Authorization" = "Bearer $global:authToken"
    }
    
    # Тестируем создание отчета для аутосомного анализа
    $reportBody = @{
        name = "Test Autosomal Report"
        description = "Test autosomal analysis report"
        decodingMethod = "autosomal_analysis"
    } | ConvertTo-Json
    
    $result = Test-Endpoint -Method "POST" -Url "$baseUrl/reports" -Description "Create Autosomal Report" -Body $reportBody -Headers $headers
    
    if ($result.Status -eq "SUCCESS") {
        try {
            $reportData = $result.Content | ConvertFrom-Json
            Write-Host "Report Created:" -ForegroundColor White
            Write-Host "  ID: $($reportData.id)" -ForegroundColor Gray
            Write-Host "  Type: $($reportData.analysisType)" -ForegroundColor Gray
            Write-Host "  Status: $($reportData.status)" -ForegroundColor Gray
        }
        catch {
            Write-Host "WARNING: Failed to parse report data: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
    
    return $result
}

# Функция для тестирования получения отчетов
function Test-GetReports {
    Write-Host "`nTesting Report Retrieval..." -ForegroundColor Yellow
    
    if (-not $global:authToken) {
        Write-Host "ERROR: No authentication token available" -ForegroundColor Red
        return @{ Status = "FAILED"; Message = "No authentication token" }
    }
    
    $headers = @{
        "Authorization" = "Bearer $global:authToken"
    }
    
    $result = Test-Endpoint -Method "GET" -Url "$baseUrl/reports" -Description "Get User Reports" -Headers $headers
    
    if ($result.Status -eq "SUCCESS") {
        try {
            $reportsData = $result.Content | ConvertFrom-Json
            Write-Host "Reports Found: $($reportsData.Count)" -ForegroundColor White
            foreach ($report in $reportsData) {
                Write-Host "  - $($report.analysisType): $($report.status)" -ForegroundColor Gray
            }
        }
        catch {
            Write-Host "WARNING: Failed to parse reports data: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
    
    return $result
}

# Функция для тестирования админских функций
function Test-AdminFunctions {
    Write-Host "`nTesting Admin Functions..." -ForegroundColor Yellow
    
    if (-not $global:authToken) {
        Write-Host "ERROR: No authentication token available" -ForegroundColor Red
        return @{ Status = "FAILED"; Message = "No authentication token" }
    }
    
    $headers = @{
        "Authorization" = "Bearer $global:authToken"
    }
    
    # Тестируем получение списка пользователей
    $result = Test-Endpoint -Method "GET" -Url "$baseUrl/admin/users" -Description "Get All Users" -Headers $headers
    
    if ($result.Status -eq "SUCCESS") {
        try {
            $usersData = $result.Content | ConvertFrom-Json
            Write-Host "Users Found: $($usersData.Count)" -ForegroundColor White
            foreach ($user in $usersData) {
                Write-Host "  - $($user.name): $($user.contact)" -ForegroundColor Gray
            }
        }
        catch {
            Write-Host "WARNING: Failed to parse users data: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
    
    return $result
}

# Основной тест
Write-Host "`nStarting Private Functions Testing..." -ForegroundColor Cyan

# Тест аутентификации
$authResult = Test-Authentication
if (-not $authResult) {
    Write-Host "CRITICAL: Authentication failed! Cannot test private functions." -ForegroundColor Red
    Write-Host "Make sure the server is running and test user exists." -ForegroundColor Yellow
    exit 1
}

# Тестируем непубличные функции
$privateTests = @(
    @{ Function = "Test-UserProfile"; Description = "User Profile Testing" },
    @{ Function = "Test-CreateReport"; Description = "Report Creation Testing" },
    @{ Function = "Test-GetReports"; Description = "Report Retrieval Testing" },
    @{ Function = "Test-AdminFunctions"; Description = "Admin Functions Testing" }
)

foreach ($test in $privateTests) {
    Write-Host "`nRunning $($test.Description)..." -ForegroundColor Yellow
    $result = & $test.Function
    $testResults += @{
        Test = $test.Description
        Status = $result.Status
        Message = $result.Message
    }
}

# Результаты
Write-Host "`nTest Results Summary:" -ForegroundColor Cyan
Write-Host "======================" -ForegroundColor Cyan

$successCount = ($testResults | Where-Object { $_.Status -eq "SUCCESS" }).Count
$failedCount = ($testResults | Where-Object { $_.Status -eq "FAILED" }).Count
$totalCount = $testResults.Count

Write-Host "Total Tests: $totalCount" -ForegroundColor White
Write-Host "Successful: $successCount" -ForegroundColor Green
Write-Host "Failed: $failedCount" -ForegroundColor Red

Write-Host "`nDetailed Results:" -ForegroundColor Yellow
foreach ($result in $testResults) {
    $statusColor = if ($result.Status -eq "SUCCESS") { "Green" } else { "Red" }
    Write-Host "$($result.Test): $($result.Status)" -ForegroundColor $statusColor
    if ($result.Message) {
        Write-Host "  Message: $($result.Message)" -ForegroundColor Gray
    }
}

# Сохраняем результаты
$testResults | ConvertTo-Json -Depth 3 | Out-File -FilePath "scripts\test_results\test_private_functions.json" -Encoding UTF8
Write-Host "`nTest results saved to scripts\test_results\test_private_functions.json" -ForegroundColor Cyan

Write-Host "`nPrivate functions testing completed!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green

if ($failedCount -eq 0) {
    Write-Host "SUCCESS: All private function tests passed!" -ForegroundColor Green
    Write-Host "Origin App private API is working correctly!" -ForegroundColor Green
} else {
    Write-Host "WARNING: Some private function tests failed. Check the results above." -ForegroundColor Yellow
}
