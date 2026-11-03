package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.dto.Views.MailConfig;
import com.btr.platform.dto.Views.SecurityConfig;
import com.btr.platform.dto.Views.SiteConfig;
import com.btr.platform.dto.Views.SwitchesConfig;
import com.btr.platform.dto.Views.TemplateConfig;
import com.btr.platform.dto.Views.UploadConfig;
import com.btr.platform.entity.SysConfig;
import com.btr.platform.mapper.SysConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 系统配置服务：KV 存储 + 默认值回退 + 敏感项加密/脱敏。
 * 管理后台在线维护，改完即时生效，无需重启。
 */
@Slf4j
@Service
public class ConfigService {

    /** 回显脱敏占位符 */
    public static final String MASKED = "******";

    public static final String KEY_MAIL_HOST = "mail.host";
    public static final String KEY_MAIL_PORT = "mail.port";
    public static final String KEY_MAIL_USERNAME = "mail.username";
    public static final String KEY_MAIL_PASSWORD = "mail.password";
    public static final String KEY_MAIL_FROM = "mail.from";
    public static final String KEY_MAIL_SSL = "mail.ssl";
    public static final String KEY_MAIL_ENABLED = "mail.enabled";
    public static final String KEY_SITE_NAME = "site.name";
    public static final String KEY_SITE_LOGO = "site.logo_url";
    public static final String KEY_SITE_FOOTER = "site.footer";
    public static final String KEY_SWITCH_REGISTER = "switch.register_enabled";
    public static final String KEY_SWITCH_CAPTCHA = "switch.captcha_enabled";
    public static final String KEY_TPL_SIGNUP = "template.signup_receipt";
    public static final String KEY_TPL_REVIEW = "template.review_reminder";
    public static final String KEY_TPL_WINNER = "template.winner_notice";
    public static final String KEY_TPL_VERIFY = "template.verify_code";
    public static final String KEY_UPLOAD_MAX_MB = "upload.max_size_mb";
    public static final String KEY_UPLOAD_TYPES = "upload.allowed_types";
    public static final String KEY_SEC_LOGIN_FAIL_LIMIT = "security.login_fail_limit";
    public static final String KEY_SEC_LOGIN_LOCK_MINUTES = "security.login_lock_minutes";

    private static final String DEFAULT_TPL_SIGNUP =
            "<p>Hi {nickname}，</p><p>你已成功报名 <b>{contestTitle}</b>，请留意作品提交截止时间。</p>";
    private static final String DEFAULT_TPL_REVIEW =
            "<p>Hi {nickname}，</p><p><b>{contestTitle}</b> 的评审已开始，请尽快完成分配作品的打分。</p>";
    private static final String DEFAULT_TPL_WINNER =
            "<p>Hi {nickname}，</p><p>恭喜你在 <b>{contestTitle}</b> 中获得 <b>{awardName}</b>！</p>";
    private static final String DEFAULT_TPL_VERIFY =
            "<p>Hi {nickname}，</p><p>你的验证码是：<b style=\"font-size:18px\">{code}</b></p>"
                    + "<p>验证码 10 分钟内有效，请勿泄露给他人。</p>";

    private final SysConfigMapper sysConfigMapper;

    @Value("${app.config.secret:}")
    private String configSecret;

