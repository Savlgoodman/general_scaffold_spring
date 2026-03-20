# RBAC 系统实现规划

## 一、现状分析

### 1.1 现有实体结构

| 实体 | 表名 | 现状 |
|------|------|------|
| AdminUser | admin_user | 缺少 `is_superuser` 字段 |
| AdminRole | admin_role | 结构完整 |
| AdminPermission | admin_permission | 缺少组权限相关字段（group_key, is_group, resource_pattern/status） |
| AdminRolePermission | admin_role_permission | 缺少 `effect` 和 `priority` 字段 |
| AdminUserPermissionOverride | admin_user_permission_override | 结构完整 |

### 1.2 需要改造的数据库表

```sql
-- 1. admin_user 表需要新增字段
ALTER TABLE admin_user ADD COLUMN is_superuser INTEGER DEFAULT 0;

-- 2. admin_permission 表需要新增字段
ALTER TABLE admin_permission ADD COLUMN group_key VARCHAR(100);
ALTER TABLE admin_permission ADD COLUMN group_name VARCHAR(100);
ALTER TABLE admin_permission ADD COLUMN is_group INTEGER DEFAULT 0;
ALTER TABLE admin_permission ADD COLUMN status INTEGER DEFAULT 1;

-- 3. admin_role_permission 表需要新增字段
ALTER TABLE admin_role_permission ADD COLUMN effect VARCHAR(10) DEFAULT 'GRANT';
ALTER TABLE admin_role_permission ADD COLUMN priority INTEGER DEFAULT 0;
```

---

## 二、核心 Service 设计

### 2.1 PermissionService（权限计算核心）

**职责**：权限的CRUD、通配符匹配、权限来源追踪

#### 2.1.1 基础查询方法

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `getById(id)` | Long id | AdminPermission | 根据ID获取权限 |
| `getByCode(code)` | String code | AdminPermission | 根据code获取权限 |
| `getAll()` | - | List<AdminPermission> | 获取所有权限 |
| `getByGroupKey(groupKey)` | String groupKey | List<AdminPermission> | 根据groupKey获取同组权限 |
| `getActivePermissions()` | - | List<AdminPermission> | 获取所有启用的权限 |

#### 2.1.2 权限匹配方法

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `matchPattern(pattern, path)` | String pattern, String path | boolean | 通配符模式匹配 |
| `findMatchingPermissions(path, method)` | String path, String method | List<AdminPermission> | 根据路径查找匹配的权限 |

#### 2.1.3 组权限相关方法

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `isGroupPermission(permissionId)` | Long permissionId | boolean | 判断是否是组权限 |
| `getChildPermissions(groupKey)` | String groupKey | List<AdminPermission> | 获取组下的子权限 |
| `isChildOfGroup(childPermId, parentPermId)` | Long childPermId, Long parentPermId | boolean | 判断子权限是否属于某组 |
| `expandGroupPermission(groupPerm)` | AdminPermission groupPerm | List<AdminPermission> | 展开组权限为子权限列表 |
| `getAllGroupedPermissions()` | - | List<PermissionGroupVO> | 获取所有分组权限（用于分配界面） |

**PermissionGroupVO 结构**：
```java
{
    groupKey: String,
    groupName: String,
    groupPermission: AdminPermission,    // 组权限本身
    children: List<AdminPermission>,    // 子权限列表
    totalCount: Integer
}
```

---

### 2.2 RoleService（角色管理服务）

**职责**：角色的CRUD、角色-权限关联管理

#### 2.2.1 基础查询方法

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `getById(id)` | Long id | AdminRole | 根据ID获取角色 |
| `getByCode(code)` | String code | AdminRole | 根据code获取角色 |
| `getAll()` | - | List<AdminRole> | 获取所有角色 |
| `getActiveRoles()` | - | List<AdminRole> | 获取所有正常状态的角色 |

#### 2.2.2 角色-权限关联方法

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `assignPermissions(roleId, permissionIds)` | Long roleId, List<Long> permissionIds | void | 分配权限给角色（批量） |
| `removePermissions(roleId, permissionIds)` | Long roleId, List<Long> permissionIds | void | 移除角色的权限（批量） |
| `getRolePermissions(roleId)` | Long roleId | List<AdminPermission> | 获取角色的所有权限 |
| `getRolePermissionIds(roleId)` | Long roleId | Set<Long> | 获取角色的权限ID集合 |
| `syncRolePermissions(roleId, permissionIds)` | Long roleId, List<Long> permissionIds | void | 同步角色权限（先删后增） |

#### 2.2.3 角色-权限详情方法（带来源追踪）

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `getRolePermissionsDetail(roleId)` | Long roleId | RolePermissionDetailVO | 获取角色权限详情（含来源） |
| `getRoleAssignablePermissions(roleId)` | Long roleId | RoleAssignablePermissionsVO | 获取角色可分配的权限 |
| `assignGroupPermissions(roleId, permissions)` | Long roleId, List<RolePermissionDTO> | void | 分配组权限 |
| `assignChildPermissions(roleId, permissions)` | Long roleId, List<RolePermissionDTO> | void | 分配子权限 |
| `revokePermissions(roleId, permissionIds)` | Long roleId, List<Long> permissionIds | void | 批量撤销角色权限 |

