package com.scaffold.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.admin.annotation.OperationLog;
import com.scaffold.admin.common.BusinessException;
import com.scaffold.admin.common.RedisKeys;
import com.scaffold.admin.common.ResultCode;
import com.scaffold.admin.model.dto.UpdateSystemConfigDTO;
import com.scaffold.admin.model.entity.SystemConfig;
import com.scaffold.admin.model.enums.OperationType;
import com.scaffold.admin.model.vo.SystemConfigGroupVO;
import com.scaffold.admin.model.vo.SystemConfigItemVO;
import com.scaffold.admin.mapper.SystemConfigMapper;
import com.scaffold.admin.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 公开配置白名单
     */
    private static final Set<String> PUBLIC_KEYS = Set.of(
        "site_title", "site_name", "site_subtitle", "site_logo", "site_favicon",
        "site_footer", "login_bg_image", "login_welcome_text", "default_theme",
        "login_captcha_enabled"
    );

    private static final String PUBLIC_BUNDLE_KEY = RedisKeys.SYSTEM_CONFIG.key("public_bundle");
    private static final long CACHE_TTL_HOURS = 1;

    @Override
    public List<SystemConfigGroupVO> listAllGrouped() {
        LambdaQueryWrapper<SystemConfig> query = new LambdaQueryWrapper<>();
        query.orderByAsc(SystemConfig::getGroupName)
             .orderByAsc(SystemConfig::getSort);
        List<SystemConfig> all = systemConfigMapper.selectList(query);

        Map<String, SystemConfigGroupVO> groupMap = new LinkedHashMap<>();
        for (SystemConfig config : all) {
            SystemConfigGroupVO group = groupMap.computeIfAbsent(config.getGroupName(), k -> {
                SystemConfigGroupVO g = new SystemConfigGroupVO();
                g.setGroupName(k);
                g.setItems(new ArrayList<>());
                return g;
            });

            SystemConfigItemVO item = new SystemConfigItemVO();
            item.setConfigKey(config.getConfigKey());
            item.setConfigValue(config.getConfigValue());
            item.setDescription(config.getDescription());
            group.getItems().add(item);
        }

        return new ArrayList<>(groupMap.values());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SystemConfigItemVO> getPublicConfigs() {
        // 尝试从缓存读取
        Object cached = redisTemplate.opsForValue().get(PUBLIC_BUNDLE_KEY);
        if (cached instanceof List<?>) {
            return (List<SystemConfigItemVO>) cached;
        }

        LambdaQueryWrapper<SystemConfig> query = new LambdaQueryWrapper<>();
        query.in(SystemConfig::getConfigKey, PUBLIC_KEYS)
             .orderByAsc(SystemConfig::getSort);
        List<SystemConfig> configs = systemConfigMapper.selectList(query);

        List<SystemConfigItemVO> result = configs.stream().map(c -> {
            SystemConfigItemVO item = new SystemConfigItemVO();
            item.setConfigKey(c.getConfigKey());
            item.setConfigValue(c.getConfigValue());
            item.setDescription(c.getDescription());
            return item;
        }).toList();

        redisTemplate.opsForValue().set(PUBLIC_BUNDLE_KEY, result, CACHE_TTL_HOURS, TimeUnit.HOURS);
        return result;
    }

    @Override
    @Transactional
    @OperationLog(module = "系统设置", type = OperationType.UPDATE)
    public void batchUpdate(UpdateSystemConfigDTO dto) {
        for (UpdateSystemConfigDTO.UpdateSystemConfigEntry entry : dto.getConfigs()) {
            LambdaQueryWrapper<SystemConfig> query = new LambdaQueryWrapper<>();
            query.eq(SystemConfig::getConfigKey, entry.getConfigKey());
            SystemConfig existing = systemConfigMapper.selectOne(query);

            if (existing == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "配置项不存在: " + entry.getConfigKey());
            }

            existing.setConfigValue(entry.getConfigValue());
            systemConfigMapper.updateById(existing);

            // 清除单项缓存
            redisTemplate.delete(RedisKeys.SYSTEM_CONFIG.key(entry.getConfigKey()));
        }

        // 清除公开配置捆绑缓存
        redisTemplate.delete(PUBLIC_BUNDLE_KEY);
    }

    @Override
    public String getConfigValue(String key) {
        String cacheKey = RedisKeys.SYSTEM_CONFIG.key(key);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.toString();
        }

        LambdaQueryWrapper<SystemConfig> query = new LambdaQueryWrapper<>();
        query.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = systemConfigMapper.selectOne(query);
        if (config == null) {
            return null;
        }

        redisTemplate.opsForValue().set(cacheKey, config.getConfigValue(), CACHE_TTL_HOURS, TimeUnit.HOURS);
        return config.getConfigValue();
    }
}
