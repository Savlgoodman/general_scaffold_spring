package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "最近登录记录")
@Data
public class StatRecentLoginVO {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "登录状态")
    private String status;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "登录时间")
    private LocalDateTime createTime;
}
