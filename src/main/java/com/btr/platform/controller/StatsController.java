package com.btr.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.common.ApiResponse;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.Signup;
import com.btr.platform.entity.User;
import com.btr.platform.entity.Work;
import com.btr.platform.mapper.ContestMapper;
import com.btr.platform.mapper.SignupMapper;
import com.btr.platform.mapper.UserMapper;
import com.btr.platform.mapper.WorkMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final ContestMapper contestMapper;
    private final SignupMapper signupMapper;
    private final WorkMapper workMapper;
    private final UserMapper userMapper;

    public StatsController(ContestMapper contestMapper, SignupMapper signupMapper,
                           WorkMapper workMapper, UserMapper userMapper) {
        this.contestMapper = contestMapper;
        this.signupMapper = signupMapper;
        this.workMapper = workMapper;
        this.userMapper = userMapper;
    }

    @GetMapping
    public ApiResponse<Map<String, Long>> stats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("contests", contestMapper.selectCount(new QueryWrapper<Contest>()
                .eq("is_public", true)));
        stats.put("signups", signupMapper.selectCount(new QueryWrapper<Signup>()
                .ne("status", "CANCELED")));
        stats.put("works", workMapper.selectCount(new QueryWrapper<Work>()
                .eq("status", "SUBMITTED")));
        stats.put("judges", userMapper.selectCount(new QueryWrapper<User>()
                .eq("role", "JUDGE").eq("status", "ACTIVE")));
        stats.put("players", userMapper.selectCount(new QueryWrapper<User>()
                .eq("role", "PLAYER").eq("status", "ACTIVE")));
        return ApiResponse.ok(stats);
    }
}
