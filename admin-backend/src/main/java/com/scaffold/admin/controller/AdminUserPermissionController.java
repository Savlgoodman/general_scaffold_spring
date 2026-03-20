package com.scaffold.admin.controller;

import com.scaffold.admin.common.R;
import com.scaffold.admin.common.ResultCode;
import com.scaffold.admin.mapper.AdminUserMapper;
import com.scaffold.admin.model.dto.AssignRolesDTO;
import com.scaffold.admin.model.dto.UserPermissionOverrideDTO;
import com.scaffold.admin.model.entity.AdminRole;
import com.scaffold.admin.model.entity.AdminUser;
import com.scaffold.admin.model.vo.*;
import com.scaffold.admin.service.RBACService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/admin-users")
@RequiredArgsConstructor
@Tag(name = "admin-users-permission", description = "用户权限管理相关接口")
public class AdminUserPermissionController {

    private final RBACService rbacService;
    private final AdminUserMapper userMapper;

    @GetMapping("/{id}/roles")
    @Operation(summary = "获取用户角色", description = "获取用户的角色列表")
    public R<List<RoleBaseVO>> getUserRoles(@PathVariable Long id) {
        AdminUser user = userMapper.selectById(id);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        List<AdminRole> roles = rbacService.getUserRoles(id);
        List<RoleBaseVO> voList = roles.stream()
            .map(role -> {
                RoleBaseVO vo = new RoleBaseVO();
                vo.setId(role.getId());
                vo.setName(role.getName());
                vo.setCode(role.getCode());
                vo.setDescription(role.getDescription());
                vo.setStatus(role.getStatus());
                vo.setSort(role.getSort());
                return vo;
            })
            .toList();

        return R.ok(voList);
    }

    @PostMapping("/{id}/roles")
    @Operation(summary = "分配用户角色", description = "分配角色给用户")
    public R<Void> assignUserRoles(
        @PathVariable Long id,
        @RequestBody @Valid AssignRolesDTO dto
    ) {
        AdminUser user = userMapper.selectById(id);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        rbacService.assignUserRoles(id, dto.getRoleIds());
        return R.ok();
    }

    @DeleteMapping("/{id}/roles")
    @Operation(summary = "批量撤销用户角色", description = "批量撤销用户角色")
    public R<Void> revokeUserRoles(
        @PathVariable Long id,
        @RequestBody @Valid AssignRolesDTO dto
    ) {
        rbacService.removeUserRoles(id, dto.getRoleIds());
        return R.ok();
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "获取用户有效权限", description = "获取用户已有效权限（含来源）")
    public R<UserPermissionVO> getUserPermissions(@PathVariable Long id) {
        AdminUser user = userMapper.selectById(id);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        UserPermissionVO permissions = rbacService.getUserPermissionsDetail(id);
        return R.ok(permissions);
    }

    @GetMapping("/{id}/permissions/effective")
    @Operation(summary = "获取用户最终有效权限", description = "获取用户最终有效权限（已计算最终决策）")
    public R<UserEffectivePermissionVO> getUserEffectivePermissions(@PathVariable Long id) {
        AdminUser user = userMapper.selectById(id);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        UserEffectivePermissionVO effectivePermissions = rbacService.getUserEffectivePermissions(id);
        return R.ok(effectivePermissions);
    }

    @GetMapping("/{id}/permissions/available")
    @Operation(summary = "获取用户可用权限", description = "获取用户未拥有的权限（用于添加覆盖）")
    public R<UserAvailablePermissionVO> getUserAvailablePermissions(@PathVariable Long id) {
        AdminUser user = userMapper.selectById(id);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        UserAvailablePermissionVO availablePermissions = rbacService.getUserAvailablePermissions(id);
        return R.ok(availablePermissions);
    }

    @GetMapping("/{id}/permission-overrides")
    @Operation(summary = "获取用户权限覆盖列表", description = "获取用户权限覆盖列表")
    public R<List<PermissionOverrideVO>> getUserPermissionOverrides(@PathVariable Long id) {
        AdminUser user = userMapper.selectById(id);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        List<PermissionOverrideVO> overrides = rbacService.getUserPermissionOverrides(id);
        return R.ok(overrides);
    }

    @PostMapping("/{id}/permission-overrides")
    @Operation(summary = "设置用户权限覆盖", description = "设置用户权限覆盖")
    public R<Void> setUserPermissionOverride(
        @PathVariable Long id,
        @RequestBody @Valid UserPermissionOverrideDTO dto
    ) {
        AdminUser user = userMapper.selectById(id);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }

        rbacService.setUserPermissionOverride(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}/permission-overrides/{overrideId}")
    @Operation(summary = "删除用户权限覆盖", description = "删除单个权限覆盖")
    public R<Void> removeUserPermissionOverride(
        @PathVariable Long id,
        @PathVariable Long overrideId
    ) {
        rbacService.removeUserPermissionOverride(id, overrideId);
        return R.ok();
    }

    @DeleteMapping("/{id}/permission-overrides")
    @Operation(summary = "清除用户所有权限覆盖", description = "清除用户所有权限覆盖")
    public R<Void> clearUserPermissionOverrides(@PathVariable Long id) {
        rbacService.clearUserPermissionOverrides(id);
        return R.ok();
    }
}
