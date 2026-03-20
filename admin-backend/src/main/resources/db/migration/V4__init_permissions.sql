-- V4__init_permissions.sql
-- 初始化权限数据

-- 插入组权限
INSERT INTO admin_permission (name, code, type, method, path, group_key, group_name, is_group, status, sort) VALUES
('用户管理', 'admin_users_group', 'api', '*', '/api/admin/admin-users/**', 'admin_users', '用户管理', 1, 1, 1),
('角色管理', 'admin_roles_group', 'api', '*', '/api/admin/roles/**', 'admin_roles', '角色管理', 1, 1, 2),
('权限管理', 'admin_permissions_group', 'api', '*', '/api/admin/permissions/**', 'admin_permissions', '权限管理', 1, 1, 3),
('菜单管理', 'admin_menus_group', 'api', '*', '/api/admin/menus/**', 'admin_menus', '菜单管理', 1, 1, 4),
('系统管理', 'admin_system_group', 'api', '*', '/api/admin/system/**', 'admin_system', '系统管理', 1, 1, 5);

-- 插入子权限 - 用户管理
INSERT INTO admin_permission (name, code, type, method, path, group_key, group_name, is_group, status, sort) VALUES
('用户列表', 'admin_users_list', 'api', 'GET', '/api/admin/admin-users', 'admin_users', '用户管理', 0, 1, 1),
('用户详情', 'admin_users_detail', 'api', 'GET', '/api/admin/admin-users/*', 'admin_users', '用户管理', 0, 1, 2),
('创建用户', 'admin_users_create', 'api', 'POST', '/api/admin/admin-users', 'admin_users', '用户管理', 0, 1, 3),
('更新用户', 'admin_users_update', 'api', 'PUT', '/api/admin/admin-users/*', 'admin_users', '用户管理', 0, 1, 4),
('删除用户', 'admin_users_delete', 'api', 'DELETE', '/api/admin/admin-users/*', 'admin_users', '用户管理', 0, 1, 5);

-- 插入子权限 - 角色管理
INSERT INTO admin_permission (name, code, type, method, path, group_key, group_name, is_group, status, sort) VALUES
('角色列表', 'admin_roles_list', 'api', 'GET', '/api/admin/roles', 'admin_roles', '角色管理', 0, 1, 1),
('角色详情', 'admin_roles_detail', 'api', 'GET', '/api/admin/roles/*', 'admin_roles', '角色管理', 0, 1, 2),
('创建角色', 'admin_roles_create', 'api', 'POST', '/api/admin/roles', 'admin_roles', '角色管理', 0, 1, 3),
('更新角色', 'admin_roles_update', 'api', 'PUT', '/api/admin/roles/*', 'admin_roles', '角色管理', 0, 1, 4),
('删除角色', 'admin_roles_delete', 'api', 'DELETE', '/api/admin/roles/*', 'admin_roles', '角色管理', 0, 1, 5),
('角色权限详情', 'admin_roles_permissions_detail', 'api', 'GET', '/api/admin/roles/*/permissions', 'admin_roles', '角色管理', 0, 1, 6),
('角色可分配权限', 'admin_roles_permissions_assignable', 'api', 'GET', '/api/admin/roles/*/permissions/assignable', 'admin_roles', '角色管理', 0, 1, 7),
('分配组权限', 'admin_roles_permissions_assign_groups', 'api', 'POST', '/api/admin/roles/*/permissions/groups', 'admin_roles', '角色管理', 0, 1, 8),
('分配子权限', 'admin_roles_permissions_assign_children', 'api', 'POST', '/api/admin/roles/*/permissions/children', 'admin_roles', '角色管理', 0, 1, 9),
('撤销角色权限', 'admin_roles_permissions_revoke', 'api', 'DELETE', '/api/admin/roles/*/permissions', 'admin_roles', '角色管理', 0, 1, 10);

-- 插入子权限 - 权限管理
INSERT INTO admin_permission (name, code, type, method, path, group_key, group_name, is_group, status, sort) VALUES
('权限列表', 'admin_permissions_list', 'api', 'GET', '/api/admin/permissions', 'admin_permissions', '权限管理', 0, 1, 1),
('权限详情', 'admin_permissions_detail', 'api', 'GET', '/api/admin/permissions/*', 'admin_permissions', '权限管理', 0, 1, 2),
('创建权限', 'admin_permissions_create', 'api', 'POST', '/api/admin/permissions', 'admin_permissions', '权限管理', 0, 1, 3),
('更新权限', 'admin_permissions_update', 'api', 'PUT', '/api/admin/permissions/*', 'admin_permissions', '权限管理', 0, 1, 4),
('删除权限', 'admin_permissions_delete', 'api', 'DELETE', '/api/admin/permissions/*', 'admin_permissions', '权限管理', 0, 1, 5),
('权限分组', 'admin_permissions_groups', 'api', 'GET', '/api/admin/permissions/groups', 'admin_permissions', '权限管理', 0, 1, 6);

-- 插入子权限 - 菜单管理
INSERT INTO admin_permission (name, code, type, method, path, group_key, group_name, is_group, status, sort) VALUES
('菜单列表', 'admin_menus_list', 'api', 'GET', '/api/admin/menus', 'admin_menus', '菜单管理', 0, 1, 1),
('菜单详情', 'admin_menus_detail', 'api', 'GET', '/api/admin/menus/*', 'admin_menus', '菜单管理', 0, 1, 2),
('创建菜单', 'admin_menus_create', 'api', 'POST', '/api/admin/menus', 'admin_menus', '菜单管理', 0, 1, 3),
('更新菜单', 'admin_menus_update', 'api', 'PUT', '/api/admin/menus/*', 'admin_menus', '菜单管理', 0, 1, 4),
('删除菜单', 'admin_menus_delete', 'api', 'DELETE', '/api/admin/menus/*', 'admin_menus', '菜单管理', 0, 1, 5);

-- 插入子权限 - 系统管理
INSERT INTO admin_permission (name, code, type, method, path, group_key, group_name, is_group, status, sort) VALUES
('系统配置', 'admin_system_config', 'api', 'GET', '/api/admin/system/config', 'admin_system', '系统管理', 0, 1, 1),
('更新配置', 'admin_system_config_update', 'api', 'PUT', '/api/admin/system/config', 'admin_system', '系统管理', 0, 1, 2),
('系统信息', 'admin_system_info', 'api', 'GET', '/api/admin/system_info/*', 'admin_system', '系统管理', 0, 1, 3);

-- 将所有权限分配给超级管理员角色(role_id=1)，高优先级
INSERT INTO admin_role_permission (role_id, permission_id, effect, priority)
SELECT 1, id, 'GRANT', 100 FROM admin_permission WHERE is_deleted = 0;

-- 更新超级管理员用户为超级管理员
UPDATE admin_user SET is_superuser = 1 WHERE username = 'admin';
