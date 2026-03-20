package com.scaffold.admin.service;

import com.scaffold.admin.model.entity.AdminUser;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 管理员用户服务接口
 */
public interface AdminUserService {

    /**
     * 根据用户名加载用户详情
     */
    UserDetails loadUserByUsername(String username);

    /**
     * 根据用户ID加载用户详情
     */
    UserDetails loadUserById(Long id);

    /**
     * 根据用户ID获取用户实体
     */
    AdminUser getUserById(Long id);
}
