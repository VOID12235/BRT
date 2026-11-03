package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import com.btr.platform.dto.Requests.ScoreReq;
import com.btr.platform.dto.Views.JudgeProgress;
import com.btr.platform.dto.Views.JudgeTask;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.Score;
import com.btr.platform.security.SecurityUtils;
import com.btr.platform.service.JudgeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/judge")
public class JudgeController {

    private final JudgeService judgeService;

    public JudgeController(JudgeService judgeService) {
        this.judgeService = judgeService;
    }

    @GetMapping("/tasks")
    public ApiResponse<List<JudgeTask>> tasks(@RequestParam(required = false) Long contestId) {
        return ApiResponse.ok(judgeService.tasks(SecurityUtils.current().id(), contestId));
    }

    @GetMapping("/progress")
    public ApiResponse<List<JudgeProgress>> progress() {
        return ApiResponse.ok(judgeService.progress(SecurityUtils.current().id()));
    }

    @GetMapping("/contests")
    public ApiResponse<List<Contest>> contests() {
        return ApiResponse.ok(judgeService.contests(SecurityUtils.current().id()));
    }

    @PostMapping("/scores")
    public ApiResponse<Score> score(@Valid @RequestBody ScoreReq req) {
        return ApiResponse.ok(judgeService.score(req, SecurityUtils.current().id()));
    }
}
