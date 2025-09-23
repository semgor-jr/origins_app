# Простой запуск тестов Origin App
# Запускает public, private и security тесты

Write-Host "Origin App - Essential Testing Suite" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$startTime = Get-Date

# Функция для проверки сервера
function Test-ServerAvailability {
    Write-Host "`nChecking Server Availability..." -ForegroundColor Yellow
    
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/decoding-methods" -Method "GET" -TimeoutSec 10
        if ($response.StatusCode -eq 200) {
            Write-Host "Server is running and accessible" -ForegroundColor Green
            return $true
        } else {
            Write-Host "Server returned status: $($response.StatusCode)" -ForegroundColor Red
            return $false
        }
    }
    catch {
        Write-Host "Server is not accessible: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Please make sure the server is running on $baseUrl" -ForegroundColor Yellow
        return $false
    }
}

# Функция для запуска теста
function Run-TestScript {
    param($scriptPath, $description)
    
    Write-Host "`nRunning $description..." -ForegroundColor Yellow
    Write-Host "Script: $scriptPath" -ForegroundColor Gray
    
    $testStartTime = Get-Date
    
    try {
        if (Test-Path $scriptPath) {
            $result = & $scriptPath
            $testEndTime = Get-Date
            $duration = ($testEndTime - $testStartTime).TotalSeconds
            
            Write-Host "$description completed in $([math]::Round($duration, 2))s" -ForegroundColor Green
            return $true
        } else {
            Write-Host "Script not found: $scriptPath" -ForegroundColor Red
            return $false
        }
    }
    catch {
        $testEndTime = Get-Date
        $duration = ($testEndTime - $testStartTime).TotalSeconds
        
        Write-Host "$description failed: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Основной скрипт
Write-Host "`nStarting Essential Testing..." -ForegroundColor Cyan

# Проверяем доступность сервера
if (-not (Test-ServerAvailability)) {
    Write-Host "`nCannot proceed with testing - server is not available" -ForegroundColor Red
    Write-Host "Please start the server and try again." -ForegroundColor Yellow
    exit 1
}

# Запускаем тесты
$tests = @(
    @{ Script = "scripts\test_public_functions.ps1"; Description = "Public Functions Testing" },
    @{ Script = "scripts\test_private_functions.ps1"; Description = "Private Functions Testing" },
    @{ Script = "scripts\test_security.ps1"; Description = "Security Testing" }
)

$successCount = 0
$totalCount = $tests.Count

foreach ($test in $tests) {
    if (Run-TestScript -scriptPath $test.Script -description $test.Description) {
        $successCount++
    }
}

# Финальный анализ
$endTime = Get-Date
$totalDuration = ($endTime - $startTime).TotalMinutes
$successRate = if ($totalCount -gt 0) { ($successCount / $totalCount) * 100 } else { 0 }

Write-Host "`nTest Results Summary" -ForegroundColor Cyan
Write-Host "====================" -ForegroundColor Cyan
Write-Host "Total Tests: $totalCount" -ForegroundColor Gray
Write-Host "Successful: $successCount" -ForegroundColor Green
Write-Host "Failed: $($totalCount - $successCount)" -ForegroundColor Red
Write-Host "Success Rate: $([math]::Round($successRate, 1))%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } else { "Yellow" })
Write-Host "Total Duration: $([math]::Round($totalDuration, 2)) minutes" -ForegroundColor Gray

# Финальный вывод
Write-Host "`nTesting Complete!" -ForegroundColor Green
Write-Host "==================" -ForegroundColor Green

if ($successRate -ge 80) {
    Write-Host "Excellent! Most tests passed successfully!" -ForegroundColor Green
    Write-Host "Your application is working well!" -ForegroundColor Green
} elseif ($successRate -ge 60) {
    Write-Host "Good progress! Some tests need attention." -ForegroundColor Yellow
    Write-Host "Check the failed tests above for details." -ForegroundColor Yellow
} else {
    Write-Host "Several tests failed. Please review the results." -ForegroundColor Red
    Write-Host "Check the detailed results in the JSON files." -ForegroundColor Red
}

# Сохраняем общие результаты тестирования
$overallResults = @{
    timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    totalTests = $totalCount
    successfulTests = $successCount
    failedTests = $totalCount - $successCount
    successRate = [math]::Round($successRate, 1)
    totalDuration = [math]::Round($totalDuration, 2)
    testSuites = @(
        @{ name = "Public Functions"; file = "test_results\test_public_functions.json" },
        @{ name = "Private Functions"; file = "test_results\test_private_functions.json" },
        @{ name = "Security"; file = "test_results\test_security.json" }
    )
}

$overallResults | ConvertTo-Json -Depth 3 | Out-File -FilePath "scripts\test_results\test_overall_results.json" -Encoding UTF8

Write-Host "`nTest Results Location: scripts\test_results\" -ForegroundColor Gray
Write-Host "Overall results saved to: scripts\test_results\test_overall_results.json" -ForegroundColor Cyan
Write-Host "Thank you for using Origin App Testing Suite!" -ForegroundColor Cyan
