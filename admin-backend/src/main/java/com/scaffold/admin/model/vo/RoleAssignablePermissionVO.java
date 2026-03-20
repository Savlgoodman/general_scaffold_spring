package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(description = "角色可分配权限")
@Data
public class RoleAssignablePermissionVO {

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "分组列表")
    private List<AssignableGroupVO> groups;

    @Schema(description = "分组可分配权限")
    @Data
    public static class AssignableGroupVO {
        @Schema(description = "分组标识")
        private String groupKey;

        @Schema(description = "分组名称")
        private String groupName;

        @Schema(description = "组权限信息")
        private AssignableItemVO groupPermission;

        @Schema(description = "未分配的子权限")
        private List<AssignableItemVO> unassignedChildren;
    }

    @Schema(description = "可分配权限项")
    @Data
    public static class AssignableItemVO {
        @Schema(description = "权限ID")
        private Long id;

        @Schema(description = "权限名称")
        private String name;

        @Schema(description = "接口路径")
        private String path;

        @Schema(description = "HTTP方法")
        private String method;

        @Schema(description = "是否已分配")
        private Boolean isAssigned;

        @Schema(description = "生效方式（GRANT/DENY）")
        private String effect;

        @Schema(description = "优先级")
        private Integer priority;
    }
}
