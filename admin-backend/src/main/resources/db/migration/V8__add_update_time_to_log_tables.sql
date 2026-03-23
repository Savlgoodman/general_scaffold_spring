-- V8__add_update_time_to_log_tables.sql
-- 为日志表补齐 update_time 字段（BaseEntity 需要）

ALTER TABLE admin_api_log ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE admin_operation_log ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE admin_error_log ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