    public ConfigService(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    /* ------------------------- 种子数据 ------------------------- */

    @PostConstruct
    public void ensureDefaults() {
        setDefault(KEY_MAIL_HOST, "", "SMTP 服务器地址");
        setDefault(KEY_MAIL_PORT, "465", "SMTP 端口");
        setDefault(KEY_MAIL_USERNAME, "", "邮箱账号");
        setDefault(KEY_MAIL_PASSWORD, "", "邮箱密码/授权码（加密存储）");
        setDefault(KEY_MAIL_FROM, "BTR 赛事平台", "发件人显示名");
        setDefault(KEY_MAIL_SSL, "true", "是否使用 SSL");
        setDefault(KEY_MAIL_ENABLED, "false", "邮件总开关");
        setDefault(KEY_SITE_NAME, "BTR 赛事平台", "站点名称");
        setDefault(KEY_SITE_LOGO, "", "站点 Logo URL");
        setDefault(KEY_SITE_FOOTER, "© 2026 BTR Competition Platform. All rights reserved.", "页脚信息");
        setDefault(KEY_SWITCH_REGISTER, "true", "是否开放注册");
        setDefault(KEY_SWITCH_CAPTCHA, "", "图形验证码开关（留空跟随环境配置）");
        setDefault(KEY_TPL_SIGNUP, DEFAULT_TPL_SIGNUP, "报名成功邮件模板，占位符 {nickname} {contestTitle}");
        setDefault(KEY_TPL_REVIEW, DEFAULT_TPL_REVIEW, "评审提醒邮件模板，占位符 {nickname} {contestTitle}");
        setDefault(KEY_TPL_WINNER, DEFAULT_TPL_WINNER, "获奖通知邮件模板，占位符 {nickname} {contestTitle} {awardName}");
        setDefault(KEY_TPL_VERIFY, DEFAULT_TPL_VERIFY, "验证码邮件模板，占位符 {nickname} {code}");
        setDefault(KEY_UPLOAD_MAX_MB, "50", "单文件上传上限（MB）");
        setDefault(KEY_UPLOAD_TYPES,
                "jpg,jpeg,png,gif,webp,svg,pdf,zip,rar,7z,doc,docx,ppt,pptx,xls,xlsx,txt,md,mp4,mov",
                "可上传文件类型白名单（逗号分隔）");
        setDefault(KEY_SEC_LOGIN_FAIL_LIMIT, "5", "登录失败锁定阈值（次）");
        setDefault(KEY_SEC_LOGIN_LOCK_MINUTES, "10", "登录锁定时长（分钟）");
        log.info("系统配置默认值初始化完成");
    }

    private void setDefault(String key, String value, String desc) {
        Long exists = sysConfigMapper.selectCount(new QueryWrapper<SysConfig>().eq("config_key", key));
        if (exists == null || exists == 0) {
            SysConfig config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(desc);
            config.setUpdatedAt(LocalDateTime.now());
            sysConfigMapper.insert(config);
        }
    }

    /* ------------------------- 通用 KV ------------------------- */

    public String get(String key, String def) {
        SysConfig config = sysConfigMapper.selectOne(new QueryWrapper<SysConfig>()
                .eq("config_key", key).last("limit 1"));
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return def;
        }
        return config.getConfigValue();
    }

    public boolean getBool(String key, boolean def) {
        String value = get(key, null);
        return value == null ? def : Boolean.parseBoolean(value);
    }

