package com.scaffold.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.admin.common.R;
import com.scaffold.admin.mapper.AdminTaskConfigMapper;
import com.scaffold.admin.mapper.AdminTaskLogMapper;
import com.scaffold.admin.model.dto.UpdateTaskConfigDTO;
import com.scaffold.admin.model.entity.AdminTaskConfig;
import com.scaffold.admin.model.entity.AdminTaskLog;
import com.scaffold.admin.service.TaskExecutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
@Tag(name = "tasks", description = "调度中心")
public class TaskController {

    private final AdminTaskConfigMapper taskConfigMapper;
    private final AdminTaskLogMapper taskLogMapper;
    private final TaskExecutorService taskExecutorService;

    @GetMapping("/configs")
    @Operation(operationId = "listTaskConfigs", summary = "任务配置列表", description = "获取所有定时任务配置")
    public R<List<AdminTaskConfig>> listConfigs() {
        return R.ok(taskConfigMapper.selectList(
            new LambdaQueryWrapper<AdminTaskConfig>().orderByAsc(AdminTaskConfig::getId)
        ));
    }

    @PutMapping("/configs/{id:\\d+}")
    @Operation(operationId = "updateTaskConfig", summary = "修改任务配置", description = "修改Cron表达式或启用状态（实时生效）")
    public R<Void> updateConfig(@PathVariable("id") Long id, @RequestBody @Valid UpdateTaskConfigDTO dto) {
        AdminTaskConfig config = taskConfigMapper.selectById(id);
        if (config == null) return R.error(404, "任务不存在");
        if (dto.getCronExpression() != null) config.setCronExpression(dto.getCronExpression());
        if (dto.getEnabled() != null) config.setEnabled(dto.getEnabled());
        if (dto.getDescription() != null) config.setDescription(dto.getDescription());
        taskConfigMapper.updateById(config);
        return R.ok();
    }

    @PostMapping("/{taskName}/run")
    @Operation(operationId = "runTaskManually", summary = "手动触发任务", description = "立即执行指定任务")
    public R<Void> runManually(@PathVariable("taskName") String taskName) {
        taskExecutorService.execute(taskName);
        return R.ok();
    }

    @GetMapping("/logs")
    @Operation(operationId = "listTaskLogs", summary = "任务执行日志", description = "分页查询任务执行日志")
    public R<Page<AdminTaskLog>> listLogs(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "20") Integer pageSize,
        @RequestParam(required = false) String taskName,
        @RequestParam(required = false) String status
    ) {
        Page<AdminTaskLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AdminTaskLog> query = new LambdaQueryWrapper<>();
        if (taskName != null && !taskName.isBlank()) query.eq(AdminTaskLog::getTaskName, taskName);
        if (status != null && !status.isBlank()) query.eq(AdminTaskLog::getStatus, status);
        query.orderByDesc(AdminTaskLog::getCreateTime);
        return R.ok(taskLogMapper.selectPage(page, query));
    }

    @GetMapping("/logs/{id:\\d+}")
    @Operation(operationId = "getTaskLogDetail", summary = "任务日志详情", description = "获取单条任务执行日志详情")
    public R<AdminTaskLog> getLogDetail(@PathVariable("id") Long id) {
        return R.ok(taskLogMapper.selectById(id));
    }
}
