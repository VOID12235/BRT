package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import com.btr.platform.common.ClientIpResolver;
import com.btr.platform.dto.Requests;
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
import org.springframework.web.bind.annotation.PutMapping;
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
    private final ClientIpResolver clientIpResolver;

    public AuthController(AuthService authService, CaptchaService captchaService,
                          ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResult> register(@Valid @RequestBody RegisterReq req, HttpServletRequest request) {
        return ApiResponse.ok(authService.register(req, clientIpResolver.resolve(request)));
    }

    @PostMapping("/send-register-code")
    public ApiResponse<Map<String, String>> sendRegisterCode(@Valid @RequestBody ForgotPasswordReq req,
                                                             HttpServletRequest request) {
        authService.sendRegisterCode(req, clientIpResolver.resolve(request));
        // 统一文案，避免通过响应差异枚举已注册邮箱
        return ApiResponse.ok(Map.of("message", "如果该邮箱可用，验证码将发送至该邮箱"));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResult> login(@Valid @RequestBody LoginReq req, HttpServletRequest request) {
        return ApiResponse.ok(authService.login(req, clientIpResolver.resolve(request)));
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

    @PutMapping("/me")
    public ApiResponse<UserView> updateMe(@Valid @RequestBody Requests.UpdateProfileReq req) {
        return ApiResponse.ok(authService.updateProfile(SecurityUtils.current().id(), req));
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaService.Captcha> captcha() {
        return ApiResponse.ok(captchaService.generate());
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordReq req,
                                                           HttpServletRequest request) {
        authService.forgotPassword(req, clientIpResolver.resolve(request));
        // 统一文案，避免通过响应差异枚举已注册邮箱
        return ApiResponse.ok(Map.of("message", "如果该邮箱已注册，验证码邮件将发送至对应邮箱"));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordByCodeReq req,
                                           HttpServletRequest request) {
        authService.resetPassword(req, clientIpResolver.resolve(request));
        return ApiResponse.ok();
    }
}
