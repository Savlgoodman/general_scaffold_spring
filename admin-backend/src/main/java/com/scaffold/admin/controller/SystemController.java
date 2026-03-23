package com.scaffold.admin.controller;

import com.scaffold.admin.common.R;
import com.scaffold.admin.model.vo.SystemInfoVO;
import com.scaffold.admin.service.SystemMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
@Tag(name = "system-monitor", description = "系统监控")
public class SystemController {

    private final SystemMonitorService systemMonitorService;

    @GetMapping("/monitor")
    @Operation(operationId = "getSystemInfo", summary = "系统监控信息", description = "获取CPU、内存、JVM、磁盘、Redis、数据库连接池等实时监控数据")
    public R<SystemInfoVO> getSystemInfo() {
        return R.ok(systemMonitorService.getSystemInfo());
    }
}
