-- 创建数据库
CREATE DATABASE IF NOT EXISTS arch_ai_review CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE arch_ai_review;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建文档表
CREATE TABLE IF NOT EXISTS documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(100),
    status ENUM('UPLOADING', 'PARSING', 'PARSED', 'REVIEWING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'UPLOADING',
    upload_user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (upload_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 创建评审结果表
CREATE TABLE IF NOT EXISTS review_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    overall_score DECIMAL(5,2),
    format_score DECIMAL(5,2),
    content_score DECIMAL(5,2),
    overall_status ENUM('PASS', 'FAIL', 'NEED_REVIEW') NOT NULL DEFAULT 'NEED_REVIEW',
    ai_review_summary TEXT,
    manual_review_notes TEXT,
    manual_review_required BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- 创建问题表
CREATE TABLE IF NOT EXISTS issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_result_id BIGINT NOT NULL,
    type ENUM('FORMAT', 'CONTENT') NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    suggestion TEXT,
    page_number INT,
    line_number INT,
    status ENUM('OPEN', 'RESOLVED', 'IGNORED') NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (review_result_id) REFERENCES review_results(id) ON DELETE CASCADE
);

-- 插入默认管理员用户
INSERT IGNORE INTO users (username, password, email, role) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTBpKLt2YLS0LHMWLqwgFBpmkWsT3Loy', 'admin@example.com', 'ADMIN');

-- 创建索引
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_upload_user_id ON documents(upload_user_id);
CREATE INDEX idx_review_results_document_id ON review_results(document_id);
CREATE INDEX idx_issues_review_result_id ON issues(review_result_id);
CREATE INDEX idx_issues_type ON issues(type);
CREATE INDEX idx_issues_severity ON issues(severity);
