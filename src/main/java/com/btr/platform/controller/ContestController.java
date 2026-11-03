package com.btr.platform.controller;

import com.btr.platform.common.ApiResponse;
import com.btr.platform.common.PageResult;
import com.btr.platform.dto.Requests.AwardReq;
import com.btr.platform.dto.Requests.ContestReq;
import com.btr.platform.dto.Requests.StatusReq;
import com.btr.platform.dto.Views.ContestCard;
import com.btr.platform.dto.Views.ContestDetail;
import com.btr.platform.entity.Award;
import com.btr.platform.entity.Contest;
import com.btr.platform.security.SecurityUtils;
import com.btr.platform.security.UserPrincipal;
import com.btr.platform.service.ContestService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contests")
public class ContestController {

    private final ContestService contestService;

    public ContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    @GetMapping
    public ApiResponse<PageResult<ContestCard>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "9") long size,
            @RequestParam(defaultValue = "false") boolean all) {
        UserPrincipal principal = SecurityUtils.currentOrNull();
        boolean includeAll = all && principal != null && "ADMIN".equals(principal.role());
        Long userId = principal == null ? null : principal.id();
        return ApiResponse.ok(contestService.list(status, keyword, page, size, includeAll, userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ContestDetail> detail(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.currentOrNull();
        boolean includeAll = principal != null && "ADMIN".equals(principal.role());
        Long userId = principal == null ? null : principal.id();
        return ApiResponse.ok(contestService.detail(id, userId, includeAll));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Contest> create(@Valid @RequestBody ContestReq req) {
        return ApiResponse.ok(contestService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Contest> update(@PathVariable Long id, @Valid @RequestBody ContestReq req) {
        return ApiResponse.ok(contestService.update(id, req));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Contest> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusReq req) {
        return ApiResponse.ok(contestService.changeStatus(id, req.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contestService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/awards")
    public ApiResponse<List<Award>> awards(@PathVariable Long id) {
        return ApiResponse.ok(contestService.awards(id));
    }

    @PostMapping("/{id}/awards")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Award> addAward(@PathVariable Long id, @Valid @RequestBody AwardReq req) {
        return ApiResponse.ok(contestService.addAward(id, req));
    }

    @PutMapping("/{id}/awards/{awardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Award> updateAward(@PathVariable Long id, @PathVariable Long awardId,
                                          @Valid @RequestBody AwardReq req) {
        return ApiResponse.ok(contestService.updateAward(id, awardId, req));
    }

    @DeleteMapping("/{id}/awards/{awardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteAward(@PathVariable Long id, @PathVariable Long awardId) {
        contestService.deleteAward(id, awardId);
        return ApiResponse.ok();
    }
}
