package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(description = "角色权限详情")
@Data
public class RolePermissionVO {

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "分组权限列表")
    private List<GroupPermissionVO> groupedPermissions;

    @Schema(description = "权限统计")
    private PermissionSummary summary;

    @Schema(description = "权限统计")
    @Data
    public static class PermissionSummary {
        @Schema(description = "总权限数")
        private Integer totalPermissions;

        @Schema(description = "组权限数")
        private Integer groupPermissions;

        @Schema(description = "子权限数")
        private Integer childPermissions;

        @Schema(description = "允许数")
        private Integer grantCount;

        @Schema(description = "拒绝数")
        private Integer denyCount;
    }
}
