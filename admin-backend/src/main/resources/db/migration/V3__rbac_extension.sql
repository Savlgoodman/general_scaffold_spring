-- V3__rbac_extension.sql
-- RBAC系统扩展字段

-- 1. admin_user 表新增超级管理员标识
ALTER TABLE admin_user ADD COLUMN is_superuser INTEGER DEFAULT 0;
COMMENT ON COLUMN admin_user.is_superuser IS '是否超级管理员（1-是 0-否）';

-- 2. admin_permission 表新增组权限相关字段
ALTER TABLE admin_permission ADD COLUMN group_key VARCHAR(100);
COMMENT ON COLUMN admin_permission.group_key IS '分组标识，如: admin_users';

ALTER TABLE admin_permission ADD COLUMN group_name VARCHAR(100);
COMMENT ON COLUMN admin_permission.group_name IS '分组名称，如: 用户管理';

ALTER TABLE admin_permission ADD COLUMN is_group INTEGER DEFAULT 0;
COMMENT ON COLUMN admin_permission.is_group IS '是否组权限（1-是 0-否）';

ALTER TABLE admin_permission ADD COLUMN status INTEGER DEFAULT 1;
COMMENT ON COLUMN admin_permission.status IS '状态（1-启用 0-禁用）';

-- 3. admin_role_permission 表新增effect和priority字段
ALTER TABLE admin_role_permission ADD COLUMN effect VARCHAR(10) DEFAULT 'GRANT';
COMMENT ON COLUMN admin_role_permission.effect IS 'GRANT-允许 DENY-拒绝';

ALTER TABLE admin_role_permission ADD COLUMN priority INTEGER DEFAULT 0;
COMMENT ON COLUMN admin_role_permission.priority IS '优先级（0-100，越大越优先）';

-- 4. 新增索引
CREATE INDEX idx_admin_user_is_superuser ON admin_user(is_superuser);
CREATE INDEX idx_admin_permission_group_key ON admin_permission(group_key);
CREATE INDEX idx_admin_permission_is_group ON admin_permission(is_group);
CREATE INDEX idx_admin_permission_status ON admin_permission(status);
CREATE INDEX idx_admin_role_permission_effect ON admin_role_permission(effect);
CREATE INDEX idx_admin_role_permission_priority ON admin_role_permission(priority);
