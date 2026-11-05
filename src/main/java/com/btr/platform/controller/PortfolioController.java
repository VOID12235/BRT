package com.btr.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.common.ApiResponse;
import com.btr.platform.common.BizException;
import com.btr.platform.dto.Views.Portfolio;
import com.btr.platform.dto.Views.PortfolioAward;
import com.btr.platform.dto.Views.PortfolioUser;
import com.btr.platform.dto.Views.PortfolioWork;
import com.btr.platform.entity.Award;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.Score;
import com.btr.platform.entity.Signup;
import com.btr.platform.entity.User;
import com.btr.platform.entity.Winner;
import com.btr.platform.entity.Work;
import com.btr.platform.mapper.AwardMapper;
import com.btr.platform.mapper.ContestMapper;
import com.btr.platform.mapper.ScoreMapper;
import com.btr.platform.mapper.SignupMapper;
import com.btr.platform.mapper.UserMapper;
import com.btr.platform.mapper.WinnerMapper;
import com.btr.platform.mapper.WorkMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 选手公开作品集：展示昵称、参赛作品、获奖记录（含奖金/奖品）。
 */
@RestController
@RequestMapping("/api/v1/users")
public class PortfolioController {

    private final UserMapper userMapper;
    private final SignupMapper signupMapper;
    private final WorkMapper workMapper;
    private final WinnerMapper winnerMapper;
    private final ContestMapper contestMapper;
    private final AwardMapper awardMapper;
    private final ScoreMapper scoreMapper;

    public PortfolioController(UserMapper userMapper, SignupMapper signupMapper, WorkMapper workMapper,
                               WinnerMapper winnerMapper, ContestMapper contestMapper,
                               AwardMapper awardMapper, ScoreMapper scoreMapper) {
        this.userMapper = userMapper;
        this.signupMapper = signupMapper;
        this.workMapper = workMapper;
        this.winnerMapper = winnerMapper;
        this.contestMapper = contestMapper;
        this.awardMapper = awardMapper;
        this.scoreMapper = scoreMapper;
    }

