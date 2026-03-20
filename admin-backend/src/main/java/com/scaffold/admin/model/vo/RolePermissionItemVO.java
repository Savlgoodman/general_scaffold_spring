package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "角色权限项")
@Data
public class RolePermissionItemVO {

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

    @Schema(description = "是否组权限")
    private Boolean isGroup;

    @Schema(description = "分组标识")
    private String groupKey;

    @Schema(description = "是否已分配")
    private Boolean isAssigned;

    @Schema(description = "生效方式（GRANT/DENY）")
    private String effect;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "是否被覆盖")
    private Boolean isOverridden;
}
