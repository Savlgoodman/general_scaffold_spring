package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "每日登录趋势")
@Data
public class StatLoginTrendVO {

    @Schema(description = "日期，格式 MM-dd")
    private String date;

    @Schema(description = "成功登录次数")
    private Long successCount;

    @Schema(description = "失败登录次数")
    private Long failedCount;
}
