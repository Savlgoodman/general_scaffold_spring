package com.scaffold.admin.controller;

import com.scaffold.admin.common.R;
import com.scaffold.admin.common.ResultCode;
import com.scaffold.admin.mapper.AdminUserMapper;
import com.scaffold.admin.model.dto.AssignRolesDTO;
import com.scaffold.admin.model.dto.SyncUserOverridesDTO;
import com.scaffold.admin.model.entity.AdminRole;
import com.scaffold.admin.model.entity.AdminUser;
import com.scaffold.admin.model.vo.RoleBaseVO;
import com.scaffold.admin.model.vo.UserPermissionOverviewVO;
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
    @Operation(operationId = "getUserRoles", summary = "获取用户角色", description = "获取用户的角色列表")
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
    @Operation(operationId = "syncUserRoles", summary = "同步用户角色", description = "同步用户角色（全量替换，传入的为最终角色列表）")
    public R<Void> assignUserRoles(
        @PathVariable Long id,
        @RequestBody @Valid AssignRolesDTO dto
    ) {
        AdminUser user = userMapper.selectById(id);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        rbacService.syncUserRoles(id, dto.getRoleIds());
        return R.ok();
    }

@GetMapping("/{id}/permissions")
    @Operation(operationId = "getUserPermissions", summary = "用户权限总览", description = "获取用户所有权限的完整视图（含来源、覆盖状态、操作ID）")
    public R<UserPermissionOverviewVO> getUserPermissions(@PathVariable Long id) {
        AdminUser user = userMapper.selectById(id);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        UserPermissionOverviewVO overview = rbacService.getUserPermissionOverview(id);
        return R.ok(overview);
    }

    @PutMapping("/{id}/permission-overrides")
    @Operation(operationId = "syncUserOverrides", summary = "同步用户权限覆盖", description = "同步用户权限覆盖（对比差异，批量增删改）")
    public R<Void> syncUserOverrides(
        @PathVariable Long id,
        @RequestBody @Valid SyncUserOverridesDTO dto
    ) {
        AdminUser user = userMapper.selectById(id);
        if (user == null) {
            return R.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        rbacService.syncUserOverrides(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}/permission-overrides/{overrideId}")
    @Operation(operationId = "removeUserPermissionOverride", summary = "删除用户权限覆盖", description = "删除单个权限覆盖")
    public R<Void> removeUserPermissionOverride(
        @PathVariable Long id,
        @PathVariable Long overrideId
    ) {
        rbacService.removeUserPermissionOverride(id, overrideId);
        return R.ok();
    }

    @DeleteMapping("/{id}/permission-overrides")
    @Operation(operationId = "clearUserPermissionOverrides", summary = "清除用户所有权限覆盖", description = "清除用户所有权限覆盖")
    public R<Void> clearUserPermissionOverrides(@PathVariable Long id) {
        rbacService.clearUserPermissionOverrides(id);
        return R.ok();
    }
}
