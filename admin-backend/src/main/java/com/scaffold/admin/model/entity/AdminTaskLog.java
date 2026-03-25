package com.scaffold.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "定时任务执行日志")
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("admin_task_log")
public class AdminTaskLog extends BaseEntity {

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务分组")
    private String taskGroup;

    @Schema(description = "状态：success/failed/running")
    private String status;

    @Schema(description = "执行结果/错误信息")
    private String message;

    @Schema(description = "执行耗时（毫秒）")
    private Long durationMs;

    @Schema(description = "详细信息")
    private String detail;
}
