package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(description = "用户可用权限（未拥有的）")
@Data
public class UserAvailablePermissionVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "是否超级管理员")
    private Boolean isSuperuser;

    @Schema(description = "未分配权限（按分组聚合）")
    private List<UnassignedGroupVO> unassignedPermissions;

    @Schema(description = "可被覆盖的权限（当前被拒绝的）")
    private List<CanOverrideVO> canBeOverridden;

    @Schema(description = "未分配权限分组")
    @Data
    public static class UnassignedGroupVO {
        @Schema(description = "分组标识")
        private String groupKey;

        @Schema(description = "分组名称")
        private String groupName;

        @Schema(description = "权限列表")
        private List<UnassignedPermissionVO> permissions;
    }

    @Schema(description = "未分配权限项")
    @Data
    public static class UnassignedPermissionVO {
        @Schema(description = "权限ID")
        private Long id;

        @Schema(description = "权限名称")
        private String name;

        @Schema(description = "接口路径")
        private String path;

        @Schema(description = "HTTP方法")
        private String method;

        @Schema(description = "当前效果（null表示未分配）")
        private String currentEffect;

        @Schema(description = "原因（UNASSIGNED/ROLE_DENIED）")
        private String reason;
    }

    @Schema(description = "可覆盖权限项")
    @Data
    public static class CanOverrideVO {
        @Schema(description = "权限ID")
        private Long id;

        @Schema(description = "权限名称")
        private String name;

        @Schema(description = "原因")
        private String reason;
    }
}