**RolePermissionDetailVO 结构**：
```java
{
    roleId: Long,
    roleName: String,
    roleCode: String,
    permissions: [
        {
            permissionId: Long,
            permissionName: String,
            path: String,
            method: String,
            isGroup: Boolean,
            groupKey: String,
            groupName: String,
            effect: String,        // GRANT/DENY
            priority: Integer,
            source: "ROLE"         // 权限来源标识
        }
    ],
    groupedPermissions: [
        {
            groupKey: String,
            groupName: String,
            groupPermission: { permission detail },
            children: [ permission details ],
            totalCount: Integer,
            assignedCount: Integer
        }
    ],
    summary: {
        totalPermissions: Integer,
        groupPermissions: Integer,
        childPermissions: Integer,
        grantCount: Integer,
        denyCount: Integer
    }
}
```

**RoleAssignablePermissionsVO 结构**：
```java
{
    roleId: Long,
    groups: [
        {
            groupKey: String,
            groupName: String,
            groupPermission: {
                id: Long,
                name: String,
                path: String,
                method: String,
                isAssigned: Boolean,
                effect: String,
                priority: Integer
            },
            unassignedChildren: [
                {
                    id: Long,
                    name: String,
                    path: String,
                    method: String,
                    isAssigned: Boolean
                }
            ]
        }
    ]
}
```

**RolePermissionDTO（请求参数）**：
```java
{
    permissionId: Long,
    effect: String,      // GRANT / DENY
    priority: Integer    // 优先级
}
```

---

### 2.3 RBACService（RBAC核心服务）

**职责**：用户-角色关联、用户权限计算、权限检查

#### 2.3.1 用户-角色关联方法

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `assignUserRoles(userId, roleIds)` | Long userId, List<Long> roleIds | void | 分配角色给用户（批量） |
| `removeUserRoles(userId, roleIds)` | Long userId, List<Long> roleIds | void | 移除用户的角色（批量） |
| `getUserRoles(userId)` | Long userId | List<AdminRole> | 获取用户的所有角色 |
| `getUserRoleIds(userId)` | Long userId | Set<Long> | 获取用户角色ID集合 |
| `getUserRoleCodes(userId)` | Long userId | Set<String> | 获取用户角色编码集合 |
| `hasRole(userId, roleCode)` | Long userId, String roleCode | boolean | 判断用户是否有某角色 |
| `syncUserRoles(userId, roleIds)` | Long userId, List<Long> roleIds | void | 同步用户角色（先删后增） |

#### 2.3.2 权限计算核心方法

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `getUserPermissions(userId)` | Long userId | Set<AdminPermission> | **【核心】** 获取用户所有有效权限 |
| `getUserPermissionIds(userId)` | Long userId | Set<Long> | 获取用户权限ID集合 |
| `getUserRolePermissions(userId)` | Long userId | Map<Long, List<AdminPermission>> | 获取用户各角色的权限（角色ID -> 权限列表） |

**权限计算流程**：
```
getUserPermissions(userId)
    │
    ├── 1. 检查用户是否是超级管理员（is_superuser=true）
    │       └── 是 → 返回所有权限
    │
    ├── 2. 获取用户所有角色
    │       └── getUserRoles(userId)
    │
    ├── 3. 遍历每个角色，获取角色权限
    │       └── for each role: getRolePermissions(roleId)
    │
    ├── 4. 合并多角色权限（ALLOW优先于DENY）
    │       └── mergeRolePermissions(rolePermissionsList)
    │
    ├── 5. 应用用户权限覆盖
    │       └── getUserPermissionOverrides(userId)
    │       └── 覆盖规则：GRANT优先于DENY
    │
    └── 6. 返回最终权限集合
```

#### 2.3.3 权限检查方法

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `checkPermission(userId, path, method)` | Long userId, String path, String method | boolean | 检查用户是否有API权限（简单返回） |
| `checkPermissionDetail(userId, path, method)` | Long userId, String path, String method | PermissionCheckResultVO | **【核心】** 检查权限并返回详细信息 |

**PermissionCheckResultVO 结构**：
```java
{
    allowed: Boolean,              // 是否允许
    matchedPermission: {           // 匹配的权限详情
        permissionId: Long,
        permissionName: String,
        path: String,
        method: String
    },
    source: String,                // 权限来源: SUPER_USER / USER_OVERRIDE / ROLE
    sourceDetails: [               // 详细来源信息
        {
            type: "ROLE",          // SUPER_USER / ROLE / USER_OVERRIDE
            roleId: Long,
            roleName: String,
            permissionId: Long,
            permissionName: String,
            effect: String,         // GRANT / DENY
            priority: Integer,
            isGroup: Boolean
        }
    ],
    denyReason: String             // 拒绝原因（仅被拒绝时填充）
}
```

