-- Создание таблиц для Origin App


-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    autosomal_data TEXT,
    y_haplogroup VARCHAR(50),
    mt_haplogroup VARCHAR(50),
    verification_code VARCHAR(10),
    is_email_verified BOOLEAN DEFAULT FALSE,
    verification_code_expires BIGINT
);

-- Таблица отчетов
CREATE TABLE IF NOT EXISTS reports (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    decoding_method VARCHAR(100) NOT NULL,
    summary TEXT,
    origins_json TEXT,
    public_id VARCHAR(36) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Таблица новостей
CREATE TABLE IF NOT EXISTS news (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(1000),
    news_source VARCHAR(255),
    published_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов для оптимизации
CREATE INDEX IF NOT EXISTS idx_users_contact ON users(contact);
CREATE INDEX IF NOT EXISTS idx_users_verification ON users(verification_code);
CREATE INDEX IF NOT EXISTS idx_reports_user_id ON reports(user_id);
CREATE INDEX IF NOT EXISTS idx_reports_method ON reports(decoding_method);
CREATE INDEX IF NOT EXISTS idx_news_published ON news(published_at DESC);

-- Создание последовательности для ID
CREATE SEQUENCE IF NOT EXISTS users_id_seq;
CREATE SEQUENCE IF NOT EXISTS reports_id_seq;
CREATE SEQUENCE IF NOT EXISTS news_id_seq;

-- Настройка прав доступа
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO origin_admin;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO origin_admin;

-- Проверка создания таблиц
SELECT 'Tables created successfully' as status;
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;


