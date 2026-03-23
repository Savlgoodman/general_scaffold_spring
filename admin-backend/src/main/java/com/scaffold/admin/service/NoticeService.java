package com.scaffold.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.admin.model.dto.CreateNoticeDTO;
import com.scaffold.admin.model.dto.UpdateNoticeDTO;
import com.scaffold.admin.model.entity.AdminNotice;

public interface NoticeService {

    Page<AdminNotice> getPage(Integer pageNum, Integer pageSize, String keyword, String type, String status);

    AdminNotice getById(Long id);

    AdminNotice create(CreateNoticeDTO dto);

    AdminNotice update(Long id, UpdateNoticeDTO dto);

    void delete(Long id);

    void publish(Long id);

    void withdraw(Long id);

    void toggleTop(Long id);
}
