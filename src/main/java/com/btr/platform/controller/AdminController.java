package com.btr.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btr.platform.common.ApiResponse;
import com.btr.platform.common.PageResult;
import com.btr.platform.dto.Requests.AdminCreateUserReq;
import com.btr.platform.dto.Requests.AssignReq;
import com.btr.platform.dto.Views;
import com.btr.platform.dto.Requests.ChangeRoleReq;
import com.btr.platform.dto.Requests.LockReq;
import com.btr.platform.dto.Requests.DbConfigReq;
import com.btr.platform.dto.Requests.MailConfigReq;
import com.btr.platform.dto.Requests.MailTestReq;
import com.btr.platform.dto.Requests.NoticeReq;
import com.btr.platform.dto.Requests.RedisConfigReq;
import com.btr.platform.dto.Requests.ResetPasswordReq;
import com.btr.platform.dto.Requests.SecurityConfigReq;
import com.btr.platform.dto.Requests.SignupStatusReq;
import com.btr.platform.dto.Requests.SiteConfigReq;
import com.btr.platform.dto.Requests.SwitchesConfigReq;
import com.btr.platform.dto.Requests.TemplateConfigReq;
import com.btr.platform.dto.Requests.UploadConfigReq;
import com.btr.platform.dto.Requests.UserStatusReq;
import com.btr.platform.dto.Views.AssignView;
import com.btr.platform.dto.Views.Dashboard;
import com.btr.platform.dto.Views.DbConfig;
import com.btr.platform.dto.Views.LogView;
import com.btr.platform.dto.Views.MailConfig;
import com.btr.platform.dto.Views.MailRecordView;
import com.btr.platform.dto.Views.RedisConfig;
import com.btr.platform.dto.Views.SecurityConfig;
import com.btr.platform.dto.Views.SignupView;
import com.btr.platform.dto.Views.SiteConfig;
import com.btr.platform.dto.Views.SwitchesConfig;
import com.btr.platform.dto.Views.TemplateConfig;
import com.btr.platform.dto.Views.UploadConfig;
import com.btr.platform.dto.Views.UserView;
import com.btr.platform.dto.Views.WorkView;
import com.btr.platform.entity.EmailRecord;
import com.btr.platform.entity.Notice;
import com.btr.platform.entity.SysLog;
import com.btr.platform.entity.User;
import com.btr.platform.mapper.EmailRecordMapper;
import com.btr.platform.mapper.SysLogMapper;
import com.btr.platform.mapper.UserMapper;
import com.btr.platform.security.SecurityUtils;
import com.btr.platform.service.AuthService;
import com.btr.platform.service.ConfigService;
import com.btr.platform.service.DashboardService;
import com.btr.platform.service.EmailService;
import com.btr.platform.service.InfraTestService;
import com.btr.platform.service.ExportService;
import com.btr.platform.service.JudgeService;
import com.btr.platform.service.LogService;
import com.btr.platform.service.NoticeService;
import com.btr.platform.service.SignupService;
import com.btr.platform.service.WorkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final DashboardService dashboardService;
    private final SignupService signupService;
    private final WorkService workService;
    private final JudgeService judgeService;
    private final AuthService authService;
    private final NoticeService noticeService;
    private final ExportService exportService;
    private final ConfigService configService;
    private final EmailService emailService;
    private final InfraTestService infraTestService;
    private final SysLogMapper sysLogMapper;
    private final EmailRecordMapper emailRecordMapper;
    private final UserMapper userMapper;
    private final LogService logService;

    public AdminController(DashboardService dashboardService, SignupService signupService,
                           WorkService workService, JudgeService judgeService, AuthService authService,
                           NoticeService noticeService, ExportService exportService,
                           ConfigService configService, EmailService emailService,
                           InfraTestService infraTestService,
                           SysLogMapper sysLogMapper, EmailRecordMapper emailRecordMapper,
                           UserMapper userMapper, LogService logService) {
        this.dashboardService = dashboardService;
        this.signupService = signupService;
        this.workService = workService;
        this.judgeService = judgeService;
        this.authService = authService;
        this.noticeService = noticeService;
        this.exportService = exportService;
        this.configService = configService;
        this.emailService = emailService;
        this.infraTestService = infraTestService;
        this.sysLogMapper = sysLogMapper;
        this.emailRecordMapper = emailRecordMapper;
        this.userMapper = userMapper;
        this.logService = logService;
    }

    /* ------------------------- 仪表盘 / 业务管理（原有） ------------------------- */

    @GetMapping("/dashboard")
    public ApiResponse<Dashboard> dashboard() {
        return ApiResponse.ok(dashboardService.overview());
    }

    @GetMapping("/signups")
    public ApiResponse<PageResult<SignupView>> signups(
            @RequestParam(required = false) Long contestId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(signupService.adminList(contestId, status, page, size));
    }

    @PutMapping("/signups/{id}/status")
    public ApiResponse<Void> signupStatus(@PathVariable Long id, @Valid @RequestBody SignupStatusReq req) {
        signupService.adminStatus(id, req.status());
        return ApiResponse.ok();
    }

    @GetMapping("/works")
    public ApiResponse<PageResult<WorkView>> works(
            @RequestParam(required = false) Long contestId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(workService.adminList(contestId, keyword, page, size));
    }

    @PutMapping("/works/{id}/lock")
    public ApiResponse<Void> lockWork(@PathVariable Long id, @Valid @RequestBody LockReq req) {
        workService.lock(id, req.locked());
        return ApiResponse.ok();
    }

    @GetMapping("/judges")
    public ApiResponse<List<UserView>> judges() {
        return ApiResponse.ok(judgeService.judges().stream().map(AuthService::toView).toList());
    }

    @GetMapping("/judge-assign")
    public ApiResponse<List<AssignView>> assignments(@RequestParam(required = false) Long contestId) {
        return ApiResponse.ok(judgeService.assignments(contestId));
    }

    @GetMapping("/judge-assign/projects")
    public ApiResponse<List<Views.AdminAssignProject>> assignProjects(
            @RequestParam(required = false) Long contestId) {
        return ApiResponse.ok(judgeService.adminProjects(contestId));
    }

    @PostMapping("/judge-assign")
    public ApiResponse<Void> assign(@Valid @RequestBody AssignReq req) {
        judgeService.assign(req);
        return ApiResponse.ok();
    }

    @DeleteMapping("/judge-assign/{id}")
    public ApiResponse<Void> unassign(@PathVariable Long id) {
        judgeService.unassign(id);
        return ApiResponse.ok();
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<UserView>> users(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(authService.listUsers(role, keyword, page, size));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> userStatus(@PathVariable Long id, @Valid @RequestBody UserStatusReq req,
                                        HttpServletRequest request) {
        authService.setUserStatus(id, req.status());
        return ApiResponse.ok();
    }

    @GetMapping("/notices")
    public ApiResponse<PageResult<Notice>> notices(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(noticeService.list(keyword, page, size));
    }

    @PostMapping("/notices")
    public ApiResponse<Notice> createNotice(@Valid @RequestBody NoticeReq req) {
        return ApiResponse.ok(noticeService.create(req));
    }

    @PutMapping("/notices/{id}")
    public ApiResponse<Notice> updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeReq req) {
        return ApiResponse.ok(noticeService.update(id, req));
    }

    @DeleteMapping("/notices/{id}")
    public ApiResponse<Void> deleteNotice(@PathVariable Long id) {
        noticeService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/export/signups")
    public ResponseEntity<byte[]> exportSignups(@RequestParam(required = false) Long contestId) {
        return excel(exportService.exportSignups(contestId), "报名名单.xlsx");
    }

    @GetMapping("/export/winners")
    public ResponseEntity<byte[]> exportWinners(@RequestParam(required = false) Long contestId) {
        return excel(exportService.exportWinners(contestId), "获奖名单.xlsx");
    }

    private ResponseEntity<byte[]> excel(byte[] bytes, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /* ------------------------- 基础配置：邮件 ------------------------- */

    @GetMapping("/config/mail")
    public ApiResponse<MailConfig> mailConfig() {
        return ApiResponse.ok(configService.getMail());
    }

    @PutMapping("/config/mail")
    public ApiResponse<Void> updateMailConfig(@Valid @RequestBody MailConfigReq req, HttpServletRequest request) {
        configService.updateMail(req);
        logAdmin("CONFIG_MAIL", "更新邮件配置", request);
        return ApiResponse.ok();
    }

    @PostMapping("/config/mail/test")
    public ApiResponse<Map<String, Object>> testMail(@Valid @RequestBody MailTestReq req, HttpServletRequest request) {
        String error = emailService.sendTest(req.to());
        logAdmin("CONFIG_MAIL_TEST", "测试邮件 -> " + req.to(), request);
        if (error == null) {
            return ApiResponse.ok(Map.of("success", true, "message", "测试邮件已发送，请查收"));
        }
        return ApiResponse.ok(Map.of("success", false, "message", error));
    }

    /* ------------------------- 基础配置：站点 / 开关 / 模板 / 上传 / 安全 ------------------------- */

    @GetMapping("/config/site")
    public ApiResponse<SiteConfig> siteConfig() {
        return ApiResponse.ok(configService.getSite());
    }

    @PutMapping("/config/site")
    public ApiResponse<Void> updateSiteConfig(@Valid @RequestBody SiteConfigReq req, HttpServletRequest request) {
        configService.updateSite(req);
        logAdmin("CONFIG_SITE", "更新站点信息", request);
        return ApiResponse.ok();
    }

    @GetMapping("/config/switches")
    public ApiResponse<SwitchesConfig> switchesConfig() {
        return ApiResponse.ok(configService.getSwitches());
    }

    @PutMapping("/config/switches")
    public ApiResponse<Void> updateSwitchesConfig(@Valid @RequestBody SwitchesConfigReq req, HttpServletRequest request) {
        configService.updateSwitches(req);
        logAdmin("CONFIG_SWITCHES", "更新系统开关", request);
        return ApiResponse.ok();
    }

    @GetMapping("/config/templates")
    public ApiResponse<List<TemplateConfig>> templates() {
        return ApiResponse.ok(configService.getTemplates());
    }

    @PutMapping("/config/templates/{key}")
    public ApiResponse<Void> updateTemplate(@PathVariable String key, @Valid @RequestBody TemplateConfigReq req,
                                            HttpServletRequest request) {
        configService.updateTemplate(key, req.content());
        logAdmin("CONFIG_TEMPLATE", "更新模板 " + key, request);
        return ApiResponse.ok();
    }

    @GetMapping("/config/uploads")
    public ApiResponse<UploadConfig> uploadConfig() {
        return ApiResponse.ok(configService.getUpload());
    }

    @PutMapping("/config/uploads")
    public ApiResponse<Void> updateUploadConfig(@Valid @RequestBody UploadConfigReq req, HttpServletRequest request) {
        configService.updateUpload(req);
        logAdmin("CONFIG_UPLOAD", "更新上传参数", request);
        return ApiResponse.ok();
    }

    @GetMapping("/config/security")
    public ApiResponse<SecurityConfig> securityConfig() {
        return ApiResponse.ok(configService.getSecurity());
    }

    @PutMapping("/config/security")
    public ApiResponse<Void> updateSecurityConfig(@Valid @RequestBody SecurityConfigReq req, HttpServletRequest request) {
        configService.updateSecurity(req);
        logAdmin("CONFIG_SECURITY", "更新安全策略", request);
        return ApiResponse.ok();
    }

    /* ------------------------- 基础配置：数据库与缓存 ------------------------- */

    @GetMapping("/config/db")
    public ApiResponse<DbConfig> dbConfig() {
        return ApiResponse.ok(configService.getDb());
    }

    @PutMapping("/config/db")
    public ApiResponse<Void> updateDbConfig(@Valid @RequestBody DbConfigReq req, HttpServletRequest request) {
        configService.updateDb(req);
        logAdmin("CONFIG_DB", "更新 MySQL 配置", request);
        return ApiResponse.ok();
    }

    @PostMapping("/config/db/test")
    public ApiResponse<Map<String, Object>> testDb(@Valid @RequestBody DbConfigReq req, HttpServletRequest request) {
        Map<String, Object> result = infraTestService.testMySql(req);
        logAdmin("CONFIG_DB_TEST", "MySQL 连接测试：" + result.get("message"), request);
        return ApiResponse.ok(result);
    }

    @GetMapping("/config/redis")
    public ApiResponse<RedisConfig> redisConfig() {
        return ApiResponse.ok(configService.getRedis());
    }

    @PutMapping("/config/redis")
    public ApiResponse<Void> updateRedisConfig(@Valid @RequestBody RedisConfigReq req, HttpServletRequest request) {
        configService.updateRedis(req);
        logAdmin("CONFIG_REDIS", "更新 Redis 配置", request);
        return ApiResponse.ok();
    }

    @PostMapping("/config/redis/test")
    public ApiResponse<Map<String, Object>> testRedis(@Valid @RequestBody RedisConfigReq req, HttpServletRequest request) {
        Map<String, Object> result = infraTestService.testRedis(req);
        logAdmin("CONFIG_REDIS_TEST", "Redis 连接测试：" + result.get("message"), request);
        return ApiResponse.ok(result);
    }

    /* ------------------------- 账号权限管理 ------------------------- */

    @PostMapping("/users")
    public ApiResponse<UserView> createUser(@Valid @RequestBody AdminCreateUserReq req, HttpServletRequest request) {
        UserView view = authService.adminCreateUser(req, SecurityUtils.current().id(), ip(request));
        logAdmin("ADMIN_CREATE_USER", "创建账号 " + view.getAccount() + "/" + view.getRole(), request);
        return ApiResponse.ok(view);
    }

    @PutMapping("/users/{id}/password")
    public ApiResponse<Void> resetUserPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordReq req,
                                               HttpServletRequest request) {
        authService.adminResetPassword(id, req, SecurityUtils.current().id(), ip(request));
        return ApiResponse.ok();
    }

    @PutMapping("/users/{id}/role")
    public ApiResponse<Void> changeUserRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleReq req,
                                            HttpServletRequest request) {
        authService.adminChangeRole(id, req.role(), SecurityUtils.current().id(), ip(request));
        return ApiResponse.ok();
    }

    /* ------------------------- 日志与邮件记录 ------------------------- */

    @GetMapping("/logs")
    public ApiResponse<PageResult<LogView>> logs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        QueryWrapper<SysLog> query = new QueryWrapper<>();
        if (action != null && !action.isBlank()) {
            query.eq("action", action);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.and(w -> w.like("target", keyword).or().like("ip", keyword));
        }
        query.orderByDesc("created_at");
        Page<SysLog> result = sysLogMapper.selectPage(new Page<>(page, size), query);
        Map<Long, User> users = result.getRecords().stream()
                .map(SysLog::getUserId).filter(java.util.Objects::nonNull).distinct()
                .collect(Collectors.toMap(Function.identity(), id -> {
                    User u = userMapper.selectById(id);
                    return u == null ? new User() : u;
                }, (a, b) -> a));
        List<LogView> views = result.getRecords().stream().map(log -> {
            LogView view = new LogView();
            view.setId(log.getId());
            view.setUserId(log.getUserId());
            view.setAction(log.getAction());
            view.setTarget(log.getTarget());
            view.setIp(log.getIp());
            view.setCreatedAt(log.getCreatedAt());
            User user = users.get(log.getUserId());
            if (user != null) {
                view.setAccount(user.getAccount());
                view.setNickname(user.getNickname());
            }
            return view;
        }).toList();
        return ApiResponse.ok(PageResult.of(views, result.getTotal(), page, size));
    }

    @GetMapping("/mails")
    public ApiResponse<PageResult<MailRecordView>> mails(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String recipient,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        QueryWrapper<EmailRecord> query = new QueryWrapper<>();
        if (status != null && !status.isBlank()) {
            query.eq("status", status);
        }
        if (recipient != null && !recipient.isBlank()) {
            query.like("recipient", recipient);
        }
        query.orderByDesc("created_at");
        Page<EmailRecord> result = emailRecordMapper.selectPage(new Page<>(page, size), query);
        List<MailRecordView> views = result.getRecords().stream().map(record -> {
            MailRecordView view = new MailRecordView();
            view.setId(record.getId());
            view.setRecipient(record.getRecipient());
            view.setTemplate(record.getTemplate());
            view.setSubject(record.getSubject());
            view.setStatus(record.getStatus());
            view.setError(record.getError());
            view.setCreatedAt(record.getCreatedAt());
            return view;
        }).toList();
        return ApiResponse.ok(PageResult.of(views, result.getTotal(), page, size));
    }

    /* ------------------------- 工具 ------------------------- */

    private void logAdmin(String action, String target, HttpServletRequest request) {
        Long operatorId = SecurityUtils.currentOrNull() == null ? null : SecurityUtils.current().id();
        logService.record(operatorId, action, target, ip(request));
    }

    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
