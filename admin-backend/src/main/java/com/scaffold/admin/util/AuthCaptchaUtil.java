package com.scaffold.admin.util;

import cn.hutool.core.util.StrUtil;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import com.scaffold.admin.common.RedisKeys;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码工具类（内部使用）
 */
public class AuthCaptchaUtil {

    private static final long CAPTCHA_EXPIRATION = 5; // 5分钟
    private static final int DEFAULT_WIDTH = 130;
    private static final int DEFAULT_HEIGHT = 48;
    private static final int DEFAULT_LEN = 4;

    /**
     * 生成验证码（使用默认配置：PNG类型）
     * @return 验证码结果，包含key和Base64图片
     */
    public static CaptchaResult generate(RedisTemplate<String, Object> redisTemplate) {
        return generate(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_LEN, "png", redisTemplate);
    }

    /**
     * 生成验证码
     * @param width 图片宽度
     * @param height 图片高度
     * @param len 验证码长度
     * @param type 验证码类型：png/gif/arithmetic/chinese/chineseGif
     * @return 验证码结果
     */
    public static CaptchaResult generate(int width, int height, int len, String type,
                                         RedisTemplate<String, Object> redisTemplate) {
        Captcha captcha;

        // 根据类型创建验证码
        if ("gif".equalsIgnoreCase(type)) {
            captcha = new com.wf.captcha.GifCaptcha(width, height, len);
        } else if ("arithmetic".equalsIgnoreCase(type)) {
            captcha = new com.wf.captcha.ArithmeticCaptcha(width, height, len);
            if (len > 0) {
                ((com.wf.captcha.ArithmeticCaptcha) captcha).setLen(len);
            }
        } else if ("chinese".equalsIgnoreCase(type)) {
            captcha = new com.wf.captcha.ChineseCaptcha(width, height, len);
        } else if ("chineseGif".equalsIgnoreCase(type)) {
            captcha = new com.wf.captcha.ChineseGifCaptcha(width, height, len);
        } else {
            captcha = new SpecCaptcha(width, height, len);
        }

        // 生成验证码文本和图片
        String code = captcha.text();
        String base64Image = captcha.toBase64();

        // 生成唯一key
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        // 存入Redis
        String redisKey = RedisKeys.CAPTCHA.key(captchaKey);
        redisTemplate.opsForValue().set(redisKey, code, CAPTCHA_EXPIRATION, TimeUnit.MINUTES);

        return new CaptchaResult(captchaKey, base64Image, type != null ? type : "png");
    }

    /**
     * 验证验证码
     * @return true-验证成功（已删除） false-验证失败或已过期
     */
    public static boolean verify(String captchaKey, String captchaCode,
                                RedisTemplate<String, Object> redisTemplate) {
        if (StrUtil.isBlank(captchaKey) || StrUtil.isBlank(captchaCode)) {
            return false;
        }
        String redisKey = RedisKeys.CAPTCHA.key(captchaKey);
        Object cachedCode = redisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null) {
            return false;
        }
        // 校验成功后删除验证码
        boolean matches = cachedCode.toString().equalsIgnoreCase(captchaCode.trim());
        if (matches) {
            redisTemplate.delete(redisKey);
        }
        return matches;
    }

    /**
     * 删除验证码
     */
    public static void delete(String captchaKey, RedisTemplate<String, Object> redisTemplate) {
        if (StrUtil.isNotBlank(captchaKey)) {
            String redisKey = RedisKeys.CAPTCHA.key(captchaKey);
            redisTemplate.delete(redisKey);
        }
    }

    /**
     * 验证码结果
     */
    public static class CaptchaResult {
        private final String captchaKey;
        private final String captchaImage;
        private final String type;

        public CaptchaResult(String captchaKey, String captchaImage, String type) {
            this.captchaKey = captchaKey;
            this.captchaImage = captchaImage;
            this.type = type;
        }

        public String getCaptchaKey() {
            return captchaKey;
        }

        public String getCaptchaImage() {
            return captchaImage;
        }

        public String getType() {
            return type;
        }
    }
}
