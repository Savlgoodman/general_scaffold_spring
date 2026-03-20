package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "权限基础信息")
@Data
public class PermissionBaseVO {

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

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "权限描述")
    private String description;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;
}
