package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "系统配置分组")
@Data
public class SystemConfigGroupVO {

    @Schema(description = "分组名")
    private String groupName;

    @Schema(description = "配置项列表")
    private List<SystemConfigItemVO> items;
}
