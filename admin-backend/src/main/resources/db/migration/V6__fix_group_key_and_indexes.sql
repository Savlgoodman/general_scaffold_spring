-- 统一 group_key 格式为下划线（与 V4 种子数据一致，修复同步脚本生成的点分隔格式）
UPDATE admin_permission
SET group_key = REPLACE(group_key, '.', '_')
WHERE group_key LIKE '%.%' AND is_deleted = 0;

-- 添加常用查询的复合索引
CREATE INDEX IF NOT EXISTS idx_role_perm_role_deleted
    ON admin_role_permission(role_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_user_override_user_deleted
    ON admin_user_permission_override(user_id, is_deleted);
