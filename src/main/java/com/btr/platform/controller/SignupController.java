package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import com.btr.platform.dto.Requests.SignupReq;
import com.btr.platform.dto.Views.SignupView;
import com.btr.platform.security.SecurityUtils;
import com.btr.platform.security.UserPrincipal;
import com.btr.platform.service.AuthService;
import com.btr.platform.service.SignupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/signups")
public class SignupController {

    private final SignupService signupService;
    private final AuthService authService;

    public SignupController(SignupService signupService, AuthService authService) {
        this.signupService = signupService;
        this.authService = authService;
    }

    @PostMapping
    public ApiResponse<SignupView> signup(@Valid @RequestBody SignupReq req) {
        UserPrincipal principal = SecurityUtils.current();
        return ApiResponse.ok(signupService.signup(req, authService.userEntity(principal.id())));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        signupService.cancel(id, SecurityUtils.current().id());
        return ApiResponse.ok();
    }

    @GetMapping("/mine")
    public ApiResponse<List<SignupView>> mine() {
        return ApiResponse.ok(signupService.mine(SecurityUtils.current().id()));
    }
}
