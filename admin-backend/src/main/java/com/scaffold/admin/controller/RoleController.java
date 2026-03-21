package com.scaffold.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.admin.common.R;
import com.scaffold.admin.common.ResultCode;
import com.scaffold.admin.mapper.AdminRoleMapper;
import com.scaffold.admin.model.dto.BatchRolePermissionDTO;
import com.scaffold.admin.model.dto.CreateRoleDTO;
import com.scaffold.admin.model.dto.RevokePermissionsDTO;
import com.scaffold.admin.model.dto.RolePermissionDTO;
import com.scaffold.admin.model.dto.UpdateRoleDTO;
import com.scaffold.admin.model.entity.AdminRole;
import com.scaffold.admin.model.vo.RoleAssignablePermissionVO;
import com.scaffold.admin.model.vo.RoleBaseVO;
import com.scaffold.admin.model.vo.RolePermissionVO;
import com.scaffold.admin.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@Tag(name = "roles", description = "角色管理相关接口")
public class RoleController {

    private final RoleService roleService;
    private final AdminRoleMapper roleMapper;

    @GetMapping
    @Operation(summary = "角色列表", description = "分页获取角色列表")
    public R<Page<RoleBaseVO>> list(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String keyword
    ) {
        Page<AdminRole> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<AdminRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AdminRole::getIsDeleted, 0)
            .orderByAsc(AdminRole::getSort);

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w
                .like(AdminRole::getName, keyword)
                .or()
                .like(AdminRole::getCode, keyword)
            );
        }

        Page<AdminRole> rolePage = roleMapper.selectPage(page, queryWrapper);

        Page<RoleBaseVO> voPage = new Page<>(rolePage.getCurrent(), rolePage.getSize(), rolePage.getTotal());
        voPage.setRecords(rolePage.getRecords().stream()
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
            .toList());

        return R.ok(voPage);
    }

    @GetMapping("/{id}")
    @Operation(summary = "角色详情", description = "获取角色详情")
    public R<RoleBaseVO> getDetail(@PathVariable Long id) {
        AdminRole role = roleMapper.selectById(id);
        if (role == null) {
            return R.error(ResultCode.NOT_FOUND, "角色不存在");
        }

        RoleBaseVO vo = new RoleBaseVO();
        vo.setId(role.getId());
        vo.setName(role.getName());
        vo.setCode(role.getCode());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setSort(role.getSort());
        return R.ok(vo);
    }

    @PostMapping
    @Operation(summary = "创建角色", description = "创建新角色")
    public R<RoleBaseVO> create(@RequestBody @Valid CreateRoleDTO dto) {
        try {
            AdminRole role = roleService.createRole(dto);
            RoleBaseVO vo = new RoleBaseVO();
            vo.setId(role.getId());
            vo.setName(role.getName());
            vo.setCode(role.getCode());
            vo.setDescription(role.getDescription());
            vo.setStatus(role.getStatus());
            vo.setSort(role.getSort());
            return R.ok(vo);
        } catch (IllegalArgumentException e) {
            return R.error(ResultCode.PARAM_ERROR, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色", description = "更新角色信息")
    public R<RoleBaseVO> update(
        @PathVariable Long id,
        @RequestBody @Valid UpdateRoleDTO dto
    ) {
        try {
            AdminRole role = roleService.updateRole(id, dto);
            RoleBaseVO vo = new RoleBaseVO();
            vo.setId(role.getId());
            vo.setName(role.getName());
            vo.setCode(role.getCode());
            vo.setDescription(role.getDescription());
            vo.setStatus(role.getStatus());
            vo.setSort(role.getSort());
            return R.ok(vo);
        } catch (IllegalArgumentException e) {
            return R.error(ResultCode.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色", description = "删除单个角色")
    public R<Void> delete(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.error(ResultCode.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping
    @Operation(summary = "批量删除角色", description = "批量删除角色")
    public R<Void> deleteBatch(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return R.error(ResultCode.PARAM_ERROR, "角色ID列表不能为空");
        }
        roleService.deleteRoles(ids);
        return R.ok();
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "角色权限详情", description = "获取角色已分配权限（含分组结构）")
    public R<RolePermissionVO> getPermissions(@PathVariable Long id) {
        RolePermissionVO permissions = roleService.getRolePermissionsDetail(id);
        if (permissions == null) {
            return R.error(ResultCode.NOT_FOUND, "角色不存在");
        }
        return R.ok(permissions);
    }

    @GetMapping("/{id}/permissions/assignable")
    @Operation(summary = "角色可分配权限", description = "获取可分配的权限（已分配/未分配状态）")
    public R<RoleAssignablePermissionVO> getAssignablePermissions(@PathVariable Long id) {
        RoleAssignablePermissionVO permissions = roleService.getRoleAssignablePermissions(id);
        return R.ok(permissions);
    }

    @PostMapping("/{id}/permissions/groups")
    @Operation(summary = "分配组权限", description = "批量分配组权限给角色")
    public R<Void> assignGroupPermissions(
        @PathVariable Long id,
        @RequestBody @Valid BatchRolePermissionDTO dto
    ) {
        roleService.assignPermissions(id, dto.getPermissions());
        return R.ok();
    }

    @PostMapping("/{id}/permissions/children")
    @Operation(summary = "分配子权限", description = "批量分配子权限给角色")
    public R<Void> assignChildPermissions(
        @PathVariable Long id,
        @RequestBody @Valid BatchRolePermissionDTO dto
    ) {
        roleService.assignPermissions(id, dto.getPermissions());
        return R.ok();
    }

    @DeleteMapping("/{id}/permissions")
    @Operation(summary = "批量撤销角色权限", description = "批量撤销角色权限")
    public R<Void> revokePermissions(
        @PathVariable Long id,
        @RequestBody @Valid RevokePermissionsDTO dto
    ) {
        roleService.revokePermissions(id, dto.getPermissionIds());
        return R.ok();
    }
}
