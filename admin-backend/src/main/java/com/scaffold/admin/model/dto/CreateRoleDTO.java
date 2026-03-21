package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "创建角色DTO")
@Data
public class CreateRoleDTO {

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称", example = "系统管理员")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    @Schema(description = "角色编码", example = "SYS_ADMIN")
    private String code;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态（1-正常 0-禁用）")
    private Integer status = 1;

    @Schema(description = "排序")
    private Integer sort = 0;
}
