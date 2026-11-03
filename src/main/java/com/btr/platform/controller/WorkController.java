package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import com.btr.platform.common.PageResult;
import com.btr.platform.dto.Requests.WorkReq;
import com.btr.platform.dto.Views.WorkView;
import com.btr.platform.security.SecurityUtils;
import com.btr.platform.service.WorkService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/works")
public class WorkController {

    private final WorkService workService;

    public WorkController(WorkService workService) {
        this.workService = workService;
    }

    @PostMapping
    public ApiResponse<WorkView> submit(@Valid @RequestBody WorkReq req) {
        return ApiResponse.ok(workService.submit(req, SecurityUtils.current().id()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> withdraw(@PathVariable Long id) {
        workService.withdraw(id, SecurityUtils.current().id());
        return ApiResponse.ok();
    }

    @GetMapping("/mine")
    public ApiResponse<List<WorkView>> mine() {
        return ApiResponse.ok(workService.mine(SecurityUtils.current().id()));
    }

    @GetMapping
    public ApiResponse<PageResult<WorkView>> list(
            @RequestParam(required = false) Long contestId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "12") long size) {
        return ApiResponse.ok(workService.publicList(contestId, page, size));
    }
}