    public int getInt(String key, int def) {
        String value = get(key, null);
        if (value == null) {
            return def;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public void set(String key, String value, String desc) {
        SysConfig config = sysConfigMapper.selectOne(new QueryWrapper<SysConfig>()
                .eq("config_key", key).last("limit 1"));
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(key);
            config.setDescription(desc);
        }
        config.setConfigValue(value);
        config.setUpdatedAt(LocalDateTime.now());
        if (config.getId() == null) {
            sysConfigMapper.insert(config);
        } else {
            sysConfigMapper.updateById(config);
        }
    }

    /* ------------------------- 邮件配置 ------------------------- */

    public MailConfig getMail() {
        MailConfig config = new MailConfig();
        config.setHost(get(KEY_MAIL_HOST, ""));
        config.setPort(getInt(KEY_MAIL_PORT, 465));
        config.setUsername(get(KEY_MAIL_USERNAME, ""));
        String password = get(KEY_MAIL_PASSWORD, "");
        config.setPassword(password.isBlank() ? "" : MASKED);
        config.setFrom(get(KEY_MAIL_FROM, "BTR 赛事平台"));
        config.setSsl(getBool(KEY_MAIL_SSL, true));
        config.setEnabled(getBool(KEY_MAIL_ENABLED, false));
        return config;
    }

    public void updateMail(com.btr.platform.dto.Requests.MailConfigReq req) {
        if (req.host() != null) {
            set(KEY_MAIL_HOST, req.host().trim(), "SMTP 服务器地址");
        }
        if (req.port() != null) {
            set(KEY_MAIL_PORT, String.valueOf(req.port()), "SMTP 端口");
        }
        if (req.username() != null) {
            set(KEY_MAIL_USERNAME, req.username().trim(), "邮箱账号");
        }
        if (req.password() != null && !req.password().isBlank() && !MASKED.equals(req.password())) {
            set(KEY_MAIL_PASSWORD, encrypt(req.password().trim()), "邮箱密码/授权码（加密存储）");
        }
        if (req.from() != null) {
            set(KEY_MAIL_FROM, req.from().trim(), "发件人显示名");
        }
        if (req.ssl() != null) {
            set(KEY_MAIL_SSL, String.valueOf(req.ssl()), "是否使用 SSL");
        }
        if (req.enabled() != null) {
            set(KEY_MAIL_ENABLED, String.valueOf(req.enabled()), "邮件总开关");
        }
    }

    /** 邮件发送所需明文配置（内部使用，不回显） */
    public MailRuntime mailRuntime() {
        MailRuntime runtime = new MailRuntime();
        runtime.host = get(KEY_MAIL_HOST, "");
        runtime.port = getInt(KEY_MAIL_PORT, 465);
        runtime.username = get(KEY_MAIL_USERNAME, "");
        String password = get(KEY_MAIL_PASSWORD, "");
        runtime.password = password.isBlank() ? "" : decrypt(password);
        runtime.from = get(KEY_MAIL_FROM, "BTR 赛事平台");
        runtime.ssl = getBool(KEY_MAIL_SSL, true);
        runtime.enabled = getBool(KEY_MAIL_ENABLED, false);
        return runtime;
    }

    public static class MailRuntime {
        public String host;
        public int port;
        public String username;
        public String password;
        public String from;
        public boolean ssl;
        public boolean enabled;
    }

    /* ------------------------- 站点配置 ------------------------- */

    public SiteConfig getSite() {
        SiteConfig config = new SiteConfig();
        config.setName(get(KEY_SITE_NAME, "BTR 赛事平台"));
        config.setLogoUrl(get(KEY_SITE_LOGO, ""));
        config.setFooter(get(KEY_SITE_FOOTER, "© 2026 BTR Competition Platform. All rights reserved."));
        return config;
    }

    public void updateSite(com.btr.platform.dto.Requests.SiteConfigReq req) {
        if (req.name() != null && !req.name().isBlank()) {
            set(KEY_SITE_NAME, req.name().trim(), "站点名称");
        }
        if (req.logoUrl() != null) {
            set(KEY_SITE_LOGO, req.logoUrl().trim(), "站点 Logo URL");
        }
        if (req.footer() != null) {
            set(KEY_SITE_FOOTER, req.footer().trim(), "页脚信息");
        }
    }

    /* ------------------------- 系统开关 ------------------------- */

    public SwitchesConfig getSwitches() {
        SwitchesConfig config = new SwitchesConfig();
        config.setRegisterEnabled(getBool(KEY_SWITCH_REGISTER, true));
        String captcha = get(KEY_SWITCH_CAPTCHA, null);
        config.setCaptchaEnabled(captcha == null ? null : Boolean.parseBoolean(captcha));
        return config;
    }

    public void updateSwitches(com.btr.platform.dto.Requests.SwitchesConfigReq req) {
        if (req.registerEnabled() != null) {
            set(KEY_SWITCH_REGISTER, String.valueOf(req.registerEnabled()), "是否开放注册");
        }
        if (req.captchaEnabled() != null) {
            set(KEY_SWITCH_CAPTCHA, String.valueOf(req.captchaEnabled()), "图形验证码开关（留空跟随环境配置）");
        }
    }

    /* ------------------------- 通知模板 ------------------------- */

    public List<TemplateConfig> getTemplates() {
        return List.of(
                tpl(KEY_TPL_SIGNUP, "报名成功邮件", "占位符 {nickname} {contestTitle}"),
                tpl(KEY_TPL_REVIEW, "评审提醒邮件", "占位符 {nickname} {contestTitle}"),
                tpl(KEY_TPL_WINNER, "获奖通知邮件", "占位符 {nickname} {contestTitle} {awardName}"),
                tpl(KEY_TPL_VERIFY, "验证码邮件", "占位符 {nickname} {code}"));
    }

    private TemplateConfig tpl(String key, String name, String desc) {
        TemplateConfig config = new TemplateConfig();
        config.setKey(key);
        config.setContent(get(key, ""));
        config.setDescription(name + "，" + desc);
        return config;
    }

    public String getTemplate(String key, String def) {
        return get(key, def);
    }

    public void updateTemplate(String key, String content) {
        String desc = key + " 模板";
        set(key, content, desc);
    }

    public static String render(String template, Map<String, String> vars) {
        if (template == null) {
            return "";
        }
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    /* ------------------------- 上传配置 ------------------------- */

    public UploadConfig getUpload() {
        UploadConfig config = new UploadConfig();
        config.setMaxSizeMb(getInt(KEY_UPLOAD_MAX_MB, 50));
        config.setAllowedTypes(get(KEY_UPLOAD_TYPES,
                "jpg,jpeg,png,gif,webp,svg,pdf,zip,rar,7z,doc,docx,ppt,pptx,xls,xlsx,txt,md,mp4,mov"));
        return config;
    }

    public void updateUpload(com.btr.platform.dto.Requests.UploadConfigReq req) {
        if (req.maxSizeMb() != null && req.maxSizeMb() > 0) {
            set(KEY_UPLOAD_MAX_MB, String.valueOf(req.maxSizeMb()), "单文件上传上限（MB）");
        }
        if (req.allowedTypes() != null && !req.allowedTypes().isBlank()) {
            set(KEY_UPLOAD_TYPES, req.allowedTypes().trim(), "可上传文件类型白名单（逗号分隔）");
        }
    }

    /* ------------------------- 安全策略 ------------------------- */

    public SecurityConfig getSecurity() {
        SecurityConfig config = new SecurityConfig();
        config.setLoginFailLimit(getInt(KEY_SEC_LOGIN_FAIL_LIMIT, 5));
        config.setLoginLockMinutes(getInt(KEY_SEC_LOGIN_LOCK_MINUTES, 10));
        return config;
    }

    public void updateSecurity(com.btr.platform.dto.Requests.SecurityConfigReq req) {
        if (req.loginFailLimit() != null && req.loginFailLimit() > 0) {
            set(KEY_SEC_LOGIN_FAIL_LIMIT, String.valueOf(req.loginFailLimit()), "登录失败锁定阈值（次）");
        }
        if (req.loginLockMinutes() != null && req.loginLockMinutes() > 0) {
            set(KEY_SEC_LOGIN_LOCK_MINUTES, String.valueOf(req.loginLockMinutes()), "登录锁定时长（分钟）");
        }
    }

    /* ------------------------- 敏感项加密 ------------------------- */

    private SecretKeySpec secretKey() throws Exception {
        String source = configSecret == null || configSecret.isBlank() ? "btr-platform-config-secret" : configSecret;
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(digest, 16), "AES");
    }

    private String encrypt(String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            log.warn("配置加密失败，回退明文存储：{}", e.getMessage());
            return plain;
        }
    }

    private String decrypt(String cipherText) {
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText);
            if (payload.length <= 12) {
                return cipherText;
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(),
                    new GCMParameterSpec(128, payload, 0, 12));
            byte[] plain = cipher.doFinal(payload, 12, payload.length - 12);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("配置解密失败（密钥变化或非加密存储）：{}", e.getMessage());
            return cipherText;
        }
    }

    /** 全部配置键值（管理端调试/展示用） */
    public Map<String, String> all() {
        return sysConfigMapper.selectList(new QueryWrapper<SysConfig>()
                        .orderByAsc("config_key")).stream()
                .collect(Collectors.toMap(SysConfig::getConfigKey,
                        c -> c.getConfigValue() == null ? "" : c.getConfigValue(),
                        (a, b) -> a, LinkedHashMap::new));
    }
}