#### 2.3.4 用户权限覆盖方法

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setUserPermissionOverride(userId, permissionId, effect)` | Long userId, Long permissionId, String effect | void | 设置用户权限覆盖 |
| `removeUserPermissionOverride(userId, overrideId)` | Long userId, Long overrideId | void | 删除用户权限覆盖 |
| `getUserPermissionOverrides(userId)` | Long userId | List<AdminUserPermissionOverride> | 获取用户权限覆盖列表 |
| `clearUserPermissionOverrides(userId)` | Long userId | void | 清除用户所有权限覆盖 |

#### 2.3.5 用户权限详情方法（带来源追踪）

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `getUserPermissionsDetail(userId)` | Long userId | UserPermissionDetailVO | 获取用户权限详情（含来源） |
| `getUserEffectivePermissions(userId)` | Long userId | UserEffectivePermissionsVO | 获取用户最终有效权限（已计算最终效果） |
| `getUserAvailablePermissions(userId)` | Long userId | UserAvailablePermissionsVO | 获取用户可分配权限（未拥有的） |

**UserPermissionDetailVO 结构**：
```java
{
    userId: Long,
    username: String,
    isSuperuser: Boolean,
    roles: [
        {
            roleId: Long,
            roleName: String,
            roleCode: String
        }
    ],
    permissions: [
        {
            permissionId: Long,
            permissionName: String,
            path: String,
            method: String,
            effect: String,         // GRANT / DENY
            priority: Integer,
            source: String,         // ROLE / USER_OVERRIDE / SUPER_USER
            sourceRoleId: Long,     // 来源角色ID（如果是角色权限）
            sourceRoleName: String  // 来源角色名称
        }
    ],
    groupedPermissions: [
        {
            groupKey: String,
            groupName: String,
            children: [ permission details with source ]
        }
    ],
    overrides: [                    // 用户覆盖权限
        {
            overrideId: Long,
            permissionId: Long,
            permissionName: String,
            effect: String
        }
    ]
}
```

**UserEffectivePermissionsVO 结构**：
```java
{
    userId: Long,
    username: String,
    isSuperuser: Boolean,
    effectivePermissions: [
        {
            permissionId: Long,
            permissionName: String,
            path: String,
            method: String,
            effectiveEffect: String,   // GRANT / DENY
            finalDecision: String,     // ALLOWED / DENIED
            sources: [                 // 所有来源（用于前端展示）
                {
                    type: String,     // ROLE / USER_OVERRIDE / SUPER_USER
                    roleId: Long,
                    roleName: String,
                    effect: String,
                    priority: Integer
                }
            ]
        }
    ],
    totalCount: Integer
}
```

**UserAvailablePermissionsVO 结构**：
```java
{
    userId: Long,
    username: String,
    isSuperuser: Boolean,
    unassignedPermissions: [        // 未分配的权限（按分组聚合）
        {
            groupKey: String,
            groupName: String,
            permissions: [
                {
                    id: Long,
                    name: String,
                    path: String,
                    method: String,
                    currentEffect: String,  // null 或 DENY
                    reason: String          // UNASSIGNED / ROLE_DENIED
                }
            ]
        }
    ],
    canBeOverridden: [              // 可以被覆盖的权限
        {
            id: Long,
            name: String,
            reason: String           // 当前被角色拒绝，可通过覆盖允许
        }
    ]
}
```

---

## 三、权限计算详细流程

### 3.1 getUserPermissions(userId) 算法

```java
public Set<AdminPermission> getUserPermissions(Long userId) {
    // 1. 超级管理员检查
    AdminUser user = adminUserMapper.selectById(userId);
    if (user != null && user.getIsSuperuser()) {
        // 返回所有启用的权限
        return new HashSet<>(permissionService.getActivePermissions());
    }

    // 2. 获取用户角色
    Set<Long> roleIds = getUserRoleIds(userId);
    if (roleIds.isEmpty()) {
        return Collections.emptySet();
    }

    // 3. 收集所有角色权限
    Map<Long, PermissionRule> mergedPermissions = new HashMap<>();

    for (Long roleId : roleIds) {
        // 获取角色直接关联的权限
        List<AdminRolePermission> rolePerms =
            adminRolePermissionMapper.selectList(
                new LambdaQueryWrapper<AdminRolePermission>()
                    .eq(AdminRolePermission::getRoleId, roleId)
                    .eq(AdminRolePermission::getIsDeleted, 0)
            );

        for (AdminRolePermission rp : rolePerms) {
            AdminPermission perm = permissionService.getById(rp.getPermissionId());
            if (perm == null || perm.getStatus() != 1) {
                continue;
            }

            PermissionRule rule = new PermissionRule(
                perm,
                rp.getEffect(),
                rp.getPriority(),
                "ROLE:" + roleId
            );

            // 合并：同权限多次出现时，优先级高的生效；同优先级时DENY优先
            mergedPermissions.merge(
                rp.getPermissionId(),
                rule,
                (existing, newRule) -> {
                    if (newRule.priority > existing.priority) {
                        return newRule;
                    } else if (newRule.priority.equals(existing.priority)) {
                        // 同优先级，DENY优先
                        return "DENY".equals(newRule.effect) ? newRule : existing;
                    }
                    return existing;
                }
            );
        }
    }

    // 4. 应用用户权限覆盖
    List<AdminUserPermissionOverride> overrides =
        adminUserPermissionOverrideMapper.selectList(
            new LambdaQueryWrapper<AdminUserPermissionOverride>()
                .eq(AdminUserPermissionOverride::getUserId, userId)
                .eq(AdminUserPermissionOverride::getIsDeleted, 0)
        );

    for (AdminUserPermissionOverride override : overrides) {
        AdminPermission perm = permissionService.getById(override.getPermissionId());
        if (perm == null || perm.getStatus() != 1) {
            continue;
        }

        PermissionRule overrideRule = new PermissionRule(
            perm,
            override.getEffect(),
            Integer.MAX_VALUE,  // 覆盖权限优先级最高
            "USER_OVERRIDE"
        );

        // 用户覆盖直接覆盖角色权限
        mergedPermissions.put(override.getPermissionId(), overrideRule);
    }

    // 5. 转换为权限集合（只返回GRANT的）
    return mergedPermissions.values().stream()
        .filter(rule -> "GRANT".equals(rule.effect))
        .map(rule -> rule.permission)
        .collect(Collectors.toSet());
}
```

### 3.2 checkPermissionDetail(userId, path, method) 算法

```java
public PermissionCheckResultVO checkPermissionDetail(
    Long userId, String path, String method) {

    // 1. 超级管理员直接放行
    AdminUser user = adminUserMapper.selectById(userId);
    if (user != null && user.getIsSuperuser()) {
        return PermissionCheckResultVO.allow("SUPER_USER");
    }

    // 2. 获取所有匹配的权限
    List<AdminPermission> matchedPerms =
        permissionService.findMatchingPermissions(path, method);

    if (matchedPerms.isEmpty()) {
        return PermissionCheckResultVO.deny(
            "无匹配权限规则，默认拒绝",
            null,
            Collections.emptyList()
        );
    }

    // 3. 收集所有相关规则（角色权限 + 用户覆盖）
    List<PermissionSourceDetail> allSources = new ArrayList<>();
    boolean hasOverride = false;

    // 4. 检查用户权限覆盖
    List<AdminUserPermissionOverride> overrides =
        adminUserPermissionOverrideMapper.selectList(
            new LambdaQueryWrapper<AdminUserPermissionOverride>()
                .eq(AdminUserPermissionOverride::getUserId, userId)
                .eq(AdminUserPermissionOverride::getIsDeleted, 0)
        );

    for (AdminUserPermissionOverride override : overrides) {
        for (AdminPermission perm : matchedPerms) {
            if (override.getPermissionId().equals(perm.getId())) {
                allSources.add(PermissionSourceDetail.builder()
                    .type("USER_OVERRIDE")
                    .permissionId(perm.getId())
                    .permissionName(perm.getName())
                    .effect(override.getEffect())
                    .priority(Integer.MAX_VALUE)
                    .build());
                hasOverride = true;
            }
        }
    }

    // 5. 如果没有用户覆盖，检查角色权限
    if (!hasOverride) {
        Set<Long> roleIds = getUserRoleIds(userId);

        for (Long roleId : roleIds) {
            List<AdminRolePermission> rolePerms =
                adminRolePermissionMapper.selectList(
                    new LambdaQueryWrapper<AdminRolePermission>()
                        .eq(AdminRolePermission::getRoleId, roleId)
                        .eq(AdminRolePermission::getIsDeleted, 0)
                );

            for (AdminRolePermission rp : rolePerms) {
                for (AdminPermission perm : matchedPerms) {
                    if (rp.getPermissionId().equals(perm.getId())) {
                        AdminRole role = roleService.getById(roleId);
                        allSources.add(PermissionSourceDetail.builder()
                            .type("ROLE")
                            .roleId(roleId)
                            .roleName(role != null ? role.getName() : "")
                            .permissionId(perm.getId())
                            .permissionName(perm.getName())
                            .effect(rp.getEffect())
                            .priority(rp.getPriority())
                            .isGroup(perm.getIsGroup() == 1)
                            .groupKey(perm.getGroupKey())
                            .build());
                    }
                }
            }
        }
    }

    // 6. 无任何规则，默认拒绝
    if (allSources.isEmpty()) {
        return PermissionCheckResultVO.deny(
            "无权限配置",
            null,
            Collections.emptyList()
        );
    }

    // 7. 按优先级排序，DENY优先
    allSources.sort((a, b) -> {
        if (!a.getPriority().equals(b.getPriority())) {
            return b.getPriority().compareTo(a.getPriority()); // 降序
        }
        // 同优先级，DENY优先
        return "DENY".equals(a.getEffect()) ? -1 : 1;
    });

    // 8. 取最高优先级规则
    PermissionSourceDetail topRule = allSources.get(0);
    boolean allowed = "GRANT".equals(topRule.getEffect());

    return PermissionCheckResultVO.builder()
        .allowed(allowed)
        .matchedPermission(matchedPerms.get(0))
        .source(topRule.getType())
        .sourceDetails(allSources)
        .denyReason(allowed ? null : buildDenyReason(topRule))
        .build();
}
```

---

## 四、数据库迁移

### 4.1 V3__rbac_extension.sql

```sql
-- V3__rbac_extension.sql
-- RBAC系统扩展字段

