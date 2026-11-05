package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import com.btr.platform.dto.Views.RankResult;
import com.btr.platform.entity.Winner;
import com.btr.platform.security.SecurityUtils;
import com.btr.platform.service.RankService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ranks")
public class RankController {

    private final RankService rankService;

    public RankController(RankService rankService) {
        this.rankService = rankService;
    }

    @GetMapping
    public ApiResponse<RankResult> result(@RequestParam Long contestId,
                                          @RequestParam(defaultValue = "false") boolean preview) {
        boolean admin = SecurityUtils.currentOrNull() != null && SecurityUtils.isAdmin();
        return ApiResponse.ok(rankService.result(contestId, preview && admin));
    }

    @GetMapping("/winners")
    public ApiResponse<List<Winner>> winners(@RequestParam Long contestId) {
        boolean admin = SecurityUtils.currentOrNull() != null && SecurityUtils.isAdmin();
        return ApiResponse.ok(rankService.winners(contestId, admin));
    }
}
