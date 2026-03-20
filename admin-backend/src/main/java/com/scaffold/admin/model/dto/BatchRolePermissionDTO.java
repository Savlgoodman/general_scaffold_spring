package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Schema(description = "批量角色权限分配DTO")
@Data
public class BatchRolePermissionDTO {

    @NotEmpty(message = "权限列表不能为空")
    @Schema(description = "权限列表")
    private List<RolePermissionDTO> permissions;
}
