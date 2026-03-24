package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "批量更新系统配置")
@Data
public class UpdateSystemConfigDTO {

    @Schema(description = "配置项列表")
    @NotEmpty(message = "配置项列表不能为空")
    @Valid
    private List<UpdateSystemConfigEntry> configs;

    @Schema(description = "单条配置更新项")
    @Data
    public static class UpdateSystemConfigEntry {

        @Schema(description = "配置键", example = "site_title")
        @NotBlank(message = "配置键不能为空")
        private String configKey;

        @Schema(description = "配置值", example = "My Admin Platform")
        private String configValue;
    }
}
