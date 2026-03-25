package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "API请求统计")
@Data
public class StatApiStatsVO {

    @Schema(description = "今日请求总数")
    private Long todayRequests;

    @Schema(description = "平均响应时间(ms)")
    private Double avgResponseTime;

    @Schema(description = "错误率（百分比，responseCode >= 400）")
    private Double errorRate;

    @Schema(description = "慢接口Top5")
    private List<StatApiStatsSlowEndpoint> slowEndpoints;

    @Schema(description = "慢接口条目")
    @Data
    public static class StatApiStatsSlowEndpoint {

        @Schema(description = "请求方法")
        private String method;

        @Schema(description = "请求路径")
        private String path;

        @Schema(description = "平均耗时(ms)")
        private Double avgDuration;

        @Schema(description = "请求次数")
        private Long count;
    }
}
