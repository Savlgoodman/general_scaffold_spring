package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户权限总览")
@Data
public class UserPermissionOverviewVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "是否超级管理员")
    private boolean isSuperuser;

    @Schema(description = "用户拥有的角色")
    private List<RoleBaseVO> roles;

    @Schema(description = "按组分类的全部权限")
    private List<UserPermGroupSection> groups;

    @Schema(description = "当前用户的所有覆盖记录")
    private List<OverrideItem> overrides;

    @Schema(description = "汇总信息")
    private UserPermSummary summary;

    @Schema(description = "用户权限分组")
    @Data
    public static class UserPermGroupSection {

        @Schema(description = "分组标识")
        private String groupKey;

        @Schema(description = "分组名称")
        private String groupName;

        @Schema(description = "该组下的权限行")
        private List<PermissionRow> children;
    }

    @Schema(description = "权限行")
    @Data
    public static class PermissionRow {

        @Schema(description = "权限ID")
        private Long permissionId;

        @Schema(description = "权限名称")
        private String name;

        @Schema(description = "接口路径")
        private String path;

        @Schema(description = "HTTP方法")
        private String method;

        @Schema(description = "是否组权限")
        private boolean isGroup;

        @Schema(description = "最终效果: GRANT/DENY/null(未分配)")
        private String finalEffect;

        @Schema(description = "来源: ROLE/OVERRIDE/NONE")
        private String source;

        @Schema(description = "来源角色名列表")
        private List<String> sourceRoles;

        @Schema(description = "是否有用户覆盖")
        private boolean hasOverride;

        @Schema(description = "覆盖记录ID（用于删除/修改）")
        private Long overrideId;

        @Schema(description = "覆盖的效果: GRANT/DENY")
        private String overrideEffect;
    }

    @Schema(description = "覆盖记录")
    @Data
    public static class OverrideItem {

        @Schema(description = "覆盖记录ID")
        private Long overrideId;

        @Schema(description = "权限ID")
        private Long permissionId;

        @Schema(description = "权限名称")
        private String permissionName;

        @Schema(description = "接口路径")
        private String path;

        @Schema(description = "HTTP方法")
        private String method;

        @Schema(description = "效果: GRANT/DENY")
        private String effect;

        @Schema(description = "创建时间")
        private LocalDateTime createTime;
    }

    @Schema(description = "用户权限汇总")
    @Data
    public static class UserPermSummary {

        @Schema(description = "总权限数")
        private int totalPermissions;

        @Schema(description = "已授权数")
        private int grantedCount;

        @Schema(description = "已拒绝数")
        private int deniedCount;

        @Schema(description = "未分配数")
        private int unassignedCount;

        @Schema(description = "覆盖数")
        private int overrideCount;
    }
}
