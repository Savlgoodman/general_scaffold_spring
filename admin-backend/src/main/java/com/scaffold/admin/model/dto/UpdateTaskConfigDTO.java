package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "修改定时任务配置")
@Data
public class UpdateTaskConfigDTO {

    @Schema(description = "Cron表达式")
    private String cronExpression;

    @Schema(description = "是否启用：1-启用 0-停用")
    private Integer enabled;

    @Schema(description = "任务描述")
    private String description;
}
