-- V10__add_system_config.sql
-- 系统配置表（KV 模式）

CREATE TABLE admin_system_config (
    id           BIGSERIAL    PRIMARY KEY,
    config_key   VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    description  VARCHAR(255),
    group_name   VARCHAR(50)  NOT NULL DEFAULT 'basic',
    sort         INTEGER      DEFAULT 0,
    is_deleted   INTEGER      DEFAULT 0,
    create_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  admin_system_config              IS '系统配置表';
COMMENT ON COLUMN admin_system_config.config_key   IS '配置键，全局唯一';
COMMENT ON COLUMN admin_system_config.config_value IS '配置值，支持纯文本或 JSON';
COMMENT ON COLUMN admin_system_config.group_name   IS '配置分组：basic / security / appearance';

-- 基础设置
INSERT INTO admin_system_config (config_key, config_value, description, group_name, sort) VALUES
('site_title',    'Admin Platform', '浏览器标签页标题',          'basic', 1),
('site_name',     'Admin',          '侧边栏 Header 主标题',     'basic', 2),
('site_subtitle', '管理系统',        '侧边栏 Header 副标题',     'basic', 3),
('site_logo',     '',               '站点 Logo URL',            'basic', 4),
('site_favicon',  '',               '浏览器 Favicon URL',       'basic', 5),
('site_footer',   '© 2026 Admin Platform', '页脚版权文字',      'basic', 6);

-- 安全设置
INSERT INTO admin_system_config (config_key, config_value, description, group_name, sort) VALUES
('login_captcha_enabled', 'true',  '登录是否需要验证码',      'security', 1),
('login_max_retry',       '5',     '登录最大失败次数',        'security', 2),
('login_lock_duration',   '30',    '账号锁定时长（分钟）',    'security', 3),
('password_min_length',   '6',     '密码最小长度',            'security', 4),
('session_timeout',       '30',    '会话超时时间（分钟）',    'security', 5);

-- 外观设置
INSERT INTO admin_system_config (config_key, config_value, description, group_name, sort) VALUES
('default_theme',      'system',  '新用户默认主题',          'appearance', 1),
('sidebar_collapsed',  'false',   '侧边栏默认是否收起',      'appearance', 2),
('login_bg_image',     '',        '登录页背景图 URL',        'appearance', 3),
('login_welcome_text', '欢迎回来', '登录页欢迎文字',         'appearance', 4);
