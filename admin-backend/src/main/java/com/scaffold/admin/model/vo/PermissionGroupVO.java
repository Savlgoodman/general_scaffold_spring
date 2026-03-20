package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(description = "权限分组信息")
@Data
public class PermissionGroupVO {

    @Schema(description = "分组标识")
    private String groupKey;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "组权限")
    private PermissionBaseVO groupPermission;

    @Schema(description = "子权限列表")
    private List<PermissionBaseVO> children;

    @Schema(description = "子权限数量")
    private Integer totalCount;
}
