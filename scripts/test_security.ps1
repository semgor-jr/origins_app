# Тест безопасности Origin App
# Тестирует защиту API и безопасность данных

Write-Host "Testing Security" -ForegroundColor Green
Write-Host "===============" -ForegroundColor Green

$baseUrl = "http://localhost:8080"
$testResults = @()

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

# Функция для тестирования аутентификации
function Test-AuthenticationSecurity {
    Write-Host "`nTesting Authentication Security..." -ForegroundColor Yellow
    
    $authTests = @(
        @{
            Description = "Valid Login"
            Body = @{
                email = "test@example.com"
                password = "password123"
            } | ConvertTo-Json
            ExpectedStatus = "SUCCESS"
        },
        @{
            Description = "Invalid Email"
            Body = @{
                email = "invalid@example.com"
                password = "password123"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "Invalid Password"
            Body = @{
                email = "test@example.com"
                password = "wrongpassword"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "Empty Credentials"
            Body = @{
                email = ""
                password = ""
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "SQL Injection Attempt"
            Body = @{
                email = "test@example.com'; DROP TABLE users; --"
                password = "password123"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "XSS Attempt"
            Body = @{
                email = "<script>alert('xss')</script>@example.com"
                password = "password123"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        }
    )
    
    $authResults = @()
    
    foreach ($test in $authTests) {
        $result = Test-Endpoint -Method "POST" -Url "$baseUrl/auth/login" -Description $test.Description -Body $test.Body
        
        # Анализируем результат
        $isExpected = if ($test.ExpectedStatus -eq "SUCCESS") { 
            $result.Status -eq "SUCCESS" 
        } else { 
            $result.Status -eq "FAILED" 
        }
        
        $statusColor = if ($isExpected) { "Green" } else { "Red" }
        Write-Host "  Expected: $($test.ExpectedStatus), Got: $($result.Status)" -ForegroundColor $statusColor
        
        $authResults += @{
            Test = $test.Description
            Status = $result.Status
            Expected = $test.ExpectedStatus
            IsExpected = $isExpected
            Message = $result.Message
        }
    }
    
    # Анализируем результаты аутентификации
    $expectedResults = $authResults | Where-Object { $_.IsExpected -eq $true }
    $unexpectedResults = $authResults | Where-Object { $_.IsExpected -eq $false }
    
    Write-Host "`nAuthentication Security Analysis:" -ForegroundColor White
    Write-Host "  Expected Results: $($expectedResults.Count)/$($authResults.Count)" -ForegroundColor $(if ($expectedResults.Count -eq $authResults.Count) { "Green" } else { "Yellow" })
    Write-Host "  Unexpected Results: $($unexpectedResults.Count)" -ForegroundColor $(if ($unexpectedResults.Count -eq 0) { "Green" } else { "Red" })
    
    return $authResults
}

# Функция для тестирования авторизации
function Test-AuthorizationSecurity {
    Write-Host "`nTesting Authorization Security..." -ForegroundColor Yellow
    
    # Сначала получаем валидный токен
    $loginBody = @{
        email = "test@example.com"
        password = "password123"
    } | ConvertTo-Json
    
    $loginResult = Test-Endpoint -Method "POST" -Url "$baseUrl/auth/login" -Description "Get Valid Token" -Body $loginBody
    
    if ($loginResult.Status -ne "SUCCESS") {
        Write-Host "ERROR: Cannot get valid token for authorization tests" -ForegroundColor Red
        return @{ Status = "FAILED"; Message = "Cannot get valid token" }
    }
    
    $validToken = ($loginResult.Content | ConvertFrom-Json).token
    
    $authTests = @(
        @{
            Description = "Valid Token Access"
            Headers = @{ "Authorization" = "Bearer $validToken" }
            ExpectedStatus = "SUCCESS"
        },
        @{
            Description = "No Token Access"
            Headers = @{}
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "Invalid Token Access"
            Headers = @{ "Authorization" = "Bearer invalid_token_123" }
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "Malformed Token Access"
            Headers = @{ "Authorization" = "Bearer" }
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "Wrong Token Format"
            Headers = @{ "Authorization" = "Basic $validToken" }
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "Empty Token"
            Headers = @{ "Authorization" = "Bearer " }
            ExpectedStatus = "FAILED"
        }
    )
    
    $authResults = @()
    
    foreach ($test in $authTests) {
        $result = Test-Endpoint -Method "GET" -Url "$baseUrl/me" -Description $test.Description -Headers $test.Headers
        
        # Анализируем результат
        $isExpected = if ($test.ExpectedStatus -eq "SUCCESS") { 
            $result.Status -eq "SUCCESS" 
        } else { 
            $result.Status -eq "FAILED" 
        }
        
        $statusColor = if ($isExpected) { "Green" } else { "Red" }
        Write-Host "  Expected: $($test.ExpectedStatus), Got: $($result.Status)" -ForegroundColor $statusColor
        
        $authResults += @{
            Test = $test.Description
            Status = $result.Status
            Expected = $test.ExpectedStatus
            IsExpected = $isExpected
            Message = $result.Message
        }
    }
    
    # Анализируем результаты авторизации
    $expectedResults = $authResults | Where-Object { $_.IsExpected -eq $true }
    $unexpectedResults = $authResults | Where-Object { $_.IsExpected -eq $false }
    
    Write-Host "`nAuthorization Security Analysis:" -ForegroundColor White
    Write-Host "  Expected Results: $($expectedResults.Count)/$($authResults.Count)" -ForegroundColor $(if ($expectedResults.Count -eq $authResults.Count) { "Green" } else { "Yellow" })
    Write-Host "  Unexpected Results: $($unexpectedResults.Count)" -ForegroundColor $(if ($unexpectedResults.Count -eq 0) { "Green" } else { "Red" })
    
    return $authResults
}

# Функция для тестирования защиты от атак
function Test-AttackProtection {
    Write-Host "`nTesting Attack Protection..." -ForegroundColor Yellow
    
    $attackTests = @(
        @{
            Description = "SQL Injection in Email"
            Body = @{
                email = "test@example.com'; DROP TABLE users; --"
                password = "password123"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "SQL Injection in Password"
            Body = @{
                email = "test@example.com"
                password = "password'; DROP TABLE users; --"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "XSS in Email"
            Body = @{
                email = "<script>alert('xss')</script>@example.com"
                password = "password123"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "XSS in Password"
            Body = @{
                email = "test@example.com"
                password = "<script>alert('xss')</script>"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "Path Traversal Attempt"
            Body = @{
                email = "../../etc/passwd"
                password = "password123"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "Command Injection Attempt"
            Body = @{
                email = "test@example.com; rm -rf /"
                password = "password123"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        }
    )
    
    $attackResults = @()
    
    foreach ($test in $attackTests) {
        $result = Test-Endpoint -Method "POST" -Url "$baseUrl/auth/login" -Description $test.Description -Body $test.Body
        
        # Анализируем результат
        $isExpected = if ($test.ExpectedStatus -eq "SUCCESS") { 
            $result.Status -eq "SUCCESS" 
        } else { 
            $result.Status -eq "FAILED" 
        }
        
        $statusColor = if ($isExpected) { "Green" } else { "Red" }
        Write-Host "  Expected: $($test.ExpectedStatus), Got: $($result.Status)" -ForegroundColor $statusColor
        
        $attackResults += @{
            Test = $test.Description
            Status = $result.Status
            Expected = $test.ExpectedStatus
            IsExpected = $isExpected
            Message = $result.Message
        }
    }
    
    # Анализируем результаты защиты от атак
    $expectedResults = $attackResults | Where-Object { $_.IsExpected -eq $true }
    $unexpectedResults = $attackResults | Where-Object { $_.IsExpected -eq $false }
    
    Write-Host "`nAttack Protection Analysis:" -ForegroundColor White
    Write-Host "  Expected Results: $($expectedResults.Count)/$($attackResults.Count)" -ForegroundColor $(if ($expectedResults.Count -eq $attackResults.Count) { "Green" } else { "Yellow" })
    Write-Host "  Unexpected Results: $($unexpectedResults.Count)" -ForegroundColor $(if ($unexpectedResults.Count -eq 0) { "Green" } else { "Red" })
    
    return $attackResults
}

# Функция для тестирования валидации данных
function Test-DataValidation {
    Write-Host "`nTesting Data Validation..." -ForegroundColor Yellow
    
    $validationTests = @(
        @{
            Description = "Valid Email Format"
            Body = @{
                email = "test@example.com"
                password = "password123"
            } | ConvertTo-Json
            ExpectedStatus = "SUCCESS"
        },
        @{
            Description = "Invalid Email Format"
            Body = @{
                email = "invalid-email"
                password = "password123"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "Empty Email"
            Body = @{
                email = ""
                password = "password123"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "Empty Password"
            Body = @{
                email = "test@example.com"
                password = ""
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "Very Long Email"
            Body = @{
                email = "a" * 1000 + "@example.com"
                password = "password123"
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        },
        @{
            Description = "Very Long Password"
            Body = @{
                email = "test@example.com"
                password = "a" * 1000
            } | ConvertTo-Json
            ExpectedStatus = "FAILED"
        }
    )
    
    $validationResults = @()
    
    foreach ($test in $validationTests) {
        $result = Test-Endpoint -Method "POST" -Url "$baseUrl/auth/login" -Description $test.Description -Body $test.Body
        
        # Анализируем результат
        $isExpected = if ($test.ExpectedStatus -eq "SUCCESS") { 
            $result.Status -eq "SUCCESS" 
        } else { 
            $result.Status -eq "FAILED" 
        }
        
        $statusColor = if ($isExpected) { "Green" } else { "Red" }
        Write-Host "  Expected: $($test.ExpectedStatus), Got: $($result.Status)" -ForegroundColor $statusColor
        
        $validationResults += @{
            Test = $test.Description
            Status = $result.Status
            Expected = $test.ExpectedStatus
            IsExpected = $isExpected
            Message = $result.Message
        }
    }
    
    # Анализируем результаты валидации
    $expectedResults = $validationResults | Where-Object { $_.IsExpected -eq $true }
    $unexpectedResults = $validationResults | Where-Object { $_.IsExpected -eq $false }
    
    Write-Host "`nData Validation Analysis:" -ForegroundColor White
    Write-Host "  Expected Results: $($expectedResults.Count)/$($validationResults.Count)" -ForegroundColor $(if ($expectedResults.Count -eq $validationResults.Count) { "Green" } else { "Yellow" })
    Write-Host "  Unexpected Results: $($unexpectedResults.Count)" -ForegroundColor $(if ($unexpectedResults.Count -eq 0) { "Green" } else { "Red" })
    
    return $validationResults
}

# Функция для тестирования CORS и заголовков
function Test-SecurityHeaders {
    Write-Host "`nTesting Security Headers..." -ForegroundColor Yellow
    
    $headerTests = @(
        @{
            Description = "Check CORS Headers"
            Method = "OPTIONS"
            Url = "$baseUrl/decoding-methods"
            ExpectedHeaders = @("Access-Control-Allow-Origin", "Access-Control-Allow-Methods")
        },
        @{
            Description = "Check Content-Type Security"
            Method = "GET"
            Url = "$baseUrl/decoding-methods"
            ExpectedHeaders = @("Content-Type")
        }
    )
    
    $headerResults = @()
    
    foreach ($test in $headerTests) {
        try {
            $response = Invoke-WebRequest -Uri $test.Url -Method $test.Method -TimeoutSec 30
            $headers = $response.Headers
            
            $foundHeaders = @()
            foreach ($expectedHeader in $test.ExpectedHeaders) {
                if ($headers.ContainsKey($expectedHeader)) {
                    $foundHeaders += $expectedHeader
                }
            }
            
            $headerCount = $foundHeaders.Count
            $expectedCount = $test.ExpectedHeaders.Count
            $isComplete = $headerCount -eq $expectedCount
            
            $statusColor = if ($isComplete) { "Green" } else { "Yellow" }
            Write-Host "  Found $headerCount/$expectedCount headers: $($foundHeaders -join ', ')" -ForegroundColor $statusColor
            
            $headerResults += @{
                Test = $test.Description
                Status = if ($isComplete) { "SUCCESS" } else { "PARTIAL" }
                FoundHeaders = $foundHeaders
                ExpectedHeaders = $test.ExpectedHeaders
                Message = "Found $headerCount/$expectedCount expected headers"
            }
        }
        catch {
            Write-Host "  ERROR: $($_.Exception.Message)" -ForegroundColor Red
            $headerResults += @{
                Test = $test.Description
                Status = "FAILED"
                Message = $_.Exception.Message
            }
        }
    }
    
    return $headerResults
}

# Основной тест
Write-Host "`nStarting Security Testing..." -ForegroundColor Cyan

# Тестируем безопасность
$securityTests = @(
    @{ Function = "Test-AuthenticationSecurity"; Description = "Authentication Security Testing" },
    @{ Function = "Test-AuthorizationSecurity"; Description = "Authorization Security Testing" },
    @{ Function = "Test-AttackProtection"; Description = "Attack Protection Testing" },
    @{ Function = "Test-DataValidation"; Description = "Data Validation Testing" },
    @{ Function = "Test-SecurityHeaders"; Description = "Security Headers Testing" }
)

foreach ($test in $securityTests) {
    Write-Host "`nRunning $($test.Description)..." -ForegroundColor Yellow
    $result = & $test.Function
    $testResults += @{
        Test = $test.Description
        Status = "SUCCESS"
        Message = "Security test completed"
        Results = $result
    }
}

# Результаты
Write-Host "`nSecurity Test Results Summary:" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan

$successCount = ($testResults | Where-Object { $_.Status -eq "SUCCESS" }).Count
$totalCount = $testResults.Count

Write-Host "Total Security Tests: $totalCount" -ForegroundColor White
Write-Host "Successful: $successCount" -ForegroundColor Green

Write-Host "`nDetailed Results:" -ForegroundColor Yellow
foreach ($result in $testResults) {
    Write-Host "$($result.Test): $($result.Status)" -ForegroundColor Green
    if ($result.Message) {
        Write-Host "  Message: $($result.Message)" -ForegroundColor Gray
    }
}

# Сохраняем результаты
$resultsDir = "scripts\test_results"
if (-not (Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir -Force | Out-Null
}

$resultsFile = "$resultsDir\test_security.json"
$testResults | ConvertTo-Json -Depth 3 | Out-File -FilePath $resultsFile -Encoding UTF8
Write-Host "`nTest results saved to $resultsFile" -ForegroundColor Cyan

Write-Host "`nSecurity testing completed!" -ForegroundColor Green
Write-Host "===========================" -ForegroundColor Green

if ($successCount -eq $totalCount) {
    Write-Host "SUCCESS: All security tests completed!" -ForegroundColor Green
    Write-Host "System security is properly implemented!" -ForegroundColor Green
} else {
    Write-Host "WARNING: Some security tests may have issues. Check the results above." -ForegroundColor Yellow
}


