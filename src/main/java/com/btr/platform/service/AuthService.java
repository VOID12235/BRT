package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btr.platform.common.BizException;
import com.btr.platform.common.CacheService;
import com.btr.platform.common.PageResult;
import com.btr.platform.dto.Requests.AdminCreateUserReq;
import com.btr.platform.dto.Requests.ForgotPasswordReq;
import com.btr.platform.dto.Requests.LoginReq;
import com.btr.platform.dto.Requests.RegisterReq;
import com.btr.platform.dto.Requests.ResetPasswordByCodeReq;
import com.btr.platform.dto.Requests.ResetPasswordReq;
import com.btr.platform.dto.Views.AuthResult;
import com.btr.platform.dto.Views.UserView;
import com.btr.platform.entity.User;
import com.btr.platform.mapper.UserMapper;
import com.btr.platform.security.JwtService;
import com.btr.platform.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private static final Set<String> ROLES = Set.of("PLAYER", "JUDGE", "ADMIN");
    private static final String VERIFY_KEY_PREFIX = "pwd:code:";
    private static final String VERIFY_FAIL_PREFIX = "pwd:fail:";
    private static final Duration VERIFY_TTL = Duration.ofMinutes(10);
    private static final int VERIFY_FAIL_LIMIT = 5;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CacheService cacheService;
    private final CaptchaService captchaService;
    private final EmailService emailService;
    private final LogService logService;
    private final ConfigService configService;

    @Value("${app.captcha.enabled:false}")
    private boolean envCaptchaEnabled;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService,
                       CacheService cacheService, CaptchaService captchaService,
                       EmailService emailService, LogService logService, ConfigService configService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cacheService = cacheService;
        this.captchaService = captchaService;
        this.emailService = emailService;
        this.logService = logService;
        this.configService = configService;
    }

    @Transactional
    public AuthResult register(RegisterReq req, String ip) {
        if (!configService.getBool(ConfigService.KEY_SWITCH_REGISTER, true)) {
            throw new BizException(403, "当前暂未开放注册，请联系管理员");
        }
        if (userMapper.selectCount(new QueryWrapper<User>()
                .eq("account", req.account())) > 0) {
            throw new BizException("该账号已被注册");
        }
        if (req.email() != null && !req.email().isBlank()
                && userMapper.selectCount(new QueryWrapper<User>()
                .eq("email", req.email())) > 0) {
            throw new BizException("该邮箱已被注册");
        }
        User user = new User();
        user.setAccount(req.account().trim());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setNickname(req.nickname().trim());
        user.setEmail(req.email());
        user.setPhone(req.phone());
        user.setRole("PLAYER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        logService.record(user.getId(), "REGISTER", user.getAccount(), ip);
        emailService.send(user.getEmail(), "WELCOME", "欢迎加入 BTR",
                "<p>Hi " + user.getNickname() + "，欢迎加入 BTR 赛事平台。</p>");
        return issue(user);
    }

    public AuthResult login(LoginReq req, String ip) {
        String account = req.account().trim();
        String lockKey = "login:lock:" + account;
        if (cacheService.get(lockKey) != null) {
            throw new BizException(403, "登录失败次数过多，账号已临时锁定，请稍后再试");
        }
        if (captchaEnabled() && !captchaService.verify(req.captchaKey(), req.captchaCode())) {
            throw new BizException("验证码错误或已过期");
        }
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("account", account));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            registerLoginFail(account);
            throw new BizException(401, "账号或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException(403, "账号已被禁用，请联系管理员");
        }
        cacheService.delete("login:fail:" + account);
        cacheService.delete(lockKey);
        logService.record(user.getId(), "LOGIN", user.getAccount(), ip);
        return issue(user);
    }

    private void registerLoginFail(String account) {
        com.btr.platform.dto.Views.SecurityConfig security = configService.getSecurity();
        int limit = Math.max(1, security.getLoginFailLimit() == null ? 5 : security.getLoginFailLimit());
        int lockMinutes = Math.max(1, security.getLoginLockMinutes() == null ? 10 : security.getLoginLockMinutes());
        String failKey = "login:fail:" + account;
        int fails = 0;
        String current = cacheService.get(failKey);
        if (current != null) {
            try {
                fails = Integer.parseInt(current);
            } catch (NumberFormatException ignored) {
                fails = 0;
            }
        }
        fails++;
        cacheService.set(failKey, String.valueOf(fails), Duration.ofMinutes(lockMinutes));
        if (fails >= limit) {
            cacheService.set("login:lock:" + account, "1", Duration.ofMinutes(lockMinutes));
            cacheService.delete(failKey);
        }
    }

    public AuthResult refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (Exception e) {
            throw new BizException(401, "登录已过期，请重新登录");
        }
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BizException(401, "无效的刷新凭证");
        }
        if (cacheService.get("jwt:blacklist:" + claims.getId()) != null) {
            throw new BizException(401, "登录已失效，请重新登录");
        }
        User user = userMapper.selectById(Long.valueOf(claims.getSubject()));
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new BizException(401, "账号不可用");
        }
        blacklist(claims);
        return issue(user);
    }

    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            blacklist(jwtService.parse(accessToken));
        } catch (Exception ignored) {
        }
    }

    private void blacklist(Claims claims) {
        long seconds = jwtService.remainingSeconds(claims);
        cacheService.set("jwt:blacklist:" + claims.getId(), "1", Duration.ofSeconds(seconds));
    }

    public UserView me(Long userId) {
        return toView(userEntity(userId));
    }

    public User userEntity(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        return user;
    }

    public PageResult<UserView> listUsers(String role, String keyword, long page, long size) {
        QueryWrapper<User> query = new QueryWrapper<>();
        if (role != null && !role.isBlank()) {
            query.eq("role", role);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.and(w -> w.like("account", keyword).or().like("nickname", keyword).or().like("email", keyword));
        }
        query.orderByDesc("created_at");
        Page<User> result = userMapper.selectPage(new Page<>(page, size), query);
        return PageResult.of(result.getRecords().stream().map(AuthService::toView).toList(),
                result.getTotal(), page, size);
    }

    public void setUserStatus(Long id, String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new BizException("非法用户状态");
        }
        if ("DISABLED".equals(status)) {
            assertNotLastAdmin(id);
        }
        User user = userEntity(id);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /* ------------------------- 管理员账号管理 ------------------------- */

    /** 管理员创建任意角色账号 */
    @Transactional
    public UserView adminCreateUser(AdminCreateUserReq req, Long operatorId, String ip) {
        String role = req.role();
        if (!ROLES.contains(role)) {
            throw new BizException("非法角色：" + role);
        }
        if (userMapper.selectCount(new QueryWrapper<User>().eq("account", req.account())) > 0) {
            throw new BizException("该账号已被注册");
        }
        if (req.email() != null && !req.email().isBlank()
                && userMapper.selectCount(new QueryWrapper<User>().eq("email", req.email())) > 0) {
            throw new BizException("该邮箱已被注册");
        }
        User user = new User();
        user.setAccount(req.account().trim());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setNickname(req.nickname().trim());
        user.setEmail(req.email());
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        logService.record(operatorId, "ADMIN_CREATE_USER", user.getAccount() + "/" + role, ip);
        return toView(user);
    }

    /** 管理员重置任意账号密码（含自己） */
    @Transactional
    public void adminResetPassword(Long id, ResetPasswordReq req, Long operatorId, String ip) {
        User user = userEntity(id);
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        logService.record(operatorId, "ADMIN_RESET_PASSWORD", user.getAccount(), ip);
    }

    /** 调整账号角色，最后一个启用中的 ADMIN 不可降级 */
    @Transactional
    public void adminChangeRole(Long id, String role, Long operatorId, String ip) {
        if (!ROLES.contains(role)) {
            throw new BizException("非法角色：" + role);
        }
        User user = userEntity(id);
        if ("ADMIN".equals(user.getRole()) && !"ADMIN".equals(role)) {
            assertNotLastAdmin(id);
        }
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        logService.record(operatorId, "ADMIN_CHANGE_ROLE", user.getAccount() + " -> " + role, ip);
    }

    /** 防锁死：目标为系统最后一个启用中的管理员时拒绝操作 */
    private void assertNotLastAdmin(Long targetId) {
        List<User> admins = userMapper.selectList(new QueryWrapper<User>()
                .eq("role", "ADMIN").eq("status", "ACTIVE"));
        if (admins.size() == 1 && admins.get(0).getId().equals(targetId)) {
            throw new BizException(400, "系统至少需要保留一个启用中的管理员账号");
        }
    }

    /* ------------------------- 忘记密码 ------------------------- */

    /** 发送重置验证码邮件 */
    public String forgotPassword(ForgotPasswordReq req, String ip) {
        String email = req.email().trim();
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", email).last("limit 1"));
        // 邮箱不存在也返回成功，防止账号枚举
        if (user == null) {
            logService.record(null, "FORGOT_PASSWORD", "unknown:" + email, ip);
            return null;
        }
        ConfigService.MailRuntime mail = configService.mailRuntime();
        if (!mail.enabled || mail.host.isBlank()) {
            throw new BizException("管理员暂未配置邮件服务，请联系管理员重置密码");
        }
        if (cacheService.get(VERIFY_KEY_PREFIX + email) != null) {
            throw new BizException("发送过于频繁，请 1 分钟后再试");
        }
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        cacheService.set(VERIFY_KEY_PREFIX + email, code, VERIFY_TTL);
        cacheService.delete(VERIFY_FAIL_PREFIX + email);
        emailService.sendVerifyCode(email, user.getNickname(), code);
        logService.record(user.getId(), "FORGOT_PASSWORD", email, ip);
        return user.getEmail();
    }

    /** 校验验证码并重置密码 */
    @Transactional
    public void resetPassword(ResetPasswordByCodeReq req, String ip) {
        String email = req.email().trim();
        String key = VERIFY_KEY_PREFIX + email;
        String expected = cacheService.get(key);
        if (expected == null) {
            throw new BizException("验证码已过期，请重新获取");
        }
        if (!expected.equals(req.code().trim())) {
            String failKey = VERIFY_FAIL_PREFIX + email;
            int fails = 0;
            String current = cacheService.get(failKey);
            if (current != null) {
                try {
                    fails = Integer.parseInt(current);
                } catch (NumberFormatException ignored) {
                    fails = 0;
                }
            }
            fails++;
            if (fails >= VERIFY_FAIL_LIMIT) {
                cacheService.delete(key);
                cacheService.delete(failKey);
                throw new BizException("验证码错误次数过多，已失效，请重新获取");
            }
            cacheService.set(failKey, String.valueOf(fails), VERIFY_TTL);
            throw new BizException("验证码错误");
        }
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", email).last("limit 1"));
        if (user == null) {
            throw new BizException("账号不存在");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        cacheService.delete(key);
        cacheService.delete(VERIFY_FAIL_PREFIX + email);
        logService.record(user.getId(), "RESET_PASSWORD", email, ip);
    }

    /* ------------------------- 其他 ------------------------- */

    private AuthResult issue(User user) {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getAccount(), user.getNickname(), user.getRole());
        AuthResult result = new AuthResult();
        result.setAccessToken(jwtService.createAccessToken(principal));
        result.setRefreshToken(jwtService.createRefreshToken(principal));
        result.setUser(toView(user));
        return result;
    }

    public static UserView toView(User user) {
        UserView view = new UserView();
        view.setId(user.getId());
        view.setAccount(user.getAccount());
        view.setNickname(user.getNickname());
        view.setEmail(user.getEmail());
        view.setPhone(user.getPhone());
        view.setRole(user.getRole());
        view.setStatus(user.getStatus());
        view.setAvatar(user.getAvatar());
        view.setCreatedAt(user.getCreatedAt());
        return view;
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled();
    }

    /** 验证码开关：后台配置优先，未配置时跟随环境变量 */
    private boolean captchaEnabled() {
        com.btr.platform.dto.Views.SwitchesConfig switches = configService.getSwitches();
        if (switches.getCaptchaEnabled() != null) {
            return switches.getCaptchaEnabled();
        }
        return envCaptchaEnabled;
    }
}
