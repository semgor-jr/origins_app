-- Проверка базы данных Origin App
-- PostgreSQL на Яндекс.Облаке

-- Проверяем версию PostgreSQL
SELECT version() as postgresql_version;

-- Проверяем существующие таблицы
SELECT 'Database tables:' as info;
SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;

-- Проверяем количество записей в каждой таблице
SELECT 'Record counts:' as info;
SELECT 'users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'reports', COUNT(*) FROM reports
UNION ALL
SELECT 'news', COUNT(*) FROM news;

-- Проверяем структуру таблицы users
SELECT 'Users table structure:' as info;
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'users' AND table_schema = 'public'
ORDER BY ordinal_position;

-- Проверяем структуру таблицы reports
SELECT 'Reports table structure:' as info;
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'reports' AND table_schema = 'public'
ORDER BY ordinal_position;

-- Проверяем структуру таблицы news
SELECT 'News table structure:' as info;
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'news' AND table_schema = 'public'
ORDER BY ordinal_position;

-- Показываем последние новости
SELECT 'Recent news:' as info;
SELECT id, title, published_at FROM news ORDER BY published_at DESC LIMIT 3;

-- Проверяем индексы
SELECT 'Database indexes:' as info;
SELECT indexname, tablename, indexdef 
FROM pg_indexes 
WHERE schemaname = 'public' 
ORDER BY tablename, indexname;

-- Проверяем права доступа
SELECT 'Database permissions:' as info;
SELECT table_name, privilege_type, grantee 
FROM information_schema.table_privileges 
WHERE table_schema = 'public' 
ORDER BY table_name, privilege_type;


