package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "更新角色DTO")
@Data
public class UpdateRoleDTO {

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态（1-正常 0-禁用）")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;
}
