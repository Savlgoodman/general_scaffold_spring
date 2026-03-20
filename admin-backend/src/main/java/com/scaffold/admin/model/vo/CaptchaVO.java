package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "图形验证码响应")
public class CaptchaVO {

    @Schema(description = "验证码唯一标识key", example = "a1b2c3d4e5f6")
    private String captchaKey;

    @Schema(description = "验证码图片Base64编码", example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...")
    private String captchaImage;

    @Schema(description = "验证码类型", example = "png")
    private String type;
}
