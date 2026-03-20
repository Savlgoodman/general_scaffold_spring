package com.scaffold.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.admin.common.BusinessException;
import com.scaffold.admin.common.ResultCode;
import com.scaffold.admin.mapper.AdminLoginLogMapper;
import com.scaffold.admin.mapper.AdminUserMapper;
import com.scaffold.admin.model.dto.LoginDTO;
import com.scaffold.admin.model.dto.RefreshTokenDTO;
import com.scaffold.admin.model.dto.RegisterDTO;
import com.scaffold.admin.model.entity.AdminLoginLog;
import com.scaffold.admin.model.entity.AdminUser;
import com.scaffold.admin.model.vo.LoginVO;
import com.scaffold.admin.model.vo.UserVO;
import com.scaffold.admin.security.AdminUserDetails;
import com.scaffold.admin.security.JwtTokenProvider;
import com.scaffold.admin.service.AuthService;
import com.scaffold.admin.util.AuthCaptchaUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final long LOCK_DURATION = 15; // 锁定15分钟

    private final AdminUserMapper adminUserMapper;
    private final AdminLoginLogMapper loginLogMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HttpServletRequest httpServletRequest;

    @Override
    @Transactional
    public LoginVO login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        String ip = getClientIp();
        String userAgent = httpServletRequest.getHeader("User-Agent");

        // 检查账户是否被锁定
        checkAccountLocked(username);

        // 验证图形验证码
        if (!AuthCaptchaUtil.verify(loginDTO.getCaptchaKey(), loginDTO.getCaptchaCode(), redisTemplate)) {
            recordLoginLog(username, "failed", ip, userAgent, "验证码错误");
            incrementLoginFailCount(username);
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误");
        }

        try {
            // 验证用户名密码
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginDTO.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取认证用户信息
            AdminUserDetails userDetails = (AdminUserDetails) authentication.getPrincipal();
            AdminUser user = adminUserMapper.selectById(userDetails.getId());

            // 检查用户状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                recordLoginLog(username, "failed", ip, userAgent, "账户已被禁用");
                throw new BusinessException(ResultCode.ACCOUNT_LOCKED, "账户已被禁用");
            }

            // 清除登录失败计数
            clearLoginFailCount(username);

            // 生成Token
            String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

            // 记录登录日志
            recordLoginLog(username, "success", ip, userAgent, "登录成功");

            // 构建响应
            return buildLoginVO(user, accessToken, refreshToken);
        } catch (BadCredentialsException e) {
            recordLoginLog(username, "failed", ip, userAgent, "密码错误");
            incrementLoginFailCount(username);
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        } catch (LockedException e) {
            recordLoginLog(username, "locked", ip, userAgent, "账户已被锁定");
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED, "账户已被锁定");
        }
    }

    @Override
    @Transactional
    public LoginVO register(RegisterDTO registerDTO) {
        // 检查用户名是否已存在
        AdminUser existUser = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, registerDTO.getUsername())
                        .eq(AdminUser::getIsDeleted, 0)
        );
        if (existUser != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名已存在");
        }

        // 创建新用户
        AdminUser user = new AdminUser();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname());
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setStatus(1); // 默认启用

        adminUserMapper.insert(user);

        // 生成Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        // 记录登录日志
        String ip = getClientIp();
        String userAgent = httpServletRequest.getHeader("User-Agent");
        recordLoginLog(user.getUsername(), "success", ip, userAgent, "注册成功");

        return buildLoginVO(user, accessToken, refreshToken);
    }

    @Override
    public LoginVO refreshToken(RefreshTokenDTO refreshTokenDTO) {
        String refreshToken = refreshTokenDTO.getRefreshToken();

        // 验证Refresh Token
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的Refresh Token");
        }

        // 检查是否在黑名单
        if (jwtTokenProvider.isInBlacklist(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Refresh Token已失效");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的Refresh Token");
        }

        // 获取用户信息
        AdminUser user = adminUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED, "账户已被禁用");
        }

        // 将旧的Refresh Token加入黑名单
        jwtTokenProvider.addToBlacklist(refreshToken);

        // 生成新Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        return buildLoginVO(user, newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(String accessToken) {
        if (StrUtil.isNotBlank(accessToken) && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
        if (StrUtil.isNotBlank(accessToken)) {
            // 将Access Token和Refresh Token都加入黑名单
            jwtTokenProvider.addToBlacklist(accessToken);
            // 注意：Refresh Token需要单独处理，这里简化处理，实际可以存储用户的refreshToken来精确注销
            log.debug("用户登出，Token已加入黑名单");
        }
    }

    /**
     * 检查账户是否被锁定
     */
    private void checkAccountLocked(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        Object failCount = redisTemplate.opsForValue().get(key);
        if (failCount != null) {
            int count = Integer.parseInt(failCount.toString());
            if (count >= MAX_LOGIN_FAIL_COUNT) {
                throw new BusinessException(ResultCode.ACCOUNT_LOCKED,
                        "登录失败次数过多，账户已被锁定" + LOCK_DURATION + "分钟");
            }
        }
    }

    /**
     * 增加登录失败计数
     */
    private void incrementLoginFailCount(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 首次失败，设置过期时间
            redisTemplate.expire(key, LOCK_DURATION, TimeUnit.MINUTES);
        }
        log.debug("用户 {} 登录失败次数: {}", username, count);
    }

    /**
     * 清除登录失败计数
     */
    private void clearLoginFailCount(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        redisTemplate.delete(key);
    }

    /**
     * 记录登录日志
     */
    private void recordLoginLog(String username, String status, String ip, String userAgent, String message) {
        try {
            AdminLoginLog loginLog = new AdminLoginLog();
            loginLog.setUsername(username);
            loginLog.setStatus(status);
            loginLog.setIp(ip);
            loginLog.setUserAgent(userAgent);
            loginLog.setMessage(message);
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        String ip = httpServletRequest.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getRemoteAddr();
        }
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 构建登录响应
     */
    private LoginVO buildLoginVO(AdminUser user, String accessToken, String refreshToken) {
        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setTokenType("Bearer");
        vo.setExpiresIn(jwtTokenProvider.getAccessTokenExpiration());

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        vo.setUser(userVO);

        return vo;
    }
}