-- 1. admin_user 表新增超级管理员标识
ALTER TABLE admin_user ADD COLUMN is_superuser INTEGER DEFAULT 0;
COMMENT ON COLUMN admin_user.is_superuser IS '是否超级管理员（1-是 0-否）';
CREATE INDEX idx_admin_user_is_superuser ON admin_user(is_superuser);

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

CREATE INDEX idx_admin_role_permission_effect ON admin_role_permission(effect);
CREATE INDEX idx_admin_role_permission_priority ON admin_role_permission(priority);

-- 4. 为超级管理员角色分配所有权限（初始数据）
-- 需要在Permission数据初始化后再执行
```

### 4.2 V4__init_permissions.sql

```sql
-- V4__init_permissions.sql
-- 初始化权限数据（从OpenAPI同步）

-- 插入组权限示例
INSERT INTO admin_permission (name, code, type, method, path, group_key, group_name, is_group, status, sort) VALUES
('用户管理', 'admin_users_group', 'api', '*', '/api/admin/admin-users/**', 'admin_users', '用户管理', 1, 1, 1),
('角色管理', 'admin_roles_group', 'api', '*', '/api/admin/roles/**', 'admin_roles', '角色管理', 1, 1, 2),
('权限管理', 'admin_permissions_group', 'api', '*', '/api/admin/permissions/**', 'admin_permissions', '权限管理', 1, 1, 3);

