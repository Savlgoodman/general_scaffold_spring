package com.scaffold.admin.controller;

import com.scaffold.admin.common.R;
import com.scaffold.admin.model.dto.UpdateSystemConfigDTO;
import com.scaffold.admin.model.vo.SystemConfigGroupVO;
import com.scaffold.admin.model.vo.SystemConfigItemVO;
import com.scaffold.admin.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/system-config")
@RequiredArgsConstructor
@Tag(name = "system-config", description = "系统配置管理")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    @Operation(operationId = "listSystemConfigs", summary = "查询所有配置", description = "按分组返回所有系统配置项")
    public R<List<SystemConfigGroupVO>> list() {
        return R.ok(systemConfigService.listAllGrouped());
    }

    @PutMapping
    @Operation(operationId = "updateSystemConfigs", summary = "批量更新配置", description = "批量更新系统配置项")
    public R<Void> update(@RequestBody @Valid UpdateSystemConfigDTO dto) {
        systemConfigService.batchUpdate(dto);
        return R.ok();
    }

    @GetMapping("/public")
    @Operation(operationId = "getPublicConfigs", summary = "获取公开配置", description = "无需登录，返回前端渲染所需的公开配置")
    public R<List<SystemConfigItemVO>> getPublic() {
        return R.ok(systemConfigService.getPublicConfigs());
    }
}
