package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import com.btr.platform.dto.Requests.ForgotPasswordReq;
import com.btr.platform.dto.Requests.LoginReq;
import com.btr.platform.dto.Requests.RefreshReq;
import com.btr.platform.dto.Requests.RegisterReq;
import com.btr.platform.dto.Requests.ResetPasswordByCodeReq;
import com.btr.platform.dto.Views.AuthResult;
import com.btr.platform.dto.Views.UserView;
import com.btr.platform.security.SecurityUtils;
import com.btr.platform.service.AuthService;
import com.btr.platform.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResult> register(@Valid @RequestBody RegisterReq req, HttpServletRequest request) {
        return ApiResponse.ok(authService.register(req, ip(request)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResult> login(@Valid @RequestBody LoginReq req, HttpServletRequest request) {
        return ApiResponse.ok(authService.login(req, ip(request)));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResult> refresh(@Valid @RequestBody RefreshReq req) {
        return ApiResponse.ok(authService.refresh(req.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization == null ? null : authorization.replace("Bearer ", ""));
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me() {
        return ApiResponse.ok(authService.me(SecurityUtils.current().id()));
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaService.Captcha> captcha() {
        return ApiResponse.ok(captchaService.generate());
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordReq req,
                                                           HttpServletRequest request) {
        String result = authService.forgotPassword(req, ip(request));
        if (result == null) {
            return ApiResponse.ok(Map.of("message", "如果该邮箱已注册，验证码邮件将发送至对应邮箱"));
        }
        return ApiResponse.ok(Map.of("message", "验证码已发送，请查收邮件"));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordByCodeReq req,
                                           HttpServletRequest request) {
        authService.resetPassword(req, ip(request));
        return ApiResponse.ok();
    }

    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