-- 插入子权限示例
INSERT INTO admin_permission (name, code, type, method, path, group_key, group_name, is_group, status, sort) VALUES
('用户列表', 'admin_users_list', 'api', 'GET', '/api/admin/admin-users', 'admin_users', '用户管理', 0, 1, 1),
('用户详情', 'admin_users_detail', 'api', 'GET', '/api/admin/admin-users/*', 'admin_users', '用户管理', 0, 1, 2),
('创建用户', 'admin_users_create', 'api', 'POST', '/api/admin/admin-users', 'admin_users', '用户管理', 0, 1, 3),
('更新用户', 'admin_users_update', 'api', 'PUT', '/api/admin/admin-users/*', 'admin_users', '用户管理', 0, 1, 4),
('删除用户', 'admin_users_delete', 'api', 'DELETE', '/api/admin/admin-users/*', 'admin_users', '用户管理', 0, 1, 5),
('角色列表', 'admin_roles_list', 'api', 'GET', '/api/admin/roles', 'admin_roles', '角色管理', 0, 1, 1),
('角色详情', 'admin_roles_detail', 'api', 'GET', '/api/admin/roles/*', 'admin_roles', '角色管理', 0, 1, 2);

-- 将所有权限分配给超级管理员角色
INSERT INTO admin_role_permission (role_id, permission_id, effect, priority)
SELECT 1, id, 'GRANT', 100 FROM admin_permission WHERE is_deleted = 0;
```

---

## 五、Controller 接口设计

### 5.1 PermissionController

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| `list` | GET | `/api/admin/permissions` | 权限列表（分页） |
| `getDetail` | GET | `/api/admin/permissions/{id}` | 权限详情 |
| `create` | POST | `/api/admin/permissions` | 创建权限 |
| `update` | PUT | `/api/admin/permissions/{id}` | 更新权限 |
| `delete` | DELETE | `/api/admin/permissions/{id}` | 删除权限 |
| `getGroups` | GET | `/api/admin/permissions/groups` | 获取权限分组列表（用于角色分配时选择） |

---

### 5.2 RoleController

#### 5.2.1 角色基础管理

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| `list` | GET | `/api/admin/roles` | 角色列表（分页） |
| `getDetail` | GET | `/api/admin/roles/{id}` | 角色详情 |
| `create` | POST | `/api/admin/roles` | 创建角色 |
| `update` | PUT | `/api/admin/roles/{id}` | 更新角色 |
| `delete` | DELETE | `/api/admin/roles/{id}` | 删除角色 |

#### 5.2.2 角色权限管理

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| `getPermissions` | GET | `/api/admin/roles/{id}/permissions` | 获取角色已分配权限（含分组结构） |
| `getAssignablePermissions` | GET | `/api/admin/roles/{id}/permissions/assignable` | 获取可分配权限列表（区分组/子权限） |
| `assignGroupPermissions` | POST | `/api/admin/roles/{id}/permissions/groups` | 分配组权限给角色 |
| `assignChildPermissions` | POST | `/api/admin/roles/{id}/permissions/children` | 分配子权限给角色 |
| `revokePermissions` | DELETE | `/api/admin/roles/{id}/permissions` | 批量撤销角色权限 |

**1. GET /api/admin/roles/{id}/permissions** - 获取角色已分配权限

响应结构 `RolePermissionsVO`：
```json
{
    "roleId": 1,
    "roleName": "系统管理员",
    "roleCode": "SYS_ADMIN",
    "groupedPermissions": [
        {
            "groupKey": "admin_users",
            "groupName": "用户管理",
            "groupPermission": {
                "id": 1,
                "name": "用户管理",
                "code": "admin_users_group",
                "path": "/api/admin/admin-users/**",
                "method": "*",
                "effect": "GRANT",
                "priority": 0,
                "isAssigned": true
            },
            "children": [
                {
                    "id": 2,
                    "name": "用户列表",
                    "code": "admin_users_list",
                    "path": "/api/admin/admin-users",
                    "method": "GET",
                    "effect": "GRANT",
                    "priority": 0,
                    "isAssigned": true
                },
                {
                    "id": 3,
                    "name": "用户详情",
                    "code": "admin_users_detail",
                    "path": "/api/admin/admin-users/*",
                    "method": "GET",
                    "effect": "DENY",
                    "priority": 10,
                    "isAssigned": true,
                    "isOverridden": true
                }
            ],
            "totalCount": 5,
            "assignedCount": 4
        }
    ],
    "summary": {
        "totalPermissions": 20,
        "groupPermissions": 3,
        "childPermissions": 17,
        "grantCount": 18,
        "denyCount": 2
    }
}
```

**2. GET /api/admin/roles/{id}/permissions/assignable** - 获取可分配权限列表

响应结构 `AssignablePermissionsVO`：
```json
{
    "roleId": 1,
    "groups": [
        {
            "groupKey": "admin_users",
            "groupName": "用户管理",
            "groupPermission": {
                "id": 1,
                "name": "用户管理",
                "path": "/api/admin/admin-users/**",
                "method": "*",
                "isAssigned": true,
                "effect": "GRANT"
            },
            "unassignedChildren": [
                {
                    "id": 5,
                    "name": "导出用户",
                    "path": "/api/admin/admin-users/export",
                    "method": "POST",
                    "isAssigned": false
                }
            ]
        },
        {
            "groupKey": "admin_roles",
            "groupName": "角色管理",
            "groupPermission": {
                "id": 10,
                "name": "角色管理",
                "path": "/api/admin/roles/**",
                "method": "*",
                "isAssigned": false
            },
            "unassignedChildren": [
                {
                    "id": 11,
                    "name": "角色列表",
                    "path": "/api/admin/roles",
                    "method": "GET",
                    "isAssigned": false
                },
                {
                    "id": 12,
                    "name": "创建角色",
                    "path": "/api/admin/roles",
                    "method": "POST",
                    "isAssigned": false
                }
            ]
        }
    ]
}
```

**3. POST /api/admin/roles/{id}/permissions/groups** - 分配组权限

请求体：
```json
{
    "permissions": [
        {
            "permissionId": 1,
            "effect": "GRANT",
            "priority": 0
        },
        {
            "permissionId": 10,
            "effect": "DENY",
            "priority": 5
        }
    ]
}
```

**4. POST /api/admin/roles/{id}/permissions/children** - 分配子权限

请求体：
```json
{
    "permissions": [
        {
            "permissionId": 2,
            "effect": "GRANT",
            "priority": 0
        },
        {
            "permissionId": 5,
            "effect": "DENY",
            "priority": 10
        }
    ]
}
```

**5. DELETE /api/admin/roles/{id}/permissions** - 批量撤销角色权限

请求体：
```json
{
    "permissionIds": [2, 3, 5]
}
```

---

### 5.3 AdminUserController

#### 5.3.1 用户角色管理

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| `getRoles` | GET | `/api/admin/admin-users/{id}/roles` | 获取用户的角色列表 |
| `assignRoles` | POST | `/api/admin/admin-users/{id}/roles` | 分配角色给用户 |
| `revokeRoles` | DELETE | `/api/admin/admin-users/{id}/roles` | 撤销用户角色 |

#### 5.3.2 用户权限管理

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| `getPermissions` | GET | `/api/admin/admin-users/{id}/permissions` | 获取用户已有效权限（含来源） |
| `getEffectivePermissions` | GET | `/api/admin/admin-users/{id}/permissions/effective` | 获取用户有效权限（已计算最终效果） |
| `getAvailablePermissions` | GET | `/api/admin/admin-users/{id}/permissions/available` | 获取用户可分配权限（未拥有的权限） |
| `getPermissionOverrides` | GET | `/api/admin/admin-users/{id}/permission-overrides` | 获取用户权限覆盖列表 |
| `setPermissionOverride` | POST | `/api/admin/admin-users/{id}/permission-overrides` | 设置用户权限覆盖 |
| `removePermissionOverride` | DELETE | `/api/admin/admin-users/{id}/permission-overrides/{overrideId}` | 删除权限覆盖 |
| `clearPermissionOverrides` | DELETE | `/api/admin/admin-users/{id}/permission-overrides` | 清除用户所有权限覆盖 |

**1. GET /api/admin/admin-users/{id}/permissions** - 获取用户已有效权限

响应结构 `UserPermissionsVO`：
```json
{
    "userId": 1,
    "username": "admin",
    "isSuperuser": true,
    "roles": [
        {
            "roleId": 1,
            "roleName": "超级管理员",
            "roleCode": "SUPER_ADMIN"
        }
    ],
    "effectivePermissions": [
        {
            "permissionId": 1,
            "permissionName": "用户管理",
            "path": "/api/admin/admin-users/**",
            "method": "*",
            "effect": "GRANT",
            "source": "SUPER_USER",
            "sourceRoleId": null,
            "sourceRoleName": null
        }
    ],
    "groupedPermissions": [
        {
            "groupKey": "admin_users",
            "groupName": "用户管理",
            "children": [
                {
                    "permissionId": 2,
                    "permissionName": "用户列表",
                    "path": "/api/admin/admin-users",
                    "method": "GET",
                    "effect": "GRANT",
                    "source": "ROLE",
                    "sourceRoleId": 1,
                    "sourceRoleName": "系统管理员"
                },
                {
                    "permissionId": 3,
                    "permissionName": "删除用户",
                    "path": "/api/admin/admin-users/*",
                    "method": "DELETE",
                    "effect": "DENY",
                    "source": "ROLE",
                    "sourceRoleId": 1,
                    "sourceRoleName": "系统管理员"
                }
            ]
        }
    ]
}
```

**2. GET /api/admin/admin-users/{id}/permissions/effective** - 获取用户最终有效权限

响应结构 `UserEffectivePermissionsVO`：
```json
{
    "userId": 1,
    "username": "admin",
    "isSuperuser": true,
    "effectivePermissions": [
        {
            "permissionId": 1,
            "permissionName": "用户管理",
            "path": "/api/admin/admin-users/**",
            "method": "*",
            "effectiveEffect": "GRANT",
            "finalDecision": "ALLOWED"
        },
        {
            "permissionId": 2,
            "permissionName": "用户列表",
            "path": "/api/admin/admin-users",
            "method": "GET",
            "effectiveEffect": "GRANT",
            "finalDecision": "ALLOWED"
        }
    ],
    "totalCount": 20
}
```

**3. GET /api/admin/admin-users/{id}/permissions/available** - 获取用户未拥有的权限（用于添加覆盖）

响应结构 `UserAvailablePermissionsVO`：
```json
{
    "userId": 1,
    "username": "admin",
    "isSuperuser": false,
    "unassignedPermissions": [
        {
            "groupKey": "admin_system",
            "groupName": "系统管理",
            "permissions": [
                {
                    "id": 50,
                    "name": "系统配置",
                    "path": "/api/admin/system/config",
                    "method": "*",
                    "currentEffect": null,
                    "reason": "UNASSIGNED"
                },
                {
                    "id": 51,
                    "name": "系统日志",
                    "path": "/api/admin/system/logs",
                    "method": "*",
                    "currentEffect": "DENY",
                    "reason": "ROLE_DENIED"
                }
            ]
        }
    ],
    "canBeOverridden": [
        {
            "id": 51,
            "name": "系统日志",
            "reason": "当前被角色拒绝，可通过覆盖允许"
        }
    ]
}
```

**4. GET /api/admin/admin-users/{id}/permission-overrides** - 获取用户权限覆盖

响应结构：
```json
{
    "userId": 1,
    "username": "admin",
    "overrides": [
        {
            "overrideId": 1,
            "permissionId": 10,
            "permissionName": "删除用户",
            "path": "/api/admin/admin-users/*",
            "method": "DELETE",
            "effect": "DENY",
            "createTime": "2026-03-20T10:00:00"
        },
        {
            "overrideId": 2,
            "permissionId": 20,
            "permissionName": "系统配置",
            "path": "/api/admin/system/config",
            "method": "*",
            "effect": "GRANT",
            "createTime": "2026-03-20T11:00:00"
        }
    ]
}
```

**5. POST /api/admin/admin-users/{id}/permission-overrides** - 设置用户权限覆盖

请求体：
```json
{
    "permissionId": 50,
    "effect": "GRANT"
}
```

响应：
```json
{
    "overrideId": 3,
    "userId": 1,
    "permissionId": 50,
    "effect": "GRANT",
    "createTime": "2026-03-20T12:00:00"
}
```

---

### 5.4 通用VO结构定义

#### 5.4.1 权限基础信息 VO

```java
@Schema(description = "权限基础信息")
@Data
public class PermissionBaseVO {
    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "权限编码")
    private String code;

    @Schema(description = "接口路径")
    private String path;

    @Schema(description = "HTTP方法")
    private String method;

    @Schema(description = "是否组权限")
    private Boolean isGroup;

    @Schema(description = "分组标识")
    private String groupKey;

    @Schema(description = "分组名称")
    private String groupName;
}
```

#### 5.4.2 角色基础信息 VO

```java
@Schema(description = "角色基础信息")
@Data
public class RoleBaseVO {
    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态")
    private Integer status;
}
```

#### 5.4.3 用户基础信息 VO

```java
@Schema(description = "用户基础信息")
@Data
public class UserBaseVO {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "是否超级管理员")
    private Boolean isSuperuser;
}
```

---

## 六、实现优先级

### Phase 1: 基础设施（核心）
1. 数据库迁移脚本 V3、V4
2. 实体类字段补充（AdminUser、AdminPermission、AdminRolePermission）
3. Mapper 层完善

### Phase 2: Service 实现
1. PermissionService - 权限基础服务 + 通配符匹配
2. RoleService - 角色管理 + 角色权限关联
3. RBACService - 核心RBAC逻辑 + 权限检查

### Phase 3: Controller 实现
1. PermissionController
2. RoleController
3. AdminUserController (权限部分)

### Phase 4: 安全拦截器
1. 权限拦截器配置
2. 动态权限验证

---

## 七、关键技术点

### 7.1 通配符匹配算法

```java
public boolean matchPattern(String pattern, String path) {
    // 支持 ** (匹配多级路径), * (匹配单级路径), ? (匹配单个字符)

    if (pattern.contains("**")) {
        // 处理 ** 通配符
        String regex = pattern
            .replace("**", "<<<DS>>>")
            .replace("*", "[^/]*")
            .replace("?", "[^/]")
            .replace("<<<DS>>>", ".*");
        return Pattern.matches("^" + regex + "$", path);
    }

    if (pattern.contains("*") || pattern.contains("?")) {
        // 处理单级通配符
        String regex = pattern
            .replace("*", "[^/]*")
            .replace("?", "[^/]");
        return Pattern.matches("^" + regex + "$", path);
    }

    // 精确匹配
    return pattern.equals(path);
}
```

### 7.2 权限合并规则

```
多角色权限合并优先级：
1. 优先级高的规则覆盖优先级低的规则
2. 同优先级时，DENY 优先于 GRANT
3. 用户权限覆盖（USER_OVERRIDE）优先级最高（Integer.MAX_VALUE）
```

---

## 八、测试验证

### 8.1 单元测试要点

1. **PermissionService 通配符匹配**
   - `**` 匹配多级路径
   - `*` 匹配单级路径
   - `?` 匹配单个字符
   - 精确匹配

2. **RBACService 权限计算**
   - 超级管理员拥有所有权限
   - 无角色用户无权限
   - 多角色权限合并
   - 用户权限覆盖生效

3. **权限检查**
   - 正确识别权限来源
   - 优先级排序正确
   - 拒绝原因描述准确

### 8.2 集成测试场景

1. 创建角色 -> 分配权限 -> 分配给用户 -> 验证权限
2. 用户权限覆盖 -> 验证覆盖生效
3. 移除角色权限 -> 验证权限回收

---

## 九、接口总览（前端参考）

### 9.1 权限管理接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| `权限列表` | GET | `/api/admin/permissions` | 分页获取权限列表 |
| `权限详情` | GET | `/api/admin/permissions/{id}` | 获取单个权限详情 |
| `创建权限` | POST | `/api/admin/permissions` | 创建新权限 |
| `更新权限` | PUT | `/api/admin/permissions/{id}` | 更新权限信息 |
| `删除权限` | DELETE | `/api/admin/permissions/{id}` | 删除权限 |
| `权限分组` | GET | `/api/admin/permissions/groups` | 获取所有权限分组（用于角色分配选择） |

### 9.2 角色管理接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| `角色列表` | GET | `/api/admin/roles` | 分页获取角色列表 |
| `角色详情` | GET | `/api/admin/roles/{id}` | 获取角色详情 |
| `创建角色` | POST | `/api/admin/roles` | 创建角色 |
| `更新角色` | PUT | `/api/admin/roles/{id}` | 更新角色信息 |
| `删除角色` | DELETE | `/api/admin/roles/{id}` | 删除角色 |

### 9.3 角色权限分配接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| `角色权限详情` | GET | `/api/admin/roles/{id}/permissions` | 获取角色已分配权限（含分组结构） |
| `可分配权限` | GET | `/api/admin/roles/{id}/permissions/assignable` | 获取可分配的权限（已分配/未分配状态） |
| `分配组权限` | POST | `/api/admin/roles/{id}/permissions/groups` | 批量分配组权限 |
| `分配子权限` | POST | `/api/admin/roles/{id}/permissions/children` | 批量分配子权限 |
| `撤销权限` | DELETE | `/api/admin/roles/{id}/permissions` | 批量撤销角色权限 |

### 9.4 用户权限管理接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| `用户角色列表` | GET | `/api/admin/admin-users/{id}/roles` | 获取用户的角色 |
| `分配用户角色` | POST | `/api/admin/admin-users/{id}/roles` | 分配角色给用户 |
| `撤销用户角色` | DELETE | `/api/admin/admin-users/{id}/roles` | 撤销用户角色 |
| `用户有效权限` | GET | `/api/admin/admin-users/{id}/permissions` | 获取用户已有效权限（含来源） |
| `用户最终权限` | GET | `/api/admin/admin-users/{id}/permissions/effective` | 获取用户最终有效权限（已计算最终决策） |
| `用户可用权限` | GET | `/api/admin/admin-users/{id}/permissions/available` | 获取用户未拥有的权限（用于添加覆盖） |
| `权限覆盖列表` | GET | `/api/admin/admin-users/{id}/permission-overrides` | 获取用户权限覆盖列表 |
| `设置权限覆盖` | POST | `/api/admin/admin-users/{id}/permission-overrides` | 设置用户权限覆盖 |
| `删除权限覆盖` | DELETE | `/api/admin/admin-users/{id}/permission-overrides/{overrideId}` | 删除单个权限覆盖 |
| `清除所有覆盖` | DELETE | `/api/admin/admin-users/{id}/permission-overrides` | 清除用户所有权限覆盖 |

---

## 十、前端场景分析

### 10.1 用户权限管理页面

**场景**：管理员查看/编辑用户权限

**需要的数据流**：
1. 调用 `GET /api/admin/admin-users/{id}/permissions` 获取用户当前有效权限
   - 返回 `effectivePermissions` 列表，每个权限带有 `source` 来源
   - 返回 `groupedPermissions` 按分组展示
2. 调用 `GET /api/admin/admin-users/{id}/permissions/available` 获取可添加的权限
   - 返回 `unassignedPermissions` 用户未拥有的权限
   - 返回 `canBeOverridden` 可以被覆盖的权限（当前被拒绝的）
3. 前端选择权限后调用 `POST /api/admin/admin-users/{id}/permission-overrides` 添加覆盖

**前端组件设计建议**：
```
用户权限管理
├── 基本信息（用户名、是否超级管理员）
├── 角色信息（当前分配的角色列表）
├── 有效权限 Tab
│   └── 分组展示权限，每个权限显示来源标签（角色/覆盖）
└── 可添加权限 Tab
    ├── 未分配权限列表
    └── 可覆盖权限列表（带DENY标记）
