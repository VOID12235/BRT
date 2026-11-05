package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.dto.Views.Dashboard;
import com.btr.platform.dto.Views.TrendPoint;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.Score;
import com.btr.platform.entity.Signup;
import com.btr.platform.entity.User;
import com.btr.platform.entity.Work;
import com.btr.platform.mapper.ContestMapper;
import com.btr.platform.mapper.NoticeMapper;
import com.btr.platform.mapper.ScoreMapper;
import com.btr.platform.mapper.SignupMapper;
import com.btr.platform.mapper.UserMapper;
import com.btr.platform.mapper.WorkMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UserMapper userMapper;
    private final ContestMapper contestMapper;
    private final SignupMapper signupMapper;
    private final WorkMapper workMapper;
    private final ScoreMapper scoreMapper;
    private final NoticeMapper noticeMapper;
    private final SignupService signupService;

    public DashboardService(UserMapper userMapper, ContestMapper contestMapper, SignupMapper signupMapper,
                            WorkMapper workMapper, ScoreMapper scoreMapper, NoticeMapper noticeMapper,
                            SignupService signupService) {
        this.userMapper = userMapper;
        this.contestMapper = contestMapper;
        this.signupMapper = signupMapper;
        this.workMapper = workMapper;
        this.scoreMapper = scoreMapper;
        this.noticeMapper = noticeMapper;
        this.signupService = signupService;
    }

    public Dashboard overview() {
        Dashboard dashboard = new Dashboard();
        dashboard.setUsers(userMapper.selectCount(null));
        dashboard.setPlayers(userMapper.selectCount(new QueryWrapper<User>().eq("role", "PLAYER")));
        dashboard.setJudges(userMapper.selectCount(new QueryWrapper<User>().eq("role", "JUDGE")));
        dashboard.setContests(contestMapper.selectCount(null));
        dashboard.setSignups(signupMapper.selectCount(new QueryWrapper<Signup>().ne("status", "CANCELED")));
        dashboard.setWorks(workMapper.selectCount(new QueryWrapper<Work>().eq("status", "SUBMITTED")));
        dashboard.setNotices(noticeMapper.selectCount(null));

        List<Long> reviewContestIds = contestMapper.selectList(new QueryWrapper<Contest>()
                        .eq("status", "REVIEW")).stream()
                .map(Contest::getId).toList();
        if (reviewContestIds.isEmpty()) {
            dashboard.setPendingReviews(0);
        } else {
            List<Work> works = workMapper.selectList(new QueryWrapper<Work>()
                    .in("contest_id", reviewContestIds).eq("status", "SUBMITTED"));
            Set<Long> scored = scoreMapper.selectList(new QueryWrapper<Score>()
                            .in("contest_id", reviewContestIds)).stream()
                    .map(Score::getWorkId).collect(Collectors.toSet());
            dashboard.setPendingReviews(works.stream().filter(w -> !scored.contains(w.getId())).count());
        }

        List<TrendPoint> trend = new ArrayList<>();
        LocalDate startDay = LocalDate.now().minusDays(6);
        // 一次查询取回 7 天内报名时间，再在内存分组，避免每天一次 COUNT
        Map<LocalDate, Long> dailyCounts = signupMapper.selectList(new QueryWrapper<Signup>()
                        .select("created_at").ge("created_at", startDay.atStartOfDay()))
                .stream()
                .filter(s -> s.getCreatedAt() != null)
                .collect(Collectors.groupingBy(s -> s.getCreatedAt().toLocalDate(), Collectors.counting()));
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            TrendPoint point = new TrendPoint();
            point.setDate(day.toString());
            point.setCount(dailyCounts.getOrDefault(day, 0L));
            trend.add(point);
        }
        dashboard.setTrend(trend);
        dashboard.setRecentSignups(signupService.adminList(null, null, 1, 6).records());
        return dashboard;
    }
}
