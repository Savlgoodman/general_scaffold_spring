package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "创建通知公告")
@Data
public class CreateNoticeDTO {

    @Schema(description = "公告标题", example = "系统维护通知")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "公告内容（通知类型选填，公告类型必填）", example = "系统将于今晚22:00进行维护升级...")
    private String content;

    @Schema(description = "公告类型（notice-公告 announcement-通告）", example = "notice")
    private String type;
}
