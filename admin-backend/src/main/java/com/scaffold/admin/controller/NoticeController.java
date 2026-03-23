package com.scaffold.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.admin.common.R;
import com.scaffold.admin.common.ResultCode;
import com.scaffold.admin.model.dto.CreateNoticeDTO;
import com.scaffold.admin.model.dto.UpdateNoticeDTO;
import com.scaffold.admin.model.entity.AdminNotice;
import com.scaffold.admin.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
@Tag(name = "notices", description = "通知公告管理")
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    @Operation(operationId = "listNotices", summary = "公告列表", description = "分页查询通知公告")
    public R<Page<AdminNotice>> list(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String status
    ) {
        return R.ok(noticeService.getPage(pageNum, pageSize, keyword, type, status));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(operationId = "getNoticeDetail", summary = "公告详情", description = "获取单条公告详情")
    public R<AdminNotice> getDetail(@PathVariable("id") Long id) {
        AdminNotice notice = noticeService.getById(id);
        if (notice == null) {
            return R.error(ResultCode.NOT_FOUND, "公告不存在");
        }
        return R.ok(notice);
    }

    @PostMapping
    @Operation(operationId = "createNotice", summary = "创建公告", description = "创建新公告（默认草稿状态）")
    public R<AdminNotice> create(@RequestBody @Valid CreateNoticeDTO dto) {
        return R.ok(noticeService.create(dto));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(operationId = "updateNotice", summary = "编辑公告", description = "编辑公告（仅草稿状态）")
    public R<AdminNotice> update(@PathVariable("id") Long id, @RequestBody @Valid UpdateNoticeDTO dto) {
        return R.ok(noticeService.update(id, dto));
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(operationId = "deleteNotice", summary = "删除公告", description = "删除公告")
    public R<Void> delete(@PathVariable("id") Long id) {
        noticeService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id:\\d+}/publish")
    @Operation(operationId = "publishNotice", summary = "发布公告", description = "将草稿公告发布")
    public R<Void> publish(@PathVariable("id") Long id) {
        noticeService.publish(id);
        return R.ok();
    }

    @PutMapping("/{id:\\d+}/withdraw")
    @Operation(operationId = "withdrawNotice", summary = "撤回公告", description = "撤回已发布的公告")
    public R<Void> withdraw(@PathVariable("id") Long id) {
        noticeService.withdraw(id);
        return R.ok();
    }

    @PutMapping("/{id:\\d+}/top")
    @Operation(operationId = "toggleNoticeTop", summary = "切换置顶", description = "切换公告置顶状态")
    public R<Void> toggleTop(@PathVariable("id") Long id) {
        noticeService.toggleTop(id);
        return R.ok();
    }
}
