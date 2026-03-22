package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "角色权限完整视图")
@Data
public class RolePermissionFullVO {

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "按组分类的权限列表")
    private List<GroupSection> groups;

    @Schema(description = "汇总信息")
    private Summary summary;

    @Schema(description = "权限分组")
    @Data
    public static class GroupSection {

        @Schema(description = "分组标识")
        private String groupKey;

        @Schema(description = "分组名称")
        private String groupName;

        @Schema(description = "组权限（该组的顶层权限）")
        private PermissionItem groupPermission;

        @Schema(description = "该组下的所有子权限")
        private List<PermissionItem> children;

        @Schema(description = "已分配数量")
        private int assignedCount;

        @Schema(description = "总数量")
        private int totalCount;
    }

    @Schema(description = "权限项")
    @Data
    public static class PermissionItem {

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

        @Schema(description = "是否已分配给此角色")
        private boolean assigned;

        @Schema(description = "效果: GRANT/DENY/null")
        private String effect;

        @Schema(description = "是否被组权限覆盖（组已GRANT时为true）")
        private boolean coveredByGroup;
    }

    @Schema(description = "权限汇总")
    @Data
    public static class Summary {

        @Schema(description = "总权限数")
        private int totalPermissions;

        @Schema(description = "已分配数")
        private int assignedCount;

        @Schema(description = "GRANT数量")
        private int grantCount;

        @Schema(description = "DENY数量")
        private int denyCount;
    }
}
