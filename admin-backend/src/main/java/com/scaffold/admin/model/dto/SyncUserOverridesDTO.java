package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "同步用户权限覆盖DTO")
@Data
public class SyncUserOverridesDTO {

    @NotEmpty(message = "覆盖列表不能为空")
    @Valid
    @Schema(description = "覆盖列表")
    private List<Item> overrides;

    @Schema(description = "覆盖项")
    @Data
    public static class Item {

        @NotNull(message = "权限ID不能为空")
        @Schema(description = "权限ID")
        private Long permissionId;

        @NotBlank(message = "效果不能为空")
        @Schema(description = "效果: GRANT/DENY")
        private String effect;
    }
}
