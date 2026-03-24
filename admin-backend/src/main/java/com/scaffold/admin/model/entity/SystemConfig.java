package com.scaffold.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "系统配置")
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("admin_system_config")
public class SystemConfig extends BaseEntity {

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "配置说明")
    private String description;

    @Schema(description = "分组名")
    private String groupName;

    @Schema(description = "排序")
    private Integer sort;
}
