package com.scaffold.admin.service;

import com.scaffold.admin.model.dto.UpdateSystemConfigDTO;
import com.scaffold.admin.model.vo.SystemConfigGroupVO;
import com.scaffold.admin.model.vo.SystemConfigItemVO;

import java.util.List;

public interface SystemConfigService {

    /**
     * 查询所有配置（按分组）
     */
    List<SystemConfigGroupVO> listAllGrouped();

    /**
     * 获取公开配置（无需登录）
     */
    List<SystemConfigItemVO> getPublicConfigs();

    /**
     * 批量更新配置
     */
    void batchUpdate(UpdateSystemConfigDTO dto);

    /**
     * 获取单个配置值（带缓存）
     */
    String getConfigValue(String key);
}
