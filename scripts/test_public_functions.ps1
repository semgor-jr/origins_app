# Тест публичных функций Origin App

Write-Host "Testing Public Functions" -ForegroundColor Green
Write-Host "=======================" -ForegroundColor Green

$baseUrl = "http://localhost:8080"
$testResults = @()

# Функция для тестирования эндпоинта
function Test-Endpoint {
    param($Method, $Url, $Description)
    
    try {
        $response = Invoke-WebRequest -Uri $Url -Method $Method -TimeoutSec 30
        Write-Host "SUCCESS: $Description - Status: $($response.StatusCode)" -ForegroundColor Green
        return @{ Status = "SUCCESS"; Code = $response.StatusCode; Message = "OK" }
    }
    catch {
        Write-Host "ERROR: $Description - $($_.Exception.Message)" -ForegroundColor Red
        return @{ Status = "FAILED"; Message = $_.Exception.Message }
    }
}

Write-Host "`nTesting Public Endpoints..." -ForegroundColor Yellow

# Тестируем публичные эндпоинты
$publicTests = @(
    @{ Method = "GET"; Url = "$baseUrl/decoding-methods"; Description = "GET /decoding-methods" },
    @{ Method = "GET"; Url = "$baseUrl/news"; Description = "GET /news" }
)

foreach ($test in $publicTests) {
    $result = Test-Endpoint -Method $test.Method -Url $test.Url -Description $test.Description
    $testResults += @{
        Test = $test.Description
        Status = $result.Status
        Message = $result.Message
    }
}

Write-Host "`nTesting News Content..." -ForegroundColor Yellow

# Тестируем содержимое новостей
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/news" -Method GET -TimeoutSec 30
    $newsData = $response.Content | ConvertFrom-Json
    
    Write-Host "SUCCESS: Retrieved $($newsData.Count) news items" -ForegroundColor Green
    
    # Анализируем новости
    $newsWithImages = ($newsData | Where-Object { $_.imageUrl -and $_.imageUrl -ne "" }).Count
    $newsWithSources = ($newsData | Where-Object { $_.source -and $_.source -ne "" }).Count
    
    Write-Host "News Analysis:" -ForegroundColor White
    Write-Host "  Total news: $($newsData.Count)" -ForegroundColor Gray
    Write-Host "  With images: $newsWithImages" -ForegroundColor Gray
    Write-Host "  With sources: $newsWithSources" -ForegroundColor Gray
    
    $testResults += @{
        Test = "News Content Analysis"
        Status = "SUCCESS"
        Message = "Analyzed $($newsData.Count) news items"
    }
}
catch {
    Write-Host "ERROR: Failed to analyze news content: $($_.Exception.Message)" -ForegroundColor Red
    $testResults += @{
        Test = "News Content Analysis"
        Status = "FAILED"
        Message = $_.Exception.Message
    }
}

Write-Host "`nTesting Decoding Methods..." -ForegroundColor Yellow

# Тестируем методы декодирования
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/decoding-methods" -Method GET -TimeoutSec 30
    $methodsData = $response.Content | ConvertFrom-Json
    
    Write-Host "SUCCESS: Retrieved $($methodsData.Count) decoding methods" -ForegroundColor Green
    
    # Анализируем методы
    Write-Host "Available Methods:" -ForegroundColor White
    foreach ($method in $methodsData) {
        Write-Host "  - $($method.name): $($method.description)" -ForegroundColor Gray
    }
    
    $testResults += @{
        Test = "Decoding Methods Analysis"
        Status = "SUCCESS"
        Message = "Retrieved $($methodsData.Count) methods"
    }
}
catch {
    Write-Host "ERROR: Failed to analyze decoding methods: $($_.Exception.Message)" -ForegroundColor Red
    $testResults += @{
        Test = "Decoding Methods Analysis"
        Status = "FAILED"
        Message = $_.Exception.Message
    }
}

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
$testResults | ConvertTo-Json -Depth 3 | Out-File -FilePath "scripts\test_results\test_public_functions.json" -Encoding UTF8
Write-Host "`nTest results saved to scripts\test_results\test_public_functions.json" -ForegroundColor Cyan

Write-Host "`nPublic functions testing completed!" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Green

if ($failedCount -eq 0) {
    Write-Host "SUCCESS: All public function tests passed!" -ForegroundColor Green
    Write-Host "Origin App public API is working correctly!" -ForegroundColor Green
} else {
    Write-Host "WARNING: Some public function tests failed. Check the results above." -ForegroundColor Yellow
}
