-- 为 admin_login_log 表添加更新时间字段（与 BaseEntity 保持一致）
ALTER TABLE admin_login_log ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 为已存在的记录填充 update_time
UPDATE admin_login_log SET update_time = create_time WHERE update_time IS NULL;
