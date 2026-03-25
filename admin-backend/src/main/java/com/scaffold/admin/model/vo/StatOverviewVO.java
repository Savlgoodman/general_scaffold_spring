package com.scaffold.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "仪表盘概览统计")
@Data
public class StatOverviewVO {

    @Schema(description = "用户总数")
    private Long totalUsers;

    @Schema(description = "活跃用户数（status=1）")
    private Long activeUsers;

    @Schema(description = "角色总数")
    private Long totalRoles;

    @Schema(description = "今日登录成功数")
    private Long todayLoginSuccess;

    @Schema(description = "今日登录失败数")
    private Long todayLoginFailed;

    @Schema(description = "当前在线用户数")
    private Integer onlineUsers;

    @Schema(description = "今日错误数")
    private Long todayErrors;

    @Schema(description = "已发布公告数")
    private Long publishedNotices;
}
