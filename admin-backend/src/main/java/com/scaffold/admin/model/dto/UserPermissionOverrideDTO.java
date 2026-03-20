package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户权限覆盖DTO")
@Data
public class UserPermissionOverrideDTO {

    @NotNull(message = "权限ID不能为空")
    @Schema(description = "权限ID")
    private Long permissionId;

    @NotNull(message = "效果不能为空")
    @Schema(description = "生效方式（GRANT/DENY）")
    private String effect;
}
