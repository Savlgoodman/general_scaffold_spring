package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户菜单总览")
public class UserMenuOverviewVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "是否超级管理员")
    private boolean isSuperuser;

    @Schema(description = "用户角色列表")
    private List<RoleBaseVO> roles;

    @Schema(description = "菜单分组列表")
    private List<UserMenuOverviewVOGroup> groups;

    @Schema(description = "统计摘要")
    private UserMenuOverviewVOSummary summary;

    @Data
    @Schema(description = "菜单分组（顶级菜单/目录）")
    public static class UserMenuOverviewVOGroup {

        @Schema(description = "菜单ID")
        private Long id;

        @Schema(description = "菜单名称")
        private String name;

        @Schema(description = "路由路径")
        private String path;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "菜单类型")
        private String type;

        @Schema(description = "是否有权限")
        private boolean granted;

        @Schema(description = "来源：SUPER_USER/ROLE/NONE")
        private String source;

        @Schema(description = "授予该菜单的角色名列表")
        private List<String> sourceRoles;

        @Schema(description = "子菜单列表")
        private List<UserMenuOverviewVOItem> children;

        @Schema(description = "已授权子菜单数")
        private int grantedCount;

        @Schema(description = "总子菜单数")
        private int totalCount;
    }

    @Data
    @Schema(description = "子菜单项")
    public static class UserMenuOverviewVOItem {

        @Schema(description = "菜单ID")
        private Long id;

        @Schema(description = "菜单名称")
        private String name;

        @Schema(description = "路由路径")
        private String path;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "菜单类型")
        private String type;

        @Schema(description = "是否有权限")
        private boolean granted;

        @Schema(description = "来源：SUPER_USER/ROLE/DIRECTORY/NONE")
        private String source;

        @Schema(description = "授予该菜单的角色名列表")
        private List<String> sourceRoles;

        @Schema(description = "是否被目录覆盖")
        private boolean coveredByDirectory;
    }

    @Data
    @Schema(description = "统计摘要")
    public static class UserMenuOverviewVOSummary {

        @Schema(description = "总菜单数")
        private int totalMenus;

        @Schema(description = "已授权菜单数")
        private int grantedCount;
    }
}
