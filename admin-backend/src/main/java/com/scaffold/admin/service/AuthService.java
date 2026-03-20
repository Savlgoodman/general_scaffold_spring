package com.scaffold.admin.service;

import com.scaffold.admin.model.dto.LoginDTO;
import com.scaffold.admin.model.dto.RefreshTokenDTO;
import com.scaffold.admin.model.dto.RegisterDTO;
import com.scaffold.admin.model.vo.CaptchaVO;
import com.scaffold.admin.model.vo.LoginVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 生成图形验证码
     */
    CaptchaVO generateCaptcha();

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 用户注册
     */
    LoginVO register(RegisterDTO registerDTO);

    /**
     * 刷新Token
     */
    LoginVO refreshToken(RefreshTokenDTO refreshTokenDTO);

    /**
     * 登出
     */
    void logout(String accessToken);
}
