package com.scaffold.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.admin.common.R;
import com.scaffold.admin.model.entity.AdminFile;
import com.scaffold.admin.model.vo.FileUploadVO;
import com.scaffold.admin.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
@Tag(name = "files", description = "文件中心")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(operationId = "uploadFile", summary = "通用文件上传", description = "上传文件到MinIO并记录到数据库（≤50MB）")
    public R<FileUploadVO> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "category", required = false) String category
    ) {
        return R.ok(fileService.uploadFile(file, category));
    }

    @PostMapping("/upload/avatar")
    @Operation(operationId = "uploadAvatar", summary = "头像上传", description = "上传头像图片（≤2MB，仅jpg/png/gif/webp）")
    public R<FileUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return R.ok(fileService.uploadAvatar(file));
    }

    @GetMapping
    @Operation(operationId = "listFiles", summary = "文件列表", description = "分页查询文件记录")
    public R<Page<AdminFile>> list(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "20") Integer pageSize,
        @RequestParam(required = false) String bucket,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword
    ) {
        return R.ok(fileService.listFiles(pageNum, pageSize, bucket, category, status, keyword));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(operationId = "getFileDetail", summary = "文件详情", description = "获取单条文件记录详情")
    public R<AdminFile> getDetail(@PathVariable("id") Long id) {
        return R.ok(fileService.getById(id));
    }

    @PutMapping("/{id:\\d+}/recycle")
    @Operation(operationId = "recycleFile", summary = "移入回收站", description = "将文件移入回收站")
    public R<Void> recycle(@PathVariable("id") Long id) {
        fileService.recycleFile(id);
        return R.ok();
    }

    @PutMapping("/{id:\\d+}/restore")
    @Operation(operationId = "restoreFile", summary = "恢复文件", description = "从回收站恢复文件")
    public R<Void> restore(@PathVariable("id") Long id) {
        fileService.restoreFile(id);
        return R.ok();
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(operationId = "deleteFilePermanently", summary = "彻底删除", description = "彻底删除文件（MinIO+数据库）")
    public R<Void> deletePermanently(@PathVariable("id") Long id) {
        fileService.deleteFilePermanently(id);
        return R.ok();
    }

    @GetMapping("/recycle-bin")
    @Operation(operationId = "listRecycleBin", summary = "回收站列表", description = "分页查询回收站文件")
    public R<Page<AdminFile>> recycleBin(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        return R.ok(fileService.listRecycleBin(pageNum, pageSize));
    }

    @GetMapping("/buckets")
    @Operation(operationId = "listBuckets", summary = "桶列表", description = "列出所有MinIO桶")
    public R<List<String>> listBuckets() {
        return R.ok(fileService.listBuckets());
    }
}
