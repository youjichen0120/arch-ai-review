-- 创建初始管理员用户（密码为 admin，使用BCrypt加密）
INSERT INTO users (username, password, email, role)
VALUES ('admin', '$2a$10$LY0Wi.dCOyOI5Mwxe8mCE.NFVeV68LUOYYTCnRDBs9hd0Ctq5VIHu', 'admin@example.com', 'ADMIN')
ON DUPLICATE KEY UPDATE username=username;