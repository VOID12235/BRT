package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import com.btr.platform.common.PageResult;
import com.btr.platform.dto.Requests.AssignReq;
import com.btr.platform.dto.Requests.LockReq;
import com.btr.platform.dto.Requests.NoticeReq;
import com.btr.platform.dto.Requests.SignupStatusReq;
import com.btr.platform.dto.Requests.UserStatusReq;
import com.btr.platform.dto.Views.AssignView;
import com.btr.platform.dto.Views.Dashboard;
import com.btr.platform.dto.Views.SignupView;
import com.btr.platform.dto.Views.UserView;
import com.btr.platform.dto.Views.WorkView;
import com.btr.platform.entity.Notice;
import com.btr.platform.service.AuthService;
import com.btr.platform.service.DashboardService;
import com.btr.platform.service.ExportService;
import com.btr.platform.service.JudgeService;
import com.btr.platform.service.NoticeService;
import com.btr.platform.service.SignupService;
import com.btr.platform.service.WorkService;
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

    public AdminController(DashboardService dashboardService, SignupService signupService,
                           WorkService workService, JudgeService judgeService, AuthService authService,
                           NoticeService noticeService, ExportService exportService) {
        this.dashboardService = dashboardService;
        this.signupService = signupService;
        this.workService = workService;
        this.judgeService = judgeService;
        this.authService = authService;
        this.noticeService = noticeService;
        this.exportService = exportService;
    }

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
    public ApiResponse<Void> userStatus(@PathVariable Long id, @Valid @RequestBody UserStatusReq req) {
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
}
