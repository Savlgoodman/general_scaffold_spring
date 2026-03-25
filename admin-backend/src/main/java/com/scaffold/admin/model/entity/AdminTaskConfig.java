package com.scaffold.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "定时任务配置")
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("admin_task_config")
public class AdminTaskConfig extends BaseEntity {

    @Schema(description = "任务标识")
    private String taskName;

    @Schema(description = "任务显示名")
    private String taskLabel;

    @Schema(description = "任务分组")
    private String taskGroup;

    @Schema(description = "Cron表达式")
    private String cronExpression;

    @Schema(description = "是否启用：1-启用 0-停用")
    private Integer enabled;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "上次执行时间")
    private LocalDateTime lastRunTime;

    @Schema(description = "上次执行状态")
    private String lastRunStatus;
}
