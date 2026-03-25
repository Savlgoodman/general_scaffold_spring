-- V12__file_center_and_task_scheduler.sql
-- 文件中心 + 调度中心

-- ========== 文件记录表 ==========
CREATE TABLE admin_file (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    object_name VARCHAR(500) NOT NULL,
    bucket_name VARCHAR(100) NOT NULL,
    url VARCHAR(1000),
    size BIGINT DEFAULT 0,
    content_type VARCHAR(100),
    category VARCHAR(50) DEFAULT 'general',
    uploader_id BIGINT,
    uploader_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'active',
    deleted_at TIMESTAMP,
    is_deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_file_status ON admin_file(status);
CREATE INDEX idx_admin_file_category ON admin_file(category);
CREATE INDEX idx_admin_file_bucket ON admin_file(bucket_name);
CREATE INDEX idx_admin_file_object ON admin_file(object_name);
CREATE INDEX idx_admin_file_uploader ON admin_file(uploader_id);

-- ========== 定时任务配置表 ==========
CREATE TABLE admin_task_config (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL UNIQUE,
    task_label VARCHAR(100) NOT NULL,
    task_group VARCHAR(50) DEFAULT 'system',
    cron_expression VARCHAR(50) NOT NULL,
    enabled INTEGER DEFAULT 1,
    description VARCHAR(255),
    last_run_time TIMESTAMP,
    last_run_status VARCHAR(20),
    is_deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 初始任务配置
INSERT INTO admin_task_config (task_name, task_label, task_group, cron_expression, description) VALUES
('api-log-cleanup', 'API日志清理', 'log', '0 0 3 * * ?', '清理超过保留天数的API请求日志'),
('operation-log-cleanup', '操作日志清理', 'log', '0 30 3 * * ?', '清理超过保留天数的操作审计日志'),
('login-log-cleanup', '登录日志清理', 'log', '0 0 4 * * ?', '清理超过保留天数的登录日志'),
('error-log-cleanup', '异常日志清理', 'log', '0 30 4 * * ?', '清理超过保留天数的异常日志'),
('orphan-file-scan', '孤儿文件扫描', 'file', '0 0 2 * * ?', '扫描无引用文件移入回收站'),
('recycle-bin-cleanup', '回收站清空', 'file', '0 0 5 * * SUN', '彻底删除回收站中超过保留天数的文件');

-- ========== 定时任务执行日志表 ==========
CREATE TABLE admin_task_log (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL,
    task_group VARCHAR(50) DEFAULT 'system',
    status VARCHAR(20) NOT NULL,
    message TEXT,
    duration_ms BIGINT,
    detail TEXT,
    is_deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_task_log_name ON admin_task_log(task_name);
CREATE INDEX idx_admin_task_log_status ON admin_task_log(status);
CREATE INDEX idx_admin_task_log_create_time ON admin_task_log(create_time);
