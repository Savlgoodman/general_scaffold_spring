package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "刷新Token请求参数")
public class RefreshTokenDTO {

    @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    @NotBlank(message = "Refresh Token不能为空")
    private String refreshToken;
}
