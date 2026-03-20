package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录请求参数")
public class LoginDTO {

    @Schema(description = "用户名", example = "admin")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", example = "admin123")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "验证码key（获取验证码时返回）", example = "a1b2c3d4e5f6")
    @NotBlank(message = "验证码key不能为空")
    private String captchaKey;

    @Schema(description = "验证码答案", example = "1234")
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
