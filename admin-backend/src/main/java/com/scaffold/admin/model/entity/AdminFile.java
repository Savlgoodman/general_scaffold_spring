package com.scaffold.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "文件记录")
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("admin_file")
public class AdminFile extends BaseEntity {

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "MinIO对象路径")
    private String objectName;

    @Schema(description = "桶名")
    private String bucketName;

    @Schema(description = "访问URL")
    private String url;

    @Schema(description = "文件大小（字节）")
    private Long size;

    @Schema(description = "MIME类型")
    private String contentType;

    @Schema(description = "分类：avatar/general/document/image")
    private String category;

    @Schema(description = "上传者ID")
    private Long uploaderId;

    @Schema(description = "上传者用户名")
    private String uploaderName;

    @Schema(description = "状态：active/recycled/deleted")
    private String status;

    @Schema(description = "移入回收站时间")
    private LocalDateTime deletedAt;
}
