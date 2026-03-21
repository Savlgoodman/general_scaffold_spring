package com.scaffold.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.scaffold.admin.mapper.AdminPermissionMapper;
import com.scaffold.admin.mapper.AdminRoleMapper;
import com.scaffold.admin.mapper.AdminRolePermissionMapper;
import com.scaffold.admin.model.dto.CreateRoleDTO;
import com.scaffold.admin.model.dto.RolePermissionDTO;
import com.scaffold.admin.model.dto.UpdateRoleDTO;
import com.scaffold.admin.model.entity.AdminPermission;
import com.scaffold.admin.model.entity.AdminRole;
import com.scaffold.admin.model.entity.AdminRolePermission;
import com.scaffold.admin.model.vo.*;
import com.scaffold.admin.service.PermissionService;
import com.scaffold.admin.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final AdminRoleMapper roleMapper;
    private final AdminRolePermissionMapper rolePermissionMapper;
    private final AdminPermissionMapper permissionMapper;
    private final PermissionService permissionService;

    @Override
    public AdminRole getById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public AdminRole getByCode(String code) {
        return roleMapper.selectOne(
            new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getCode, code)
                .eq(AdminRole::getIsDeleted, 0)
        );
    }

    @Override
    public List<AdminRole> getAll() {
        return roleMapper.selectList(
            new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getIsDeleted, 0)
                .orderByAsc(AdminRole::getSort)
        );
    }

    @Override
    public List<AdminRole> getActiveRoles() {
        return roleMapper.selectList(
            new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getIsDeleted, 0)
                .eq(AdminRole::getStatus, 1)
                .orderByAsc(AdminRole::getSort)
        );
    }

    @Override
    @Transactional
    public AdminRole createRole(CreateRoleDTO dto) {
        if (isCodeExists(dto.getCode())) {
            throw new IllegalArgumentException("角色编码已存在: " + dto.getCode());
        }

        AdminRole role = new AdminRole();
        role.setName(dto.getName());
        role.setCode(dto.getCode());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        role.setSort(dto.getSort() != null ? dto.getSort() : 0);

        roleMapper.insert(role);
        return role;
    }

    @Override
    @Transactional
    public AdminRole updateRole(Long id, UpdateRoleDTO dto) {
        AdminRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在: " + id);
        }

        if (dto.getName() != null) {
            role.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            role.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }
        if (dto.getSort() != null) {
            role.setSort(dto.getSort());
        }

        roleMapper.updateById(role);
        return role;
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        AdminRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在: " + id);
        }

        role.setIsDeleted(1);
        roleMapper.updateById(role);
    }

    @Override
    @Transactional
    public void deleteRoles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        roleMapper.update(
            new AdminRole(),
            new LambdaUpdateWrapper<AdminRole>()
                .in(AdminRole::getId, ids)
                .set(AdminRole::getIsDeleted, 1)
        );
    }

    @Override
    public boolean isCodeExists(String code) {
        Long count = roleMapper.selectCount(
            new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getCode, code)
                .eq(AdminRole::getIsDeleted, 0)
        );
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<RolePermissionDTO> permissions) {
        for (RolePermissionDTO dto : permissions) {
            // 检查是否已存在
            AdminRolePermission existing = rolePermissionMapper.selectOne(
                new LambdaQueryWrapper<AdminRolePermission>()
                    .eq(AdminRolePermission::getRoleId, roleId)
                    .eq(AdminRolePermission::getPermissionId, dto.getPermissionId())
                    .eq(AdminRolePermission::getIsDeleted, 0)
            );

            if (existing != null) {
                // 更新现有记录
                existing.setEffect(dto.getEffect());
                existing.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
                rolePermissionMapper.updateById(existing);
            } else {
                // 新增记录
                AdminRolePermission newPerm = new AdminRolePermission();
                newPerm.setRoleId(roleId);
                newPerm.setPermissionId(dto.getPermissionId());
                newPerm.setEffect(dto.getEffect());
                newPerm.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
                rolePermissionMapper.insert(newPerm);
            }
        }
    }

    @Override
    @Transactional
    public void removePermissions(Long roleId, List<Long> permissionIds) {
        for (Long permissionId : permissionIds) {
            AdminRolePermission existing = rolePermissionMapper.selectOne(
                new LambdaQueryWrapper<AdminRolePermission>()
                    .eq(AdminRolePermission::getRoleId, roleId)
                    .eq(AdminRolePermission::getPermissionId, permissionId)
                    .eq(AdminRolePermission::getIsDeleted, 0)
            );

            if (existing != null) {
                existing.setIsDeleted(1);
                rolePermissionMapper.updateById(existing);
            }
        }
    }

    @Override
    @Transactional
    public void revokePermissions(Long roleId, List<Long> permissionIds) {
        removePermissions(roleId, permissionIds);
    }

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        List<AdminRolePermission> rolePermissions = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<AdminRolePermission>()
                .eq(AdminRolePermission::getRoleId, roleId)
                .eq(AdminRolePermission::getIsDeleted, 0)
        );
        return rolePermissions.stream()
            .map(AdminRolePermission::getPermissionId)
            .collect(Collectors.toList());
    }

    @Override
    public RolePermissionVO getRolePermissionsDetail(Long roleId) {
        AdminRole role = getById(roleId);
        if (role == null) {
            return null;
        }

        RolePermissionVO vo = new RolePermissionVO();
        vo.setRoleId(role.getId());
        vo.setRoleName(role.getName());
        vo.setRoleCode(role.getCode());

        // 获取角色关联的所有权限
        List<AdminRolePermission> rolePermissions = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<AdminRolePermission>()
                .eq(AdminRolePermission::getRoleId, roleId)
                .eq(AdminRolePermission::getIsDeleted, 0)
        );

        // 构建权限ID到角色权限的映射
        Map<Long, AdminRolePermission> permRoleMap = rolePermissions.stream()
            .collect(Collectors.toMap(
                AdminRolePermission::getPermissionId,
                rp -> rp,
                (a, b) -> a
            ));

        Set<Long> assignedPermIds = permRoleMap.keySet();

        // 获取所有分组权限
        List<PermissionGroupVO> allGroups = permissionService.getAllGroupedPermissions();

        // 构建返回结果
        List<GroupPermissionVO> groupedPermissions = new ArrayList<>();
        int totalCount = 0;
        int grantCount = 0;
        int denyCount = 0;
        int groupCount = 0;
        int childCount = 0;

        for (PermissionGroupVO group : allGroups) {
            GroupPermissionVO groupVO = new GroupPermissionVO();
            groupVO.setGroupKey(group.getGroupKey());
            groupVO.setGroupName(group.getGroupName());

            // 处理组权限
            PermissionBaseVO groupPerm = group.getGroupPermission();
            RolePermissionItemVO groupItemVO = new RolePermissionItemVO();
            if (groupPerm != null) {
                groupItemVO.setId(groupPerm.getId());
                groupItemVO.setName(groupPerm.getName());
                groupItemVO.setCode(groupPerm.getCode());
                groupItemVO.setPath(groupPerm.getPath());
                groupItemVO.setMethod(groupPerm.getMethod());
                groupItemVO.setIsGroup(true);
                groupItemVO.setGroupKey(groupPerm.getGroupKey());

                AdminRolePermission rp = permRoleMap.get(groupPerm.getId());
                if (rp != null) {
                    groupItemVO.setIsAssigned(true);
                    groupItemVO.setEffect(rp.getEffect());
                    groupItemVO.setPriority(rp.getPriority());
                    groupCount++;
                    if ("GRANT".equals(rp.getEffect())) {
                        grantCount++;
                    } else {
                        denyCount++;
                    }
                } else {
                    groupItemVO.setIsAssigned(false);
                    groupItemVO.setEffect("GRANT");
                    groupItemVO.setPriority(0);
                }
            }
            groupVO.setGroupPermission(groupItemVO);

            // 处理子权限
            List<RolePermissionItemVO> children = new ArrayList<>();
            List<RolePermissionItemVO> unassignedChildren = new ArrayList<>();

            for (PermissionBaseVO childPerm : group.getChildren()) {
                RolePermissionItemVO childItemVO = new RolePermissionItemVO();
                childItemVO.setId(childPerm.getId());
                childItemVO.setName(childPerm.getName());
                childItemVO.setCode(childPerm.getCode());
                childItemVO.setPath(childPerm.getPath());
                childItemVO.setMethod(childPerm.getMethod());
                childItemVO.setIsGroup(false);
                childItemVO.setGroupKey(childPerm.getGroupKey());

                AdminRolePermission rp = permRoleMap.get(childPerm.getId());
                if (rp != null) {
                    childItemVO.setIsAssigned(true);
                    childItemVO.setEffect(rp.getEffect());
                    childItemVO.setPriority(rp.getPriority());
                    childCount++;
                    if ("GRANT".equals(rp.getEffect())) {
                        grantCount++;
                    } else {
                        denyCount++;
                    }
                    children.add(childItemVO);
                } else {
                    childItemVO.setIsAssigned(false);
                    childItemVO.setEffect("GRANT");
                    childItemVO.setPriority(0);
                    unassignedChildren.add(childItemVO);
                }
                totalCount++;
            }

            groupVO.setChildren(children);
            groupVO.setTotalCount(group.getChildren().size());
            groupVO.setAssignedCount(children.size());

            groupedPermissions.add(groupVO);
        }

        vo.setGroupedPermissions(groupedPermissions);

        // 统计信息
        RolePermissionVO.PermissionSummary summary = new RolePermissionVO.PermissionSummary();
        summary.setTotalPermissions(totalCount);
        summary.setGroupPermissions(groupCount);
        summary.setChildPermissions(childCount);
        summary.setGrantCount(grantCount);
        summary.setDenyCount(denyCount);
        vo.setSummary(summary);

        return vo;
    }

    @Override
    public RoleAssignablePermissionVO getRoleAssignablePermissions(Long roleId) {
        RoleAssignablePermissionVO vo = new RoleAssignablePermissionVO();
        vo.setRoleId(roleId);

        // 获取角色关联的所有权限
        List<AdminRolePermission> rolePermissions = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<AdminRolePermission>()
                .eq(AdminRolePermission::getRoleId, roleId)
                .eq(AdminRolePermission::getIsDeleted, 0)
        );

        Set<Long> assignedPermIds = rolePermissions.stream()
            .map(AdminRolePermission::getPermissionId)
            .collect(Collectors.toSet());

        // 获取所有分组权限
        List<PermissionGroupVO> allGroups = permissionService.getAllGroupedPermissions();

        List<RoleAssignablePermissionVO.AssignableGroupVO> groups = new ArrayList<>();

        for (PermissionGroupVO group : allGroups) {
            RoleAssignablePermissionVO.AssignableGroupVO assignableGroup =
                new RoleAssignablePermissionVO.AssignableGroupVO();
            assignableGroup.setGroupKey(group.getGroupKey());
            assignableGroup.setGroupName(group.getGroupName());

            // 组权限
            RoleAssignablePermissionVO.AssignableItemVO groupItem =
                new RoleAssignablePermissionVO.AssignableItemVO();
            PermissionBaseVO groupPerm = group.getGroupPermission();
            if (groupPerm != null) {
                groupItem.setId(groupPerm.getId());
                groupItem.setName(groupPerm.getName());
                groupItem.setPath(groupPerm.getPath());
                groupItem.setMethod(groupPerm.getMethod());
                groupItem.setIsAssigned(assignedPermIds.contains(groupPerm.getId()));
            }
            assignableGroup.setGroupPermission(groupItem);

            // 未分配的子权限
            List<RoleAssignablePermissionVO.AssignableItemVO> unassignedChildren = new ArrayList<>();
            for (PermissionBaseVO childPerm : group.getChildren()) {
                if (!assignedPermIds.contains(childPerm.getId())) {
                    RoleAssignablePermissionVO.AssignableItemVO childItem =
                        new RoleAssignablePermissionVO.AssignableItemVO();
                    childItem.setId(childPerm.getId());
                    childItem.setName(childPerm.getName());
                    childItem.setPath(childPerm.getPath());
                    childItem.setMethod(childPerm.getMethod());
                    childItem.setIsAssigned(false);
                    unassignedChildren.add(childItem);
                }
            }
            assignableGroup.setUnassignedChildren(unassignedChildren);

            groups.add(assignableGroup);
        }

        vo.setGroups(groups);
        return vo;
    }
}
