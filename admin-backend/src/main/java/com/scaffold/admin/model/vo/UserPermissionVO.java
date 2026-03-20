package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(description = "用户权限详情")
@Data
public class UserPermissionVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "是否超级管理员")
    private Boolean isSuperuser;

    @Schema(description = "用户角色列表")
    private List<RoleBaseVO> roles;

    @Schema(description = "有效权限列表")
    private List<UserPermissionItemVO> effectivePermissions;

    @Schema(description = "分组权限列表")
    private List<UserGroupPermissionVO> groupedPermissions;

    @Schema(description = "权限覆盖列表")
    private List<PermissionOverrideVO> overrides;
}
