package com.scaffold.admin.util;

import cn.hutool.core.util.StrUtil;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码工具类
 */
public class AuthCaptchaUtil {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final long CAPTCHA_EXPIRATION = 5; // 5分钟

    /**
     * 生成验证码
     * @param width 图片宽度
     * @param height 图片高度
     * @param len 验证码长度
     * @param redisTemplate Redis模板
     * @return 验证码结果，包含key和Base64图片
     */
    public static CaptchaResult generate(int width, int height, int len,
                                         RedisTemplate<String, Object> redisTemplate) {
        return generate(width, height, len, null, redisTemplate);
    }

    /**
     * 生成验证码
     * @param width 图片宽度
     * @param height 图片高度
     * @param len 验证码长度
     * @param type 验证码类型：png/gif/arithmetic/chinese/chineseGif
     * @param redisTemplate Redis模板
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
        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        redisTemplate.opsForValue().set(redisKey, code, CAPTCHA_EXPIRATION, TimeUnit.MINUTES);

        return new CaptchaResult(captchaKey, base64Image, type);
    }

    /**
     * 验证验证码
     */
    public static boolean verify(String captchaKey, String captchaCode,
                                RedisTemplate<String, Object> redisTemplate) {
        if (StrUtil.isBlank(captchaKey) || StrUtil.isBlank(captchaCode)) {
            return false;
        }
        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
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
            String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
            redisTemplate.delete(redisKey);
        }
    }

    /**
     * 验证码结果
     */
    public static class CaptchaResult {
        private String captchaKey;
        private String captchaImage;
        private String type;

        public CaptchaResult(String captchaKey, String captchaImage, String type) {
            this.captchaKey = captchaKey;
            this.captchaImage = captchaImage;
            this.type = type;
        }

        public String getCaptchaKey() {
            return captchaKey;
        }

        public void setCaptchaKey(String captchaKey) {
            this.captchaKey = captchaKey;
        }

        public String getCaptchaImage() {
            return captchaImage;
        }

        public void setCaptchaImage(String captchaImage) {
            this.captchaImage = captchaImage;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
