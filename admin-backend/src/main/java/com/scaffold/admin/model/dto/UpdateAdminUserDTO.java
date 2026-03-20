package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "更新用户DTO")
@Data
public class UpdateAdminUserDTO {

    @Schema(description = "昵称", example = "管理员")
    private String nickname;

    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "是否超级管理员（1-是 0-否）")
    private Integer isSuperuser;

    @Schema(description = "状态（1-正常 0-禁用）")
    private Integer status;

    @Schema(description = "密码（可选，不修改则留空）")
    private String password;
}
