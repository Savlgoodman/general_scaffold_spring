package com.scaffold.admin.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.admin.mapper.AdminApiLogMapper;
import com.scaffold.admin.model.entity.AdminApiLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 日志定期清理任务
 * 每天凌晨 3 点执行，删除超过保留天数的 API 日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanupTask {

    @Value("${app.log.api-log-retention-days:30}")
    private int apiLogRetentionDays;

    private final AdminApiLogMapper apiLogMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredApiLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(apiLogRetentionDays);
        int deleted = apiLogMapper.delete(
            new LambdaQueryWrapper<AdminApiLog>()
                .lt(AdminApiLog::getCreateTime, cutoff)
        );
        if (deleted > 0) {
            log.info("清理API日志: 删除 {} 天前的记录 {} 条", apiLogRetentionDays, deleted);
        }
    }
}
