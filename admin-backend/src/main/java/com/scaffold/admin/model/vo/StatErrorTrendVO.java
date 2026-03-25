package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "每日错误趋势")
@Data
public class StatErrorTrendVO {

    @Schema(description = "日期，格式 MM-dd")
    private String date;

    @Schema(description = "错误数量")
    private Long count;
}
