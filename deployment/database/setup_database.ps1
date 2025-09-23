# Скрипт для настройки базы данных Origin App
# PostgreSQL на Яндекс.Облаке

param(
    [Parameter(Mandatory=$true)]
    [string]$DatabaseHost,
    
    [Parameter(Mandatory=$true)]
    [string]$DatabaseName,
    
    [Parameter(Mandatory=$true)]
    [string]$DatabaseUser,
    
    [Parameter(Mandatory=$true)]
    [string]$DatabasePassword,
    
    [Parameter(Mandatory=$false)]
    [int]$DatabasePort = 6432
)

Write-Host "Setting up Origin App Database" -ForegroundColor Green
Write-Host "=============================" -ForegroundColor Green

Write-Host "Database Host: $DatabaseHost" -ForegroundColor Cyan
Write-Host "Database Port: $DatabasePort" -ForegroundColor Cyan
Write-Host "Database Name: $DatabaseName" -ForegroundColor Cyan
Write-Host "Database User: $DatabaseUser" -ForegroundColor Cyan

# Проверяем наличие psql
Write-Host "`n1. Checking PostgreSQL client..." -ForegroundColor Yellow
try {
    psql --version | Out-Null
    Write-Host "✅ PostgreSQL client is available" -ForegroundColor Green
} catch {
    Write-Host "❌ PostgreSQL client not found. Please install PostgreSQL client tools." -ForegroundColor Red
    Write-Host "Download from: https://www.postgresql.org/download/windows/" -ForegroundColor Yellow
    exit 1
}

# Создаем строку подключения
$connectionString = "host=$DatabaseHost port=$DatabasePort dbname=$DatabaseName user=$DatabaseUser sslmode=require"

Write-Host "`n2. Testing database connection..." -ForegroundColor Yellow
try {
    $env:PGPASSWORD = $DatabasePassword
    psql $connectionString -c "SELECT version();" | Out-Null
    Write-Host "✅ Database connection successful" -ForegroundColor Green
} catch {
    Write-Host "❌ Database connection failed" -ForegroundColor Red
    Write-Host "Please check your database credentials and network access" -ForegroundColor Yellow
    exit 1
}

Write-Host "`n3. Creating database tables..." -ForegroundColor Yellow
try {
    $env:PGPASSWORD = $DatabasePassword
    psql $connectionString -f "create_tables.sql"
    Write-Host "✅ Database tables created successfully" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to create tables" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Yellow
    exit 1
}

Write-Host "`n4. Loading news data..." -ForegroundColor Yellow
try {
    $env:PGPASSWORD = $DatabasePassword
    psql $connectionString -f "load_news.sql"
    Write-Host "✅ News data loaded successfully" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to load news data" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Yellow
    exit 1
}

Write-Host "`n5. Verifying database setup..." -ForegroundColor Yellow
try {
    $env:PGPASSWORD = $DatabasePassword
    psql $connectionString -f "check_database.sql"
    Write-Host "✅ Database verification completed" -ForegroundColor Green
} catch {
    Write-Host "❌ Database verification failed" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Yellow
    exit 1
}

Write-Host "`n🎉 Database setup completed successfully!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green

Write-Host "`nDatabase is ready for Origin App!" -ForegroundColor Cyan
Write-Host "Connection details:" -ForegroundColor Yellow
Write-Host "  Host: $DatabaseHost" -ForegroundColor White
Write-Host "  Port: $DatabasePort" -ForegroundColor White
Write-Host "  Database: $DatabaseName" -ForegroundColor White
Write-Host "  User: $DatabaseUser" -ForegroundColor White

Write-Host "`nNext steps:" -ForegroundColor Cyan
Write-Host "1. Update your application configuration" -ForegroundColor White
Write-Host "2. Deploy your application" -ForegroundColor White
Write-Host "3. Test the application with the new database" -ForegroundColor White

# Очищаем переменную окружения
Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue


