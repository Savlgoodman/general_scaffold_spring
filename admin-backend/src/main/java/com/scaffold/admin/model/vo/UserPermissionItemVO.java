package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户权限项")
@Data
public class UserPermissionItemVO {

    @Schema(description = "权限ID")
    private Long permissionId;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "权限编码")
    private String code;

    @Schema(description = "接口路径")
    private String path;

    @Schema(description = "HTTP方法")
    private String method;

    @Schema(description = "是否组权限")
    private Boolean isGroup;

    @Schema(description = "分组标识")
    private String groupKey;

    @Schema(description = "生效方式（GRANT/DENY）")
    private String effect;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "权限来源（SUPER_USER/ROLE/USER_OVERRIDE）")
    private String source;

    @Schema(description = "来源角色ID")
    private Long sourceRoleId;

    @Schema(description = "来源角色名称")
    private String sourceRoleName;
}
