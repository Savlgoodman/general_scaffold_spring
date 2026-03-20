package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Schema(description = "分配用户角色DTO")
@Data
public class AssignRolesDTO {

    @NotEmpty(message = "角色ID列表不能为空")
    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}
