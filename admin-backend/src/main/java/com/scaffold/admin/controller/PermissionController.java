package com.scaffold.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.admin.common.R;
import com.scaffold.admin.common.ResultCode;
import com.scaffold.admin.mapper.AdminPermissionMapper;
import com.scaffold.admin.model.entity.AdminPermission;
import com.scaffold.admin.model.vo.PermissionBaseVO;
import com.scaffold.admin.model.vo.PermissionGroupVO;
import com.scaffold.admin.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
@Tag(name = "permissions", description = "权限管理相关接口")
public class PermissionController {

    private final PermissionService permissionService;
    private final AdminPermissionMapper permissionMapper;

    @GetMapping
    @Operation(operationId = "listPermissions", summary = "权限列表", description = "分页获取权限列表")
    public R<Page<PermissionBaseVO>> list(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String keyword
    ) {
        Page<AdminPermission> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<AdminPermission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AdminPermission::getIsDeleted, 0)
            .orderByAsc(AdminPermission::getSort);

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w
                .like(AdminPermission::getName, keyword)
                .or()
                .like(AdminPermission::getCode, keyword)
            );
        }

        Page<AdminPermission> permPage = permissionMapper.selectPage(page, queryWrapper);

        Page<PermissionBaseVO> voPage = new Page<>(permPage.getCurrent(), permPage.getSize(), permPage.getTotal());
        voPage.setRecords(permPage.getRecords().stream()
            .map(permissionService::convertToBaseVO)
            .toList());

        return R.ok(voPage);
    }

    @GetMapping("/{id:\\d+}")
    @Operation(operationId = "getPermissionDetail", summary = "权限详情", description = "获取单个权限详情")
    public R<PermissionBaseVO> getDetail(@PathVariable("id") Long id) {
        AdminPermission permission = permissionService.getById(id);
        if (permission == null) {
            return R.error(ResultCode.NOT_FOUND, "权限不存在");
        }
        return R.ok(permissionService.convertToBaseVO(permission));
    }

    @GetMapping("/groups")
    @Operation(operationId = "getPermissionGroups", summary = "权限分组列表", description = "获取所有权限分组（用于角色分配选择）")
    public R<List<PermissionGroupVO>> getGroups() {
        List<PermissionGroupVO> groups = permissionService.getAllGroupedPermissions();
        return R.ok(groups);
    }
}