```

### 10.2 角色权限配置页面

**场景**：管理员配置角色权限

**需要的数据流**：
1. 调用 `GET /api/admin/roles/{id}/permissions` 获取角色当前权限
   - 返回 `groupedPermissions` 按分组展示
   - 每个子权限显示 `effect`（GRANT/DENY）和 `priority`
   - 显示 `summary` 统计信息
2. 调用 `GET /api/admin/roles/{id}/permissions/assignable` 获取可分配权限
   - `groups[].groupPermission` 显示组权限分配状态
   - `groups[].unassignedChildren` 显示未分配的子权限
3. 前端选择权限后：
   - 分配组权限：`POST /api/admin/roles/{id}/permissions/groups`
   - 分配子权限：`POST /api/admin/roles/{id}/permissions/children`
   - 撤销权限：`DELETE /api/admin/roles/{id}/permissions`

**前端组件设计建议**：
```
角色权限配置
├── 角色信息（名称、编码、描述）
├── 权限统计卡片（总数、组权限数、子权限数、允许数、拒绝数）
├── 已分配权限 Tab
│   └── 分组展示，每个权限可编辑 effect/priority，可撤销
└── 可分配权限 Tab
    ├── 组权限列表（可勾选分配）
    └── 子权限列表（可勾选分配）
```

### 10.3 权限覆盖操作示例

**场景**：用户A被角色拒绝访问接口X，需要临时允许

**步骤**：
1. 调用 `GET /api/admin/admin-users/{id}/permissions/available`
2. 在 `canBeOverridden` 列表中找到接口X（reason: "当前被角色拒绝，可通过覆盖允许"）
3. 调用 `POST /api/admin/admin-users/{id}/permission-overrides` 设置 `effect: GRANT`
4. 验证：`GET /api/admin/admin-users/{id}/permissions/effective` 接口X变为 ALLOWED