    @GetMapping("/{id}/portfolio")
    public ApiResponse<Portfolio> portfolio(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        Portfolio out = new Portfolio();
        PortfolioUser pu = new PortfolioUser();
        pu.setId(user.getId());
        pu.setNickname(user.getNickname());
        pu.setAvatar(user.getAvatar());
        pu.setRole(user.getRole());
        pu.setEmail(maskEmail(user.getEmail()));
        pu.setPhone(maskPhone(user.getPhone()));
        out.setUser(pu);

        out.setSignupCount(signupMapper.selectCount(new QueryWrapper<Signup>()
                .eq("user_id", id).ne("status", "CANCELED")));

        List<Work> works = workMapper.selectList(new QueryWrapper<Work>()
                .eq("user_id", id).eq("status", "SUBMITTED")
                // 仅展示已公示 / 已结束赛事中的作品，避免未公示作品提前泄露
                .inSql("contest_id", "SELECT id FROM contest WHERE status IN ('PUBLISHED','FINISHED')")
                .orderByDesc("submitted_at"));

        // 关联赛事
        Set<Long> contestIds = works.stream().map(Work::getContestId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        contestIds.addAll(winnerRelatedContests(id));
        Map<Long, Contest> contestMap = contestIds.isEmpty() ? Map.of()
                : contestMapper.selectBatchIds(contestIds).stream()
                        .collect(Collectors.toMap(Contest::getId, Function.identity(), (a, b) -> a));

        // 作品平均分
        Set<Long> workIds = works.stream().map(Work::getId).collect(Collectors.toSet());
        Map<Long, BigDecimal> avgMap = workIds.isEmpty() ? Map.of() : averageScores(workIds);

        // 作品对应奖项
        Map<Long, Winner> winnerByWork = workIds.isEmpty() ? Map.of()
                : winnerMapper.selectList(new QueryWrapper<Winner>().in("work_id", workIds)).stream()
                        .filter(w -> w.getWorkId() != null)
                        .collect(Collectors.toMap(Winner::getWorkId, Function.identity(), (a, b) -> a));

        Map<Long, Award> awardMap = awardMapFor(winnerByWork.values());

        List<PortfolioWork> workViews = works.stream().map(w -> {
            PortfolioWork v = new PortfolioWork();
            v.setId(w.getId());
            v.setTitle(w.getTitle());
            v.setDescription(w.getDescription());
            v.setFileUrl(w.getFileUrl());
            v.setFileName(w.getFileName());
            v.setContestId(w.getContestId());
            Contest c = contestMap.get(w.getContestId());
            if (c != null) {
                v.setContestTitle(c.getTitle());
                v.setContestMode(c.getMode());
            }
            v.setStatus(w.getStatus());
            Winner winner = winnerByWork.get(w.getId());
            if (winner != null) {
                v.setRankNo(winner.getRankNo());
                Award award = awardMap.get(winner.getAwardId());
                if (award != null) v.setAwardName(award.getName());
            }
            v.setAvgScore(avgMap.get(w.getId()));
            return v;
        }).toList();
        out.setWorks(workViews);
        out.setWorkCount((long) workViews.size());

        // 获奖记录（作品关联 + 线下报名关联）
        List<Winner> winners = new ArrayList<>(winnerByWork.values());
        List<Signup> signups = signupMapper.selectList(new QueryWrapper<Signup>()
                .eq("user_id", id).ne("status", "CANCELED"));
        Set<Long> signupIds = signups.stream().map(Signup::getId).collect(Collectors.toSet());
        if (!signupIds.isEmpty()) {
            List<Winner> signupWinners = winnerMapper.selectList(new QueryWrapper<Winner>()
                    .in("signup_id", signupIds).isNull("work_id"));
            winners.addAll(signupWinners);
        }
        Map<Long, Award> allAwardMap = awardMapFor(winners);
        List<PortfolioAward> awardViews = winners.stream()
                .sorted((a, b) -> b.getRankNo() == null || a.getRankNo() == null ? 0 : Integer.compare(a.getRankNo(), b.getRankNo()))
                .map(w -> {
                    PortfolioAward v = new PortfolioAward();
                    v.setContestId(w.getContestId());
                    Contest c = contestMap.get(w.getContestId());
                    if (c != null) {
                        v.setContestTitle(c.getTitle());
                        v.setContestMode(c.getMode());
                    }
                    v.setRankNo(w.getRankNo());
                    v.setPublishedAt(w.getCreatedAt());
                    Award award = allAwardMap.get(w.getAwardId());
                    if (award != null) {
                        v.setAwardName(award.getName());
                        v.setPrizeAmount(award.getPrizeAmount());
                        v.setGift(award.getGift());
                    }
                    return v;
                }).distinct().toList();
        out.setAwards(awardViews);
        out.setAwardCount((long) awardViews.size());
        return ApiResponse.ok(out);
    }

    private Set<Long> winnerRelatedContests(Long userId) {
        List<Work> userWorks = workMapper.selectList(new QueryWrapper<Work>().eq("user_id", userId));
        Set<Long> ids = userWorks.stream().map(Work::getContestId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Signup> signups = signupMapper.selectList(new QueryWrapper<Signup>().eq("user_id", userId));
        ids.addAll(signups.stream().map(Signup::getContestId).filter(Objects::nonNull).toList());
        return ids;
    }

    private Map<Long, BigDecimal> averageScores(Set<Long> workIds) {
        List<Score> scores = scoreMapper.selectList(new QueryWrapper<Score>().in("work_id", workIds));
        Map<Long, List<BigDecimal>> grouped = scores.stream()
                .filter(s -> s.getWorkId() != null && s.getScore() != null)
                .collect(Collectors.groupingBy(Score::getWorkId,
                        Collectors.mapping(Score::getScore, Collectors.toList())));
        Map<Long, BigDecimal> out = new java.util.HashMap<>();
        grouped.forEach((wid, list) -> {
            BigDecimal sum = list.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            out.put(wid, sum.divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP));
        });
        return out;
    }

    private Map<Long, Award> awardMapFor(java.util.Collection<Winner> winners) {
        Set<Long> awardIds = winners.stream().map(Winner::getAwardId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return awardIds.isEmpty() ? Map.of()
                : awardMapper.selectBatchIds(awardIds).stream()
                        .collect(Collectors.toMap(Award::getId, Function.identity(), (a, b) -> a));
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return null;
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(Math.max(0, at));
        return email.substring(0, 1) + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
