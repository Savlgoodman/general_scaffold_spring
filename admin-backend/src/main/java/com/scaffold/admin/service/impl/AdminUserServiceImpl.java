package com.scaffold.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.admin.mapper.AdminUserMapper;
import com.scaffold.admin.model.entity.AdminUser;
import com.scaffold.admin.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

/**
 * 管理员用户服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserMapper adminUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser user = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, username)
                        .eq(AdminUser::getIsDeleted, 0)
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        return new AdminUserDetails(user, Collections.emptyList());
    }

    @Override
    public UserDetails loadUserById(Long id) {
        AdminUser user = adminUserMapper.selectById(id);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在, ID: " + id);
        }
        return new AdminUserDetails(user, Collections.emptyList());
    }

    @Override
    public AdminUser getUserById(Long id) {
        return adminUserMapper.selectById(id);
    }

    /**
     * 管理员用户详情
     */
    public static class AdminUserDetails implements UserDetails {

        private final Long id;
        private final String username;
        private final String password;
        private final String nickname;
        private final Integer status;
        private final Collection<? extends GrantedAuthority> authorities;

        public AdminUserDetails(AdminUser user, Collection<? extends GrantedAuthority> authorities) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.password = user.getPassword();
            this.nickname = user.getNickname();
            this.status = user.getStatus();
            this.authorities = authorities;
        }

        public Long getId() {
            return id;
        }

        public String getNickname() {
            return nickname;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return status != null && status == 1;
        }
    }
}
