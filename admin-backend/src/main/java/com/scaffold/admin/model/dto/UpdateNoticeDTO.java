package com.scaffold.admin.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "更新通知公告")
@Data
public class UpdateNoticeDTO {

    @Schema(description = "公告标题")
    private String title;

    @Schema(description = "公告内容")
    private String content;

    @Schema(description = "公告类型（notice-公告 announcement-通告）")
    private String type;
}
