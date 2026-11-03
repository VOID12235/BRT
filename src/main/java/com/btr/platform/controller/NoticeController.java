package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import com.btr.platform.common.PageResult;
import com.btr.platform.entity.Notice;
import com.btr.platform.service.NoticeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<PageResult<Notice>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(noticeService.list(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Notice> detail(@PathVariable Long id) {
        return ApiResponse.ok(noticeService.get(id));
    }
}
