package com.scaffold.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.scaffold.admin.common.RedisKeys;
import com.scaffold.admin.mapper.*;
import com.scaffold.admin.model.entity.*;
import com.scaffold.admin.model.vo.*;
import com.scaffold.admin.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AdminLoginLogMapper adminLoginLogMapper;
    private final AdminApiLogMapper adminApiLogMapper;
    private final AdminErrorLogMapper adminErrorLogMapper;
    private final AdminNoticeMapper adminNoticeMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    @Override
    public StatOverviewVO getOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        StatOverviewVO vo = new StatOverviewVO();
        vo.setTotalUsers(adminUserMapper.selectCount(new LambdaQueryWrapper<>()));
        vo.setActiveUsers(adminUserMapper.selectCount(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getStatus, 1)));
        vo.setTotalRoles(adminRoleMapper.selectCount(new LambdaQueryWrapper<>()));

        vo.setTodayLoginSuccess(adminLoginLogMapper.selectCount(
                new LambdaQueryWrapper<AdminLoginLog>()
                        .eq(AdminLoginLog::getStatus, "success")
                        .ge(AdminLoginLog::getCreateTime, todayStart)));
        vo.setTodayLoginFailed(adminLoginLogMapper.selectCount(
                new LambdaQueryWrapper<AdminLoginLog>()
                        .eq(AdminLoginLog::getStatus, "failed")
                        .ge(AdminLoginLog::getCreateTime, todayStart)));

        // 在线用户数：扫描 Redis
        Set<String> onlineKeys = redisTemplate.keys(RedisKeys.ONLINE_SESSION.key("*"));
        vo.setOnlineUsers(onlineKeys != null ? onlineKeys.size() : 0);

        vo.setTodayErrors(adminErrorLogMapper.selectCount(
                new LambdaQueryWrapper<AdminErrorLog>()
                        .ge(AdminErrorLog::getCreateTime, todayStart)));

        vo.setPublishedNotices(adminNoticeMapper.selectCount(
                new LambdaQueryWrapper<AdminNotice>()
                        .eq(AdminNotice::getStatus, "published")));

        return vo;
    }

    @Override
    public List<StatLoginTrendVO> getLoginTrend() {
        List<StatLoginTrendVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            StatLoginTrendVO vo = new StatLoginTrendVO();
            vo.setDate(date.format(DATE_FORMAT));

            vo.setSuccessCount(adminLoginLogMapper.selectCount(
                    new LambdaQueryWrapper<AdminLoginLog>()
                            .eq(AdminLoginLog::getStatus, "success")
                            .ge(AdminLoginLog::getCreateTime, dayStart)
                            .lt(AdminLoginLog::getCreateTime, dayEnd)));

            vo.setFailedCount(adminLoginLogMapper.selectCount(
                    new LambdaQueryWrapper<AdminLoginLog>()
                            .eq(AdminLoginLog::getStatus, "failed")
                            .ge(AdminLoginLog::getCreateTime, dayStart)
                            .lt(AdminLoginLog::getCreateTime, dayEnd)));

            result.add(vo);
        }
        return result;
    }

    @Override
    public StatApiStatsVO getApiStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        StatApiStatsVO vo = new StatApiStatsVO();

        // 今日请求总数
        long todayRequests = adminApiLogMapper.selectCount(
                new LambdaQueryWrapper<AdminApiLog>()
                        .ge(AdminApiLog::getCreateTime, todayStart));
        vo.setTodayRequests(todayRequests);

        if (todayRequests == 0) {
            vo.setAvgResponseTime(0.0);
            vo.setErrorRate(0.0);
            vo.setSlowEndpoints(List.of());
            return vo;
        }

        // 平均响应时间
        QueryWrapper<AdminApiLog> avgWrapper = new QueryWrapper<>();
        avgWrapper.select("AVG(duration_ms) as duration_ms")
                .ge("create_time", todayStart);
        List<Map<String, Object>> avgResult = adminApiLogMapper.selectMaps(avgWrapper);
        if (!avgResult.isEmpty() && avgResult.get(0).get("duration_ms") != null) {
            vo.setAvgResponseTime(((Number) avgResult.get(0).get("duration_ms")).doubleValue());
        } else {
            vo.setAvgResponseTime(0.0);
        }

        // 错误率
        long errorCount = adminApiLogMapper.selectCount(
                new LambdaQueryWrapper<AdminApiLog>()
                        .ge(AdminApiLog::getCreateTime, todayStart)
                        .ge(AdminApiLog::getResponseCode, 400));
        vo.setErrorRate(Math.round(errorCount * 10000.0 / todayRequests) / 100.0);

        // 慢接口 Top5
        QueryWrapper<AdminApiLog> slowWrapper = new QueryWrapper<>();
        slowWrapper.select("method", "path", "AVG(duration_ms) as avg_duration", "COUNT(*) as cnt")
                .ge("create_time", todayStart)
                .groupBy("method", "path")
                .orderByDesc("avg_duration")
                .last("LIMIT 5");
        List<Map<String, Object>> slowResult = adminApiLogMapper.selectMaps(slowWrapper);
        List<StatApiStatsVO.StatApiStatsSlowEndpoint> slowEndpoints = slowResult.stream()
                .map(row -> {
                    StatApiStatsVO.StatApiStatsSlowEndpoint endpoint = new StatApiStatsVO.StatApiStatsSlowEndpoint();
                    endpoint.setMethod((String) row.get("method"));
                    endpoint.setPath((String) row.get("path"));
                    endpoint.setAvgDuration(((Number) row.get("avg_duration")).doubleValue());
                    endpoint.setCount(((Number) row.get("cnt")).longValue());
                    return endpoint;
                })
                .collect(Collectors.toList());
        vo.setSlowEndpoints(slowEndpoints);

        return vo;
    }

    @Override
    public List<StatRecentLoginVO> getRecentLogins(int limit) {
        List<AdminLoginLog> logs = adminLoginLogMapper.selectList(
                new LambdaQueryWrapper<AdminLoginLog>()
                        .orderByDesc(AdminLoginLog::getCreateTime)
                        .last("LIMIT " + limit));

        return logs.stream().map(log -> {
            StatRecentLoginVO vo = new StatRecentLoginVO();
            vo.setUsername(log.getUsername());
            vo.setStatus(log.getStatus());
            vo.setIp(log.getIp());
            vo.setCreateTime(log.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<StatErrorTrendVO> getErrorTrend() {
        List<StatErrorTrendVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            StatErrorTrendVO vo = new StatErrorTrendVO();
            vo.setDate(date.format(DATE_FORMAT));
            vo.setCount(adminErrorLogMapper.selectCount(
                    new LambdaQueryWrapper<AdminErrorLog>()
                            .ge(AdminErrorLog::getCreateTime, dayStart)
                            .lt(AdminErrorLog::getCreateTime, dayEnd)));
            result.add(vo);
        }
        return result;
    }
}
