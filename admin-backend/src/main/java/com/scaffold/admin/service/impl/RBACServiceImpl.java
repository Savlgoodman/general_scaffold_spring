package com.scaffold.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.scaffold.admin.mapper.*;
import com.scaffold.admin.model.dto.UserPermissionOverrideDTO;
import com.scaffold.admin.model.entity.*;
import com.scaffold.admin.model.vo.*;
import com.scaffold.admin.service.PermissionService;
import com.scaffold.admin.service.RBACService;
import com.scaffold.admin.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RBACServiceImpl implements RBACService {

    private final AdminUserMapper userMapper;
    private final AdminUserRoleMapper userRoleMapper;
    private final AdminRoleMapper roleMapper;
    private final AdminRolePermissionMapper rolePermissionMapper;
    private final AdminPermissionMapper permissionMapper;
    private final AdminUserPermissionOverrideMapper overrideMapper;
    private final PermissionService permissionService;
    private final RoleService roleService;

    // ==================== 用户-角色关联 ====================

    @Override
    public List<AdminRole> getUserRoles(Long userId) {
        List<AdminUserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<AdminUserRole>()
                .eq(AdminUserRole::getUserId, userId)
                .eq(AdminUserRole::getIsDeleted, 0)
        );

        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = userRoles.stream()
            .map(AdminUserRole::getRoleId)
            .collect(Collectors.toList());

        return roleMapper.selectList(
            new LambdaQueryWrapper<AdminRole>()
                .in(AdminRole::getId, roleIds)
                .eq(AdminRole::getIsDeleted, 0)
        );
    }

    @Override
    public Set<Long> getUserRoleIds(Long userId) {
        List<AdminUserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<AdminUserRole>()
                .eq(AdminUserRole::getUserId, userId)
                .eq(AdminUserRole::getIsDeleted, 0)
        );

        return userRoles.stream()
            .map(AdminUserRole::getRoleId)
            .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getUserRoleCodes(Long userId) {
        List<AdminRole> roles = getUserRoles(userId);
        return roles.stream()
            .map(AdminRole::getCode)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean hasRole(Long userId, String roleCode) {
        return getUserRoleCodes(userId).contains(roleCode);
    }

    @Override
    @Transactional
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            // 检查是否已存在
            AdminUserRole existing = userRoleMapper.selectOne(
                new LambdaQueryWrapper<AdminUserRole>()
                    .eq(AdminUserRole::getUserId, userId)
                    .eq(AdminUserRole::getRoleId, roleId)
                    .eq(AdminUserRole::getIsDeleted, 0)
            );

            if (existing == null) {
                AdminUserRole newUserRole = new AdminUserRole();
                newUserRole.setUserId(userId);
                newUserRole.setRoleId(roleId);
                userRoleMapper.insert(newUserRole);
            }
        }
    }

    @Override
    @Transactional
    public void removeUserRoles(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            AdminUserRole existing = userRoleMapper.selectOne(
                new LambdaQueryWrapper<AdminUserRole>()
                    .eq(AdminUserRole::getUserId, userId)
                    .eq(AdminUserRole::getRoleId, roleId)
                    .eq(AdminUserRole::getIsDeleted, 0)
            );

            if (existing != null) {
                existing.setIsDeleted(1);
                userRoleMapper.updateById(existing);
            }
        }
    }

    @Override
    @Transactional
    public void syncUserRoles(Long userId, List<Long> roleIds) {
        // 先删除所有现有角色关联
        List<AdminUserRole> existing = userRoleMapper.selectList(
            new LambdaQueryWrapper<AdminUserRole>()
                .eq(AdminUserRole::getUserId, userId)
                .eq(AdminUserRole::getIsDeleted, 0)
        );

        for (AdminUserRole ur : existing) {
            ur.setIsDeleted(1);
            userRoleMapper.updateById(ur);
        }

        // 再添加新的角色关联
        if (roleIds != null && !roleIds.isEmpty()) {
            assignUserRoles(userId, roleIds);
        }
    }

    // ==================== 用户权限覆盖 ====================

    @Override
    @Transactional
    public void setUserPermissionOverride(Long userId, UserPermissionOverrideDTO dto) {
        // 检查是否已存在覆盖
        AdminUserPermissionOverride existing = overrideMapper.selectOne(
            new LambdaQueryWrapper<AdminUserPermissionOverride>()
                .eq(AdminUserPermissionOverride::getUserId, userId)
                .eq(AdminUserPermissionOverride::getPermissionId, dto.getPermissionId())
                .eq(AdminUserPermissionOverride::getIsDeleted, 0)
        );

        if (existing != null) {
            // 更新现有覆盖
            existing.setEffect(dto.getEffect());
            overrideMapper.updateById(existing);
        } else {
            // 新增覆盖
            AdminUserPermissionOverride newOverride = new AdminUserPermissionOverride();
            newOverride.setUserId(userId);
            newOverride.setPermissionId(dto.getPermissionId());
            newOverride.setEffect(dto.getEffect());
            overrideMapper.insert(newOverride);
        }
    }

    @Override
    @Transactional
    public void removeUserPermissionOverride(Long userId, Long overrideId) {
        AdminUserPermissionOverride existing = overrideMapper.selectOne(
            new LambdaQueryWrapper<AdminUserPermissionOverride>()
                .eq(AdminUserPermissionOverride::getId, overrideId)
                .eq(AdminUserPermissionOverride::getUserId, userId)
                .eq(AdminUserPermissionOverride::getIsDeleted, 0)
        );

        if (existing != null) {
            existing.setIsDeleted(1);
            overrideMapper.updateById(existing);
        }
    }

    @Override
    @Transactional
    public void clearUserPermissionOverrides(Long userId) {
        overrideMapper.update(
            new AdminUserPermissionOverride(),
            new LambdaUpdateWrapper<AdminUserPermissionOverride>()
                .eq(AdminUserPermissionOverride::getUserId, userId)
                .eq(AdminUserPermissionOverride::getIsDeleted, 0)
                .set(AdminUserPermissionOverride::getIsDeleted, 1)
        );
    }

    @Override
    public List<PermissionOverrideVO> getUserPermissionOverrides(Long userId) {
        AdminUser user = userMapper.selectById(userId);
        if (user == null) {
            return Collections.emptyList();
        }

        List<AdminUserPermissionOverride> overrides = overrideMapper.selectList(
            new LambdaQueryWrapper<AdminUserPermissionOverride>()
                .eq(AdminUserPermissionOverride::getUserId, userId)
                .eq(AdminUserPermissionOverride::getIsDeleted, 0)
        );

        List<Long> permIds = overrides.stream()
            .map(AdminUserPermissionOverride::getPermissionId)
            .collect(Collectors.toList());

        final Map<Long, AdminPermission> permMap = new HashMap<>();
        if (!permIds.isEmpty()) {
            List<AdminPermission> permissions = permissionMapper.selectBatchIds(permIds);
            permMap.putAll(permissions.stream()
                .collect(Collectors.toMap(AdminPermission::getId, p -> p)));
        }

        return overrides.stream().map(override -> {
            PermissionOverrideVO vo = new PermissionOverrideVO();
            vo.setOverrideId(override.getId());
            vo.setUserId(override.getUserId());
            vo.setPermissionId(override.getPermissionId());
            vo.setEffect(override.getEffect());
            vo.setCreateTime(override.getCreateTime());

            AdminPermission perm = permMap.get(override.getPermissionId());
            if (perm != null) {
                vo.setPermissionName(perm.getName());
                vo.setPath(perm.getPath());
                vo.setMethod(perm.getMethod());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 用户权限计算 ====================

    @Override
    public Set<Long> getUserPermissionIds(Long userId) {
        AdminUser user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return Collections.emptySet();
        }

        // 超级管理员拥有所有权限
        if (user.getIsSuperuser() != null && user.getIsSuperuser() == 1) {
            return permissionMapper.selectList(
                new LambdaQueryWrapper<AdminPermission>()
                    .eq(AdminPermission::getIsDeleted, 0)
                    .eq(AdminPermission::getStatus, 1)
            ).stream()
                .map(AdminPermission::getId)
                .collect(Collectors.toSet());
        }

        // 获取用户角色
        Set<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        // 收集所有角色权限（取GRANT的）
        Set<Long> permissionIds = new HashSet<>();

        for (Long roleId : roleIds) {
            List<AdminRolePermission> rolePerms = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<AdminRolePermission>()
                    .eq(AdminRolePermission::getRoleId, roleId)
                    .eq(AdminRolePermission::getIsDeleted, 0)
                    .eq(AdminRolePermission::getEffect, "GRANT")
            );

            rolePerms.forEach(rp -> permissionIds.add(rp.getPermissionId()));
        }

        // 应用用户权限覆盖
        List<AdminUserPermissionOverride> overrides = overrideMapper.selectList(
            new LambdaQueryWrapper<AdminUserPermissionOverride>()
                .eq(AdminUserPermissionOverride::getUserId, userId)
                .eq(AdminUserPermissionOverride::getIsDeleted, 0)
        );

        for (AdminUserPermissionOverride override : overrides) {
            if ("GRANT".equals(override.getEffect())) {
                permissionIds.add(override.getPermissionId());
            } else {
                permissionIds.remove(override.getPermissionId());
            }
        }

        return permissionIds;
    }

    @Override
    public UserPermissionVO getUserPermissionsDetail(Long userId) {
        AdminUser user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        UserPermissionVO vo = new UserPermissionVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setIsSuperuser(user.getIsSuperuser() != null && user.getIsSuperuser() == 1);

        // 获取用户角色
        List<AdminRole> roles = getUserRoles(userId);
        vo.setRoles(roles.stream().map(role -> {
            RoleBaseVO roleVO = new RoleBaseVO();
            roleVO.setId(role.getId());
            roleVO.setName(role.getName());
            roleVO.setCode(role.getCode());
            return roleVO;
        }).collect(Collectors.toList()));

        // 超级管理员拥有所有权限
        if (vo.getIsSuperuser()) {
            List<AdminPermission> allPerms = permissionService.getActivePermissions();
            vo.setEffectivePermissions(allPerms.stream().map(perm -> {
                UserPermissionItemVO item = new UserPermissionItemVO();
                item.setPermissionId(perm.getId());
                item.setName(perm.getName());
                item.setCode(perm.getCode());
                item.setPath(perm.getPath());
                item.setMethod(perm.getMethod());
                item.setIsGroup(perm.getIsGroup() != null && perm.getIsGroup() == 1);
                item.setGroupKey(perm.getGroupKey());
                item.setEffect("GRANT");
                item.setPriority(Integer.MAX_VALUE);
                item.setSource("SUPER_USER");
                return item;
            }).collect(Collectors.toList()));
            vo.setOverrides(Collections.emptyList());
            return vo;
        }

        // 获取用户权限覆盖
        List<PermissionOverrideVO> overrides = getUserPermissionOverrides(userId);
        vo.setOverrides(overrides);

        Map<Long, AdminUserPermissionOverride> overrideMap = overrides.stream()
            .collect(Collectors.toMap(
                PermissionOverrideVO::getPermissionId,
                o -> {
                    AdminUserPermissionOverride rp = new AdminUserPermissionOverride();
                    rp.setId(o.getOverrideId());
                    rp.setPermissionId(o.getPermissionId());
                    rp.setEffect(o.getEffect());
                    return rp;
                },
                (a, b) -> a
            ));

        // 获取角色权限
        Set<Long> roleIds = getUserRoleIds(userId);
        List<UserPermissionItemVO> allPermissions = new ArrayList<>();
        Map<String, List<UserPermissionItemVO>> groupedMap = new HashMap<>();

        for (Long roleId : roleIds) {
            AdminRole role = roleMapper.selectById(roleId);

            List<AdminRolePermission> rolePerms = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<AdminRolePermission>()
                    .eq(AdminRolePermission::getRoleId, roleId)
                    .eq(AdminRolePermission::getIsDeleted, 0)
            );

            for (AdminRolePermission rp : rolePerms) {
                AdminPermission perm = permissionMapper.selectById(rp.getPermissionId());
                if (perm == null || perm.getStatus() == null || perm.getStatus() != 1) {
                    continue;
                }

                // 检查是否有覆盖
                AdminUserPermissionOverride override = overrideMap.get(perm.getId());
                String effect = rp.getEffect();
                String source = "ROLE";
                Long sourceRoleId = roleId;
                String sourceRoleName = role != null ? role.getName() : "";

                if (override != null) {
                    effect = override.getEffect();
                    source = "USER_OVERRIDE";
                    sourceRoleId = null;
                    sourceRoleName = "";
                }

                UserPermissionItemVO item = new UserPermissionItemVO();
                item.setPermissionId(perm.getId());
                item.setName(perm.getName());
                item.setCode(perm.getCode());
                item.setPath(perm.getPath());
                item.setMethod(perm.getMethod());
                item.setIsGroup(perm.getIsGroup() != null && perm.getIsGroup() == 1);
                item.setGroupKey(perm.getGroupKey());
                item.setEffect(effect);
                item.setPriority(rp.getPriority());
                item.setSource(source);
                item.setSourceRoleId(sourceRoleId);
                item.setSourceRoleName(sourceRoleName);

                allPermissions.add(item);

                // 分组
                if (perm.getGroupKey() != null) {
                    groupedMap.computeIfAbsent(perm.getGroupKey(), k -> new ArrayList<>()).add(item);
                }
            }
        }

        vo.setEffectivePermissions(allPermissions);

        // 构建分组权限
        List<UserGroupPermissionVO> groupedPermissions = new ArrayList<>();
        List<PermissionGroupVO> allGroups = permissionService.getAllGroupedPermissions();

        for (PermissionGroupVO group : allGroups) {
            List<UserPermissionItemVO> children = groupedMap.get(group.getGroupKey());
            if (children != null && !children.isEmpty()) {
                UserGroupPermissionVO groupVO = new UserGroupPermissionVO();
                groupVO.setGroupKey(group.getGroupKey());
                groupVO.setGroupName(group.getGroupName());
                groupVO.setChildren(children);
                groupedPermissions.add(groupVO);
            }
        }

        vo.setGroupedPermissions(groupedPermissions);

        return vo;
    }

    @Override
    public UserAvailablePermissionVO getUserAvailablePermissions(Long userId) {
        AdminUser user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        UserAvailablePermissionVO vo = new UserAvailablePermissionVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setIsSuperuser(user.getIsSuperuser() != null && user.getIsSuperuser() == 1);

        // 超级管理员没有可添加的权限
        if (vo.getIsSuperuser()) {
            vo.setUnassignedPermissions(Collections.emptyList());
            vo.setCanBeOverridden(Collections.emptyList());
            return vo;
        }

        // 获取用户已拥有的权限
        Set<Long> userPermIds = getUserPermissionIds(userId);

        // 获取用户被拒绝的权限（来自角色但效果为DENY）
        Set<Long> deniedPermIds = new HashSet<>();
        Set<Long> roleIds = getUserRoleIds(userId);
        for (Long roleId : roleIds) {
            List<AdminRolePermission> rolePerms = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<AdminRolePermission>()
                    .eq(AdminRolePermission::getRoleId, roleId)
                    .eq(AdminRolePermission::getIsDeleted, 0)
                    .eq(AdminRolePermission::getEffect, "DENY")
            );
            rolePerms.forEach(rp -> deniedPermIds.add(rp.getPermissionId()));
        }

        // 获取所有未拥有的权限（按分组聚合）
        List<AdminPermission> allPerms = permissionService.getActivePermissions();
        List<AdminPermission> unassignedPerms = allPerms.stream()
            .filter(p -> !userPermIds.contains(p.getId()))
            .collect(Collectors.toList());

        // 按 groupKey 分组
        Map<String, List<AdminPermission>> groupMap = new HashMap<>();
        for (AdminPermission perm : unassignedPerms) {
            if (perm.getGroupKey() != null) {
                groupMap.computeIfAbsent(perm.getGroupKey(), k -> new ArrayList<>()).add(perm);
            }
        }

        List<UserAvailablePermissionVO.UnassignedGroupVO> unassignedGroups = new ArrayList<>();
        List<UserAvailablePermissionVO.CanOverrideVO> canOverride = new ArrayList<>();

        for (Map.Entry<String, List<AdminPermission>> entry : groupMap.entrySet()) {
            String groupKey = entry.getKey();
            List<AdminPermission> perms = entry.getValue();

            // 获取分组名称
            AdminPermission groupPerm = permissionMapper.selectOne(
                new LambdaQueryWrapper<AdminPermission>()
                    .eq(AdminPermission::getGroupKey, groupKey)
                    .eq(AdminPermission::getIsGroup, 1)
                    .eq(AdminPermission::getIsDeleted, 0)
            );

            UserAvailablePermissionVO.UnassignedGroupVO groupVO =
                new UserAvailablePermissionVO.UnassignedGroupVO();
            groupVO.setGroupKey(groupKey);
            groupVO.setGroupName(groupPerm != null ? groupPerm.getGroupName() : groupKey);

            List<UserAvailablePermissionVO.UnassignedPermissionVO> permVOList = new ArrayList<>();

            for (AdminPermission perm : perms) {
                UserAvailablePermissionVO.UnassignedPermissionVO permVO =
                    new UserAvailablePermissionVO.UnassignedPermissionVO();
                permVO.setId(perm.getId());
                permVO.setName(perm.getName());
                permVO.setPath(perm.getPath());
                permVO.setMethod(perm.getMethod());

                if (deniedPermIds.contains(perm.getId())) {
                    permVO.setCurrentEffect("DENY");
                    permVO.setReason("ROLE_DENIED");

                    UserAvailablePermissionVO.CanOverrideVO overrideVO =
                        new UserAvailablePermissionVO.CanOverrideVO();
                    overrideVO.setId(perm.getId());
                    overrideVO.setName(perm.getName());
                    overrideVO.setReason("当前被角色拒绝，可通过覆盖允许");
                    canOverride.add(overrideVO);
                } else {
                    permVO.setCurrentEffect(null);
                    permVO.setReason("UNASSIGNED");
                }

                permVOList.add(permVO);
            }

            groupVO.setPermissions(permVOList);
            unassignedGroups.add(groupVO);
        }

        vo.setUnassignedPermissions(unassignedGroups);
        vo.setCanBeOverridden(canOverride);

        return vo;
    }

    @Override
    public UserEffectivePermissionVO getUserEffectivePermissions(Long userId) {
        AdminUser user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        UserEffectivePermissionVO vo = new UserEffectivePermissionVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setIsSuperuser(user.getIsSuperuser() != null && user.getIsSuperuser() == 1);

        // 超级管理员拥有所有权限
        if (vo.getIsSuperuser()) {
            List<AdminPermission> allPerms = permissionService.getActivePermissions();
            vo.setEffectivePermissions(allPerms.stream().map(perm -> {
                UserEffectivePermissionVO.EffectivePermissionItemVO item =
                    new UserEffectivePermissionVO.EffectivePermissionItemVO();
                item.setPermissionId(perm.getId());
                item.setName(perm.getName());
                item.setPath(perm.getPath());
                item.setMethod(perm.getMethod());
                item.setEffectiveEffect("GRANT");
                item.setFinalDecision("ALLOWED");

                UserEffectivePermissionVO.PermissionSourceVO source =
                    new UserEffectivePermissionVO.PermissionSourceVO();
                source.setType("SUPER_USER");
                source.setEffect("GRANT");
                source.setPriority(Integer.MAX_VALUE);
                item.setSources(Collections.singletonList(source));

                return item;
            }).collect(Collectors.toList()));
            vo.setTotalCount(vo.getEffectivePermissions().size());
            return vo;
        }

        // 获取用户权限详情
        UserPermissionVO detail = getUserPermissionsDetail(userId);

        // 计算最终权限
        List<UserEffectivePermissionVO.EffectivePermissionItemVO> effectivePermissions = new ArrayList<>();

        // 按 permissionId 分组合并来源
        Map<Long, List<UserPermissionItemVO>> permSourcesMap = new HashMap<>();
        for (UserPermissionItemVO item : detail.getEffectivePermissions()) {
            permSourcesMap.computeIfAbsent(item.getPermissionId(), k -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<Long, List<UserPermissionItemVO>> entry : permSourcesMap.entrySet()) {
            List<UserPermissionItemVO> sources = entry.getValue();

            // 按优先级排序，取最高优先级
            sources.sort((a, b) -> {
                if (!a.getPriority().equals(b.getPriority())) {
                    return b.getPriority().compareTo(a.getPriority());
                }
                return "DENY".equals(a.getEffect()) ? -1 : 1;
            });

            UserPermissionItemVO topSource = sources.get(0);
            boolean allowed = "GRANT".equals(topSource.getEffect());

            UserEffectivePermissionVO.EffectivePermissionItemVO item =
                new UserEffectivePermissionVO.EffectivePermissionItemVO();
            item.setPermissionId(topSource.getPermissionId());
            item.setName(topSource.getName());
            item.setPath(topSource.getPath());
            item.setMethod(topSource.getMethod());
            item.setEffectiveEffect(topSource.getEffect());
            item.setFinalDecision(allowed ? "ALLOWED" : "DENIED");

            // 构建来源详情
            List<UserEffectivePermissionVO.PermissionSourceVO> sourceVOs = sources.stream()
                .filter(s -> s.getSourceRoleId() != null || "USER_OVERRIDE".equals(s.getSource()) || "SUPER_USER".equals(s.getSource()))
                .map(s -> {
                    UserEffectivePermissionVO.PermissionSourceVO sourceVO =
                        new UserEffectivePermissionVO.PermissionSourceVO();
                    sourceVO.setType(s.getSource());
                    sourceVO.setRoleId(s.getSourceRoleId());
                    sourceVO.setRoleName(s.getSourceRoleName());
                    sourceVO.setEffect(s.getEffect());
                    sourceVO.setPriority(s.getPriority());
                    return sourceVO;
                }).collect(Collectors.toList());

            item.setSources(sourceVOs);
            effectivePermissions.add(item);
        }

        vo.setEffectivePermissions(effectivePermissions);
        vo.setTotalCount(effectivePermissions.size());

        return vo;
    }

    // ==================== 权限检查 ====================

    @Override
    public boolean checkPermission(Long userId, String path, String method) {
        AdminUser user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return false;
        }

        // 超级管理员直接放行
        if (user.getIsSuperuser() != null && user.getIsSuperuser() == 1) {
            return true;
        }

        // 获取匹配的权限
        List<AdminPermission> matchedPerms = permissionService.findMatchingPermissions(path, method);
        if (matchedPerms.isEmpty()) {
            return false;
        }

        // 获取用户权限覆盖
        List<AdminUserPermissionOverride> overrides = overrideMapper.selectList(
            new LambdaQueryWrapper<AdminUserPermissionOverride>()
                .eq(AdminUserPermissionOverride::getUserId, userId)
                .eq(AdminUserPermissionOverride::getIsDeleted, 0)
        );

        Map<Long, AdminUserPermissionOverride> overrideMap = overrides.stream()
            .collect(Collectors.toMap(
                AdminUserPermissionOverride::getPermissionId,
                o -> o
            ));

        // 获取角色权限
        Set<Long> roleIds = getUserRoleIds(userId);
        List<AdminRolePermission> allRolePerms = new ArrayList<>();

        for (Long roleId : roleIds) {
            List<AdminRolePermission> rolePerms = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<AdminRolePermission>()
                    .eq(AdminRolePermission::getRoleId, roleId)
                    .eq(AdminRolePermission::getIsDeleted, 0)
            );
            allRolePerms.addAll(rolePerms);
        }

        Map<Long, AdminRolePermission> rolePermMap = allRolePerms.stream()
            .collect(Collectors.toMap(
                AdminRolePermission::getPermissionId,
                rp -> rp,
                (a, b) -> a
            ));

        // 收集所有相关规则
        List<AdminRolePermission> matchedRolePerms = new ArrayList<>();
        boolean hasOverride = false;

        for (AdminPermission perm : matchedPerms) {
            // 先检查用户覆盖
            AdminUserPermissionOverride override = overrideMap.get(perm.getId());
            if (override != null) {
                hasOverride = true;
                // 覆盖直接决定结果
                return "GRANT".equals(override.getEffect());
            }

            // 检查角色权限
            AdminRolePermission rp = rolePermMap.get(perm.getId());
            if (rp != null) {
                matchedRolePerms.add(rp);
            }
        }

        // 有覆盖但没匹配到权限
        if (hasOverride) {
            return false; // 覆盖是DENY或没有GRANT覆盖
        }

        // 无任何规则，默认拒绝
        if (matchedRolePerms.isEmpty()) {
            return false;
        }

        // 按优先级排序，DENY优先
        matchedRolePerms.sort((a, b) -> {
            if (!a.getPriority().equals(b.getPriority())) {
                return b.getPriority().compareTo(a.getPriority());
            }
            return "DENY".equals(a.getEffect()) ? -1 : 1;
        });

        AdminRolePermission topRule = matchedRolePerms.get(0);
        return "GRANT".equals(topRule.getEffect());
    }
}
