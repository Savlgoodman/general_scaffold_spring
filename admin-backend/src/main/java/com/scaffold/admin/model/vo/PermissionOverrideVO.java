package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "权限覆盖信息")
@Data
public class PermissionOverrideVO {

    @Schema(description = "覆盖ID")
    private Long overrideId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "权限ID")
    private Long permissionId;

    @Schema(description = "权限名称")
    private String permissionName;

    @Schema(description = "接口路径")
    private String path;

    @Schema(description = "HTTP方法")
    private String method;

    @Schema(description = "生效方式（GRANT/DENY）")
    private String effect;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
