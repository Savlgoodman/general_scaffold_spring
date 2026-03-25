package com.scaffold.admin.controller;

import com.scaffold.admin.common.R;
import com.scaffold.admin.model.vo.*;
import com.scaffold.admin.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@Tag(name = "statistics", description = "仪表盘统计")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    @Operation(operationId = "getStatOverview", summary = "概览统计", description = "获取用户数、角色数、今日登录、在线用户、错误数等概览数据")
    public R<StatOverviewVO> getOverview() {
        return R.ok(statisticsService.getOverview());
    }

    @GetMapping("/login-trend")
    @Operation(operationId = "getStatLoginTrend", summary = "登录趋势", description = "获取最近7天每日登录成功/失败次数")
    public R<List<StatLoginTrendVO>> getLoginTrend() {
        return R.ok(statisticsService.getLoginTrend());
    }

    @GetMapping("/api-stats")
    @Operation(operationId = "getStatApiStats", summary = "API请求统计", description = "获取今日请求总数、平均响应时间、错误率、慢接口Top5")
    public R<StatApiStatsVO> getApiStats() {
        return R.ok(statisticsService.getApiStats());
    }

    @GetMapping("/recent-logins")
    @Operation(operationId = "getStatRecentLogins", summary = "最近登录记录", description = "获取最近N条登录记录")
    public R<List<StatRecentLoginVO>> getRecentLogins(
            @RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(statisticsService.getRecentLogins(limit));
    }

    @GetMapping("/error-trend")
    @Operation(operationId = "getStatErrorTrend", summary = "错误趋势", description = "获取最近7天每日错误数量")
    public R<List<StatErrorTrendVO>> getErrorTrend() {
        return R.ok(statisticsService.getErrorTrend());
    }
}
