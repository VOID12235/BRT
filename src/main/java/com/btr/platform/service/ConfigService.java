package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.common.BizException;
import com.btr.platform.dto.Views.DbConfig;
import com.btr.platform.dto.Views.MailConfig;
import com.btr.platform.dto.Views.RedisConfig;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    public static final String KEY_ABOUT_INTRO = "about.intro";
    public static final String KEY_ABOUT_RULES = "about.rules";
    public static final String KEY_ABOUT_CONTACT = "about.contact";
    public static final String KEY_SWITCH_REGISTER = "switch.register_enabled";
    public static final String KEY_SWITCH_CAPTCHA = "switch.captcha_enabled";
    public static final String KEY_TPL_SIGNUP = "template.signup_receipt";
    public static final String KEY_TPL_REVIEW = "template.review_reminder";
    public static final String KEY_TPL_WINNER = "template.winner_notice";
    public static final String KEY_TPL_VERIFY = "template.verify_code";
    public static final String KEY_TPL_CERT = "template.award_certificate";
    public static final String KEY_UPLOAD_MAX_MB = "upload.max_size_mb";
    public static final String KEY_UPLOAD_TYPES = "upload.allowed_types";
    public static final String KEY_SEC_LOGIN_FAIL_LIMIT = "security.login_fail_limit";
    public static final String KEY_SEC_LOGIN_LOCK_MINUTES = "security.login_lock_minutes";
    public static final String KEY_DB_MYSQL_HOST = "db.mysql.host";
    public static final String KEY_DB_MYSQL_PORT = "db.mysql.port";
    public static final String KEY_DB_MYSQL_DATABASE = "db.mysql.database";
    public static final String KEY_DB_MYSQL_USERNAME = "db.mysql.username";
    public static final String KEY_DB_MYSQL_PASSWORD = "db.mysql.password";
    public static final String KEY_REDIS_HOST = "redis.host";
    public static final String KEY_REDIS_PORT = "redis.port";
    public static final String KEY_REDIS_PASSWORD = "redis.password";
    public static final String KEY_REDIS_DATABASE = "redis.database";

    private static final String DEFAULT_TPL_SIGNUP =
            "<p>Hi {nickname}，</p><p>你已成功报名 <b>{contestTitle}</b>，请留意作品提交截止时间。</p>";
    private static final String DEFAULT_TPL_REVIEW =
            "<p>Hi {nickname}，</p><p><b>{contestTitle}</b> 的评审已开始，请尽快完成分配作品的打分。</p>";
    private static final String DEFAULT_TPL_WINNER =
            "<p>Hi {nickname}，</p><p>恭喜你在 <b>{contestTitle}</b> 中获得 <b>{awardName}</b>！</p>";
    private static final String DEFAULT_TPL_VERIFY =
            "<p>Hi {nickname}，</p><p>你的验证码是：<b style=\"font-size:18px\">{code}</b></p>"
                    + "<p>验证码 10 分钟内有效，请勿泄露给他人。</p>";
    public static final String DEFAULT_TPL_CERT =
            "BTR 赛事平台\n"
                    + "#获奖证书\n"
                    + "兹证明 {nickname} 同学\n"
                    + "在「{contestTitle}」赛事中，作品《{workTitle}》表现优异，荣获\n"
                    + "##{awardName}\n"
                    + "第 {rankNo} 名　·　奖励 {prizeText}\n"
                    + "颁发日期：{date}\n"
                    + "BTR 赛事平台 · 本证书以公示获奖名单为准";

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            KEY_MAIL_PASSWORD, KEY_DB_MYSQL_PASSWORD, KEY_REDIS_PASSWORD);
    private static final Set<String> TEMPLATE_KEYS = Set.of(
            KEY_TPL_SIGNUP, KEY_TPL_REVIEW, KEY_TPL_WINNER, KEY_TPL_VERIFY, KEY_TPL_CERT);

    private final SysConfigMapper sysConfigMapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

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
        setDefault(KEY_ABOUT_INTRO,
                "BTR 赛事平台面向赛事组织者、选手与评委，提供赛事发布、在线报名、作品提交、评委打分与榜单公示的一站式服务。\n"
                        + "平台致力于用清晰的流程与可靠的数据，让每一场比赛被认真对待。",
                "关于页 · 平台介绍");
        setDefault(KEY_ABOUT_RULES,
                "1. 参赛者须为本人真实信息报名，线下赛事实行实名制；\n"
                        + "2. 线上作品须为本人原创，禁止抄袭、代打与违规合作；\n"
                        + "3. 评委按分配任务独立打分，评语全程留痕；\n"
                        + "4. 榜单以公示结果为准，获奖证书与奖金按公示名单发放；\n"
                        + "5. 违规行为一经查实，平台有权取消成绩并保留追责权利。",
                "关于页 · 规则说明");
        setDefault(KEY_ABOUT_CONTACT,
                "平台邮箱：support@btr.dev\n"
                        + "工作时间：周一至周五 9:00 - 18:00\n"
                        + "商务合作：business@btr.dev",
                "关于页 · 联系我们");
        setDefault(KEY_SWITCH_REGISTER, "true", "是否开放注册");
        setDefault(KEY_SWITCH_CAPTCHA, "", "图形验证码开关（留空跟随环境配置）");
        setDefault(KEY_TPL_SIGNUP, DEFAULT_TPL_SIGNUP, "报名成功邮件模板，占位符 {nickname} {contestTitle}");
        setDefault(KEY_TPL_REVIEW, DEFAULT_TPL_REVIEW, "评审提醒邮件模板，占位符 {nickname} {contestTitle}");
        setDefault(KEY_TPL_WINNER, DEFAULT_TPL_WINNER, "获奖通知邮件模板，占位符 {nickname} {contestTitle} {awardName}");
        setDefault(KEY_TPL_VERIFY, DEFAULT_TPL_VERIFY, "验证码邮件模板，占位符 {nickname} {code}");
        setDefault(KEY_TPL_CERT, DEFAULT_TPL_CERT,
                "获奖证书模板（每行一段，# 标题 ## 强调，占位符 {nickname} {contestTitle} {workTitle} {awardName} {rankNo} {prizeAmount} {date}）");
        setDefault(KEY_UPLOAD_MAX_MB, "50", "单文件上传上限（MB）");
        setDefault(KEY_UPLOAD_TYPES,
                "jpg,jpeg,png,gif,webp,svg,pdf,zip,rar,7z,doc,docx,ppt,pptx,xls,xlsx,txt,md,mp4,mov",
                "可上传文件类型白名单（逗号分隔）");
        setDefault(KEY_SEC_LOGIN_FAIL_LIMIT, "5", "登录失败锁定阈值（次）");
        setDefault(KEY_SEC_LOGIN_LOCK_MINUTES, "10", "登录锁定时长（分钟）");
        setDefault(KEY_DB_MYSQL_HOST, "", "MySQL 主机地址");
        setDefault(KEY_DB_MYSQL_PORT, "3306", "MySQL 端口");
        setDefault(KEY_DB_MYSQL_DATABASE, "", "MySQL 数据库名");
        setDefault(KEY_DB_MYSQL_USERNAME, "", "MySQL 用户名");
        setDefault(KEY_DB_MYSQL_PASSWORD, "", "MySQL 密码（加密存储）");
        setDefault(KEY_REDIS_HOST, "127.0.0.1", "Redis 主机地址");
        setDefault(KEY_REDIS_PORT, "6379", "Redis 端口");
        setDefault(KEY_REDIS_PASSWORD, "", "Redis 密码（加密存储）");
        setDefault(KEY_REDIS_DATABASE, "0", "Redis 数据库索引");
        reloadCache();
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
        String value = cache.get(key);
        if (value == null) {
            // 运行期新增的键（或缓存未命中）回源一次，避免每个请求都查库
            SysConfig config = sysConfigMapper.selectOne(new QueryWrapper<SysConfig>()
                    .eq("config_key", key).last("limit 1"));
            if (config == null) {
                return def;
            }
            value = config.getConfigValue() == null ? "" : config.getConfigValue();
            cache.put(key, value);
        }
        return value.isBlank() ? def : value;
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
        cache.put(key, value == null ? "" : value);
    }

    /** 从数据库重新加载配置缓存（启动或批量变更后调用）。 */
    public void reloadCache() {
        Map<String, String> loaded = new LinkedHashMap<>();
        for (SysConfig config : sysConfigMapper.selectList(
                new QueryWrapper<SysConfig>().orderByAsc("config_key"))) {
            loaded.put(config.getConfigKey(), config.getConfigValue() == null ? "" : config.getConfigValue());
        }
        cache.clear();
        cache.putAll(loaded);
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
        config.setAboutIntro(get(KEY_ABOUT_INTRO, ""));
        config.setAboutRules(get(KEY_ABOUT_RULES, ""));
        config.setAboutContact(get(KEY_ABOUT_CONTACT, ""));
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
        if (req.aboutIntro() != null) {
            set(KEY_ABOUT_INTRO, req.aboutIntro(), "关于页 · 平台介绍");
        }
        if (req.aboutRules() != null) {
            set(KEY_ABOUT_RULES, req.aboutRules(), "关于页 · 规则说明");
        }
        if (req.aboutContact() != null) {
            set(KEY_ABOUT_CONTACT, req.aboutContact(), "关于页 · 联系我们");
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
                tpl(KEY_TPL_VERIFY, "验证码邮件", "占位符 {nickname} {code}"),
                tpl(KEY_TPL_CERT, "获奖证书",
                        "每行一段；# 标题、## 强调；占位符 {nickname} {contestTitle} {workTitle} {awardName} {rankNo} {prizeAmount} {date}"));
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
        if (key == null || !TEMPLATE_KEYS.contains(key)) {
            throw new BizException(400, "未知的模板键");
        }
        set(key, content, key + " 模板");
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

    /* ------------------------- 数据库与缓存配置 ------------------------- */

    public DbConfig getDb() {
        DbConfig config = new DbConfig();
        config.setHost(get(KEY_DB_MYSQL_HOST, ""));
        config.setPort(getInt(KEY_DB_MYSQL_PORT, 3306));
        config.setDatabase(get(KEY_DB_MYSQL_DATABASE, ""));
        config.setUsername(get(KEY_DB_MYSQL_USERNAME, ""));
        String password = get(KEY_DB_MYSQL_PASSWORD, "");
        config.setPassword(password.isBlank() ? "" : MASKED);
        return config;
    }

    public void updateDb(com.btr.platform.dto.Requests.DbConfigReq req) {
        if (req.host() != null) {
            set(KEY_DB_MYSQL_HOST, req.host().trim(), "MySQL 主机地址");
        }
        if (req.port() != null) {
            set(KEY_DB_MYSQL_PORT, String.valueOf(req.port()), "MySQL 端口");
        }
        if (req.database() != null) {
            set(KEY_DB_MYSQL_DATABASE, req.database().trim(), "MySQL 数据库名");
        }
        if (req.username() != null) {
            set(KEY_DB_MYSQL_USERNAME, req.username().trim(), "MySQL 用户名");
        }
        if (req.password() != null && !req.password().isBlank() && !MASKED.equals(req.password())) {
            set(KEY_DB_MYSQL_PASSWORD, encrypt(req.password().trim()), "MySQL 密码（加密存储）");
        }
    }

    /** MySQL 明文配置（内部使用，不回显） */
    public DbRuntime dbRuntime() {
        DbRuntime runtime = new DbRuntime();
        runtime.host = get(KEY_DB_MYSQL_HOST, "");
        runtime.port = getInt(KEY_DB_MYSQL_PORT, 3306);
        runtime.database = get(KEY_DB_MYSQL_DATABASE, "");
        runtime.username = get(KEY_DB_MYSQL_USERNAME, "");
        String password = get(KEY_DB_MYSQL_PASSWORD, "");
        runtime.password = password.isBlank() ? "" : decrypt(password);
        return runtime;
    }

    public RedisConfig getRedis() {
        RedisConfig config = new RedisConfig();
        config.setHost(get(KEY_REDIS_HOST, "127.0.0.1"));
        config.setPort(getInt(KEY_REDIS_PORT, 6379));
        String password = get(KEY_REDIS_PASSWORD, "");
        config.setPassword(password.isBlank() ? "" : MASKED);
        config.setDatabase(getInt(KEY_REDIS_DATABASE, 0));
        return config;
    }

    public void updateRedis(com.btr.platform.dto.Requests.RedisConfigReq req) {
        if (req.host() != null) {
            set(KEY_REDIS_HOST, req.host().trim(), "Redis 主机地址");
        }
        if (req.port() != null) {
            set(KEY_REDIS_PORT, String.valueOf(req.port()), "Redis 端口");
        }
        if (req.password() != null && !req.password().isBlank() && !MASKED.equals(req.password())) {
            set(KEY_REDIS_PASSWORD, encrypt(req.password().trim()), "Redis 密码（加密存储）");
        }
        if (req.database() != null) {
            set(KEY_REDIS_DATABASE, String.valueOf(req.database()), "Redis 数据库索引");
        }
    }

    /** Redis 明文配置（内部使用，不回显） */
    public RedisRuntime redisRuntime() {
        RedisRuntime runtime = new RedisRuntime();
        runtime.host = get(KEY_REDIS_HOST, "127.0.0.1");
        runtime.port = getInt(KEY_REDIS_PORT, 6379);
        runtime.database = getInt(KEY_REDIS_DATABASE, 0);
        String password = get(KEY_REDIS_PASSWORD, "");
        runtime.password = password.isBlank() ? "" : decrypt(password);
        return runtime;
    }

    public static class DbRuntime {
        public String host;
        public int port;
        public String database;
        public String username;
        public String password;
    }

    public static class RedisRuntime {
        public String host;
        public int port;
        public int database;
        public String password;
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

    /** 全部配置键值（管理端调试/展示用），敏感项统一脱敏。 */
    public Map<String, String> all() {
        return sysConfigMapper.selectList(new QueryWrapper<SysConfig>()
                        .orderByAsc("config_key")).stream()
                .collect(Collectors.toMap(SysConfig::getConfigKey,
                        c -> maskIfSensitive(c.getConfigKey(), c.getConfigValue()),
                        (a, b) -> a, LinkedHashMap::new));
    }

    private String maskIfSensitive(String key, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return SENSITIVE_KEYS.contains(key) ? MASKED : value;
    }
}
