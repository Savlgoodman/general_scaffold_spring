package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(description = "用户最终有效权限")
@Data
public class UserEffectivePermissionVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "是否超级管理员")
    private Boolean isSuperuser;

    @Schema(description = "最终有效权限列表")
    private List<EffectivePermissionItemVO> effectivePermissions;

    @Schema(description = "总数量")
    private Integer totalCount;

    @Schema(description = "最终有效权限项")
    @Data
    public static class EffectivePermissionItemVO {
        @Schema(description = "权限ID")
        private Long permissionId;

        @Schema(description = "权限名称")
        private String name;

        @Schema(description = "接口路径")
        private String path;

        @Schema(description = "HTTP方法")
        private String method;

        @Schema(description = "有效效果（GRANT/DENY）")
        private String effectiveEffect;

        @Schema(description = "最终决策（ALLOWED/DENIED）")
        private String finalDecision;

        @Schema(description = "来源详情")
        private List<PermissionSourceVO> sources;
    }

    @Schema(description = "权限来源详情")
    @Data
    public static class PermissionSourceVO {
        @Schema(description = "来源类型（SUPER_USER/ROLE/USER_OVERRIDE）")
        private String type;

        @Schema(description = "角色ID")
        private Long roleId;

        @Schema(description = "角色名称")
        private String roleName;

        @Schema(description = "效果")
        private String effect;

        @Schema(description = "优先级")
        private Integer priority;
    }
}
