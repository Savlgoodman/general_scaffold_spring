package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(description = "用户分组权限")
@Data
public class UserGroupPermissionVO {

    @Schema(description = "分组标识")
    private String groupKey;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "子权限列表")
    private List<UserPermissionItemVO> children;
}
