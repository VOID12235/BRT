package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btr.platform.common.BizException;
import com.btr.platform.common.CacheService;
import com.btr.platform.common.PageResult;
import com.btr.platform.dto.Requests.LoginReq;
import com.btr.platform.dto.Requests.RegisterReq;
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

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CacheService cacheService;
    private final CaptchaService captchaService;
    private final EmailService emailService;
    private final LogService logService;

    @Value("${app.captcha.enabled:false}")
    private boolean captchaEnabled;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService,
                       CacheService cacheService, CaptchaService captchaService,
                       EmailService emailService, LogService logService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cacheService = cacheService;
        this.captchaService = captchaService;
        this.emailService = emailService;
        this.logService = logService;
    }

    @Transactional
    public AuthResult register(RegisterReq req, String ip) {
        if (userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("account", req.account())) > 0) {
            throw new BizException("该账号已被注册");
        }
        if (req.email() != null && !req.email().isBlank()
                && userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
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
        if (captchaEnabled && !captchaService.verify(req.captchaKey(), req.captchaCode())) {
            throw new BizException("验证码错误或已过期");
        }
        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("account", req.account().trim()));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BizException(401, "账号或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException(403, "账号已被禁用，请联系管理员");
        }
        logService.record(user.getId(), "LOGIN", user.getAccount(), ip);
        return issue(user);
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
        User user = userEntity(id);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

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
        return captchaEnabled;
    }
}
