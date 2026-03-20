package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(description = "分组权限")
@Data
public class GroupPermissionVO {

    @Schema(description = "分组标识")
    private String groupKey;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "组权限")
    private RolePermissionItemVO groupPermission;

    @Schema(description = "子权限列表")
    private List<RolePermissionItemVO> children;

    @Schema(description = "总数量")
    private Integer totalCount;

    @Schema(description = "已分配数量")
    private Integer assignedCount;
}
