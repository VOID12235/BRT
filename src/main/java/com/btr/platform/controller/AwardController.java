package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import com.btr.platform.dto.Views.AwardItem;
import com.btr.platform.dto.Views.MyAwards;
import com.btr.platform.entity.User;
import com.btr.platform.security.SecurityUtils;
import com.btr.platform.service.AuthService;
import com.btr.platform.service.AwardService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/awards")
public class AwardController {

    private final AwardService awardService;
    private final AuthService authService;

    public AwardController(AwardService awardService, AuthService authService) {
        this.awardService = awardService;
        this.authService = authService;
    }

    /** 当前用户获奖明细（公示固化的真实数据） */
    @GetMapping("/mine")
    public ApiResponse<MyAwards> mine() {
        return ApiResponse.ok(awardService.listMyAwards(SecurityUtils.current().id()));
    }

    /** 下载指定赛事的获奖证书 PDF（仅限本人） */
    @GetMapping("/mine/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam Long contestId) {
        Long userId = SecurityUtils.current().id();
        AwardItem item = awardService.myAward(userId, contestId);
        User user = authService.userEntity(userId);
        byte[] pdf = awardService.buildCertificatePdf(item, user);
        String filename = URLEncoder.encode("获奖证书-" + item.getContestTitle() + ".pdf", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
