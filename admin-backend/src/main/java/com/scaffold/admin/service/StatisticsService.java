package com.scaffold.admin.service;

import com.scaffold.admin.model.vo.*;

import java.util.List;

/**
 * 仪表盘统计服务接口
 */
public interface StatisticsService {

    /**
     * 概览统计
     */
    StatOverviewVO getOverview();

    /**
     * 最近7天登录趋势
     */
    List<StatLoginTrendVO> getLoginTrend();

    /**
     * 今日API请求统计
     */
    StatApiStatsVO getApiStats();

    /**
     * 最近登录记录
     */
    List<StatRecentLoginVO> getRecentLogins(int limit);

    /**
     * 最近7天错误趋势
     */
    List<StatErrorTrendVO> getErrorTrend();
}
