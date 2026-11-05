package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.common.BizException;
import com.btr.platform.common.CacheService;
import com.btr.platform.dto.Views.RankGroup;
import com.btr.platform.dto.Views.RankResult;
import com.btr.platform.dto.Views.RankRow;
import com.btr.platform.dto.Views.WorkScore;
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
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RankService {

    private static final String CACHE_PREFIX = "rank:contest:";
    private static final String ZSET_PREFIX = "rank:zset:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final ContestMapper contestMapper;
    private final WorkMapper workMapper;
    private final SignupMapper signupMapper;
    private final UserMapper userMapper;
    private final ScoreMapper scoreMapper;
    private final AwardMapper awardMapper;
    private final WinnerMapper winnerMapper;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    public RankService(ContestMapper contestMapper, WorkMapper workMapper, SignupMapper signupMapper,
                       UserMapper userMapper, ScoreMapper scoreMapper, AwardMapper awardMapper,
                       WinnerMapper winnerMapper, CacheService cacheService, ObjectMapper objectMapper,
                       EmailService emailService) {
        this.contestMapper = contestMapper;
        this.workMapper = workMapper;
        this.signupMapper = signupMapper;
        this.userMapper = userMapper;
        this.scoreMapper = scoreMapper;
        this.awardMapper = awardMapper;
        this.winnerMapper = winnerMapper;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.emailService = emailService;
    }

    public RankResult result(Long contestId, boolean includeUnpublished) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BizException(404, "赛事不存在");
        }
        boolean published = "PUBLISHED".equals(contest.getStatus()) || "FINISHED".equals(contest.getStatus());
        if (!published && !includeUnpublished) {
            RankResult empty = new RankResult();
            empty.setContestId(contest.getId());
            empty.setContestTitle(contest.getTitle());
            empty.setStatus(contest.getStatus());
            empty.setPublished(false);
            empty.setRows(List.of());
            empty.setGroups(List.of());
            return empty;
        }
        // 已公示赛事优先展示公示获奖名单（winner 表），保证公开榜单与获奖名单/证书一致；
        // 管理员预览（includeUnpublished）仍返回实时分数结果。
        if (published && !includeUnpublished) {
            if (winnerMapper.selectCount(new QueryWrapper<Winner>().eq("contest_id", contestId)) > 0) {
                return buildFromWinners(contest);
            }
        }
        String key = CACHE_PREFIX + contestId;
        if (published) {
            String cached = cacheService.get(key);
            if (cached != null) {
                try {
                    return objectMapper.readValue(cached, RankResult.class);
                } catch (Exception e) {
                    log.warn("榜单缓存解析失败，重新计算：{}", e.getMessage());
                }
            }
        }
        RankResult result = compute(contest);
        if (published) {
            cache(result);
        }
        return result;
    }

    /** 从公示获奖名单（winner 表）组装公开榜单，管理员手动调整立即生效。 */
    private RankResult buildFromWinners(Contest contest) {
        Long contestId = contest.getId();
        boolean offline = "OFFLINE".equals(contest.getMode());
        List<Winner> winners = winnerMapper.selectList(new QueryWrapper<Winner>()
                .eq("contest_id", contestId).orderByAsc("rank_no"));
        RankResult result = new RankResult();
        result.setContestId(contestId);
        result.setContestTitle(contest.getTitle());
        result.setStatus(contest.getStatus());
        result.setMode(contest.getMode());
        result.setPublished(true);
        result.setUpdatedAt(LocalDateTime.now());
        result.setWorkCount(workMapper.selectCount(new QueryWrapper<Work>()
                .eq("contest_id", contestId).eq("status", "SUBMITTED")));
        result.setJudgeCount(scoreMapper.judgeCount(contestId));
        result.setScoredCount(scoreMapper.countScoredWorks(contestId));
        if (winners.isEmpty()) {
            result.setRows(List.of());
            result.setGroups(List.of());
            return result;
        }

        Map<Long, Work> works = new HashMap<>();
        Map<Long, Signup> signups = new HashMap<>();
        Map<Long, Award> awards = new HashMap<>();
        if (!offline) {
            List<Long> workIds = winners.stream().map(Winner::getWorkId)
                    .filter(java.util.Objects::nonNull).distinct().toList();
            if (!workIds.isEmpty()) {
                workMapper.selectBatchIds(workIds).forEach(w -> works.put(w.getId(), w));
            }
        }
        List<Long> signupIds = winners.stream().map(Winner::getSignupId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (!signupIds.isEmpty()) {
            signupMapper.selectBatchIds(signupIds).forEach(s -> signups.put(s.getId(), s));
        }
        // 兜底：旧数据 winner.signup_id 可能缺失，按作品的 signup_id 补齐，保证榜单显示选手昵称
        if (!offline && !works.isEmpty()) {
            List<Long> workSignupIds = works.values().stream().map(Work::getSignupId)
                    .filter(java.util.Objects::nonNull).distinct().toList();
            List<Long> missing = workSignupIds.stream()
                    .filter(id -> !signups.containsKey(id)).toList();
            if (!missing.isEmpty()) {
                signupMapper.selectBatchIds(missing).forEach(s -> signups.put(s.getId(), s));
            }
        }
        List<Long> awardIds = winners.stream().map(Winner::getAwardId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (!awardIds.isEmpty()) {
            awardMapper.selectBatchIds(awardIds).forEach(a -> awards.put(a.getId(), a));
        }
        Map<Long, WorkScore> scoreMap = offline
                ? scoreMapper.avgByContestSignup(contestId).stream()
                        .collect(Collectors.toMap(WorkScore::getSignupId, Function.identity(), (a, b) -> a))
                : scoreMapper.avgByContest(contestId).stream()
                        .collect(Collectors.toMap(WorkScore::getWorkId, Function.identity(), (a, b) -> a));

        List<RankRow> rows = new ArrayList<>();
        Map<Long, RankGroup> groupMap = new java.util.LinkedHashMap<>();
        for (Winner winner : winners) {
            RankRow row = new RankRow();
            row.setRank(winner.getRankNo());
            row.setUserId(null);
            WorkScore score = null;
            if (offline) {
                Signup signup = signups.get(winner.getSignupId());
                if (signup == null) {
                    continue;
                }
                row.setSignupId(signup.getId());
                row.setUserId(signup.getUserId());
                row.setWorkTitle("选手：" + signup.getRealName());
                row.setAuthor(signup.getRealName());
                row.setRealName(signup.getRealName());
                row.setSchool(signup.getSchool());
                score = scoreMap.get(signup.getId());
            } else {
                Work work = works.get(winner.getWorkId());
                if (work == null) {
                    continue;
                }
                row.setWorkId(work.getId());
                row.setUserId(work.getUserId());
                row.setWorkTitle(work.getTitle());
                row.setFileUrl(work.getFileUrl());
                row.setFileName(work.getFileName());
                row.setCover(work.getCover());
                Signup signup = signups.get(work.getSignupId());
                if (signup != null) {
                    row.setAuthor(signup.getRealName());
                    row.setRealName(signup.getRealName());
                    row.setSchool(signup.getSchool());
                } else {
                    User user = userMapper.selectById(work.getUserId());
                    row.setAuthor(user == null ? "" : user.getNickname());
                    row.setRealName(row.getAuthor());
                }
                score = scoreMap.get(work.getId());
            }
            if (score != null) {
                row.setAvgScore(score.getAvgScore() == null ? null
                        : score.getAvgScore().setScale(2, RoundingMode.HALF_UP));
                row.setScoreCount(score.getScoreCount() == null ? 0 : score.getScoreCount());
            }
            Award award = awards.get(winner.getAwardId());
            if (award != null) {
                row.setAwardName(award.getName());
                row.setPrizeAmount(award.getPrizeAmount());
                row.setGift(award.getGift());
                RankGroup group = groupMap.computeIfAbsent(award.getId(), k -> {
                    RankGroup g = new RankGroup();
                    g.setAwardId(award.getId());
                    g.setAwardName(award.getName());
                    g.setQuota(award.getQuota());
                    g.setPrizeAmount(award.getPrizeAmount());
                    g.setGift(award.getGift());
                    g.setItems(new ArrayList<>());
                    return g;
                });
                group.getItems().add(row);
            }
            rows.add(row);
        }
        result.setRows(rows);
        result.setGroups(new ArrayList<>(groupMap.values()));
        return result;
    }

    @Transactional
    public RankResult computeAndCache(Long contestId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BizException(404, "赛事不存在");
        }
        RankResult result = compute(contest);
        cache(result);
        return result;
    }

    private RankResult compute(Contest contest) {
        Long contestId = contest.getId();
        boolean offline = "OFFLINE".equals(contest.getMode());
        Map<Long, WorkScore> scoreMap;
        Map<Long, Work> works = new HashMap<>();
        Map<Long, Signup> signups = new HashMap<>();
        List<Signup> allSignups = signupMapper.selectList(new QueryWrapper<Signup>()
                .eq("contest_id", contestId));
        for (Signup signup : allSignups) {
            signups.put(signup.getId(), signup);
        }
        if (offline) {
            scoreMap = scoreMapper.avgByContestSignup(contestId).stream()
                    .collect(Collectors.toMap(WorkScore::getSignupId, Function.identity(), (a, b) -> a));
        } else {
            List<Work> workList = workMapper.selectList(new QueryWrapper<Work>()
                    .eq("contest_id", contestId).eq("status", "SUBMITTED"));
            for (Work work : workList) {
                works.put(work.getId(), work);
            }
            scoreMap = scoreMapper.avgByContest(contestId).stream()
                    .collect(Collectors.toMap(WorkScore::getWorkId, Function.identity(), (a, b) -> a));
        }

        List<RankRow> rows = new ArrayList<>();
        if (offline) {
            for (Map.Entry<Long, WorkScore> entry : scoreMap.entrySet()) {
                Signup signup = signups.get(entry.getKey());
                if (signup == null) {
                    continue;
                }
                WorkScore score = entry.getValue();
                RankRow row = new RankRow();
                row.setSignupId(signup.getId());
                row.setUserId(signup.getUserId());
                row.setWorkTitle("选手：" + signup.getRealName());
                row.setAuthor(signup.getRealName());
                row.setRealName(signup.getRealName());
                row.setSchool(signup.getSchool());
                row.setAvgScore(score.getAvgScore().setScale(2, RoundingMode.HALF_UP));
                row.setScoreCount(score.getScoreCount());
                rows.add(row);
            }
        } else {
            for (Work work : works.values()) {
                WorkScore score = scoreMap.get(work.getId());
                if (score == null || score.getScoreCount() == null || score.getScoreCount() == 0) {
                    continue;
                }
                RankRow row = new RankRow();
                row.setWorkId(work.getId());
                row.setUserId(work.getUserId());
                row.setWorkTitle(work.getTitle());
                row.setFileUrl(work.getFileUrl());
                row.setFileName(work.getFileName());
                row.setCover(work.getCover());
                Signup signup = signups.get(work.getSignupId());
                if (signup != null) {
                    row.setAuthor(signup.getRealName());
                    row.setRealName(signup.getRealName());
                    row.setSchool(signup.getSchool());
                } else {
                    User user = userMapper.selectById(work.getUserId());
                    row.setAuthor(user == null ? "匿名选手" : user.getNickname());
                    row.setRealName(row.getAuthor());
                }
                row.setAvgScore(score.getAvgScore().setScale(2, RoundingMode.HALF_UP));
                row.setScoreCount(score.getScoreCount());
                rows.add(row);
            }
        }
        rows.sort(Comparator.comparing(RankRow::getAvgScore).reversed()
                .thenComparing(Comparator.comparingLong(RankRow::getScoreCount).reversed()));
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRank(i + 1);
        }

        List<Award> awards = awardMapper.selectList(new QueryWrapper<Award>()
                .eq("contest_id", contestId).orderByAsc("sort_order"));
        Map<Long, List<RankRow>> grouped = new HashMap<>();
        int cursor = 0;
        for (Award award : awards) {
            int taken = 0;
            while (cursor < rows.size() && taken < award.getQuota()) {
                RankRow row = rows.get(cursor);
                row.setAwardName(award.getName());
                row.setPrizeAmount(award.getPrizeAmount());
                row.setGift(award.getGift());
                grouped.computeIfAbsent(award.getId(), k -> new ArrayList<>()).add(row);
                cursor++;
                taken++;
            }
        }

        List<RankGroup> groups = new ArrayList<>();
        for (Award award : awards) {
            RankGroup group = new RankGroup();
            group.setAwardId(award.getId());
            group.setAwardName(award.getName());
            group.setQuota(award.getQuota());
            group.setPrizeAmount(award.getPrizeAmount());
            group.setGift(award.getGift());
            group.setItems(grouped.getOrDefault(award.getId(), List.of()));
            if (!group.getItems().isEmpty()) {
                groups.add(group);
            }
        }

        RankResult result = new RankResult();
        result.setContestId(contestId);
        result.setContestTitle(contest.getTitle());
        result.setStatus(contest.getStatus());
        result.setMode(contest.getMode());
        result.setPublished("PUBLISHED".equals(contest.getStatus()) || "FINISHED".equals(contest.getStatus()));
        result.setUpdatedAt(LocalDateTime.now());
        result.setRows(rows);
        result.setGroups(groups);
        result.setWorkCount(offline ? allSignups.size() : works.size());
        result.setJudgeCount(scoreMapper.judgeCount(contestId));
        result.setScoredCount(rows.size());
        return result;
    }

    private void cache(RankResult result) {
        try {
            cacheService.set(CACHE_PREFIX + result.getContestId(),
                    objectMapper.writeValueAsString(result), TTL);
        } catch (Exception e) {
            log.warn("榜单缓存写入失败：{}", e.getMessage());
        }
        StringRedisTemplate redis = cacheService.redis();
        if (redis == null) {
            return;
        }
        try {
            String zsetKey = ZSET_PREFIX + result.getContestId();
            redis.delete(zsetKey);
            for (RankRow row : result.getRows()) {
                redis.opsForZSet().add(zsetKey, String.valueOf(row.getWorkId()), row.getAvgScore().doubleValue());
            }
            redis.expire(zsetKey, TTL);
        } catch (Exception e) {
            cacheService.markRedisDown(e);
        }
    }

    public void evict(Long contestId) {
        cacheService.delete(CACHE_PREFIX + contestId);
        cacheService.delete(ZSET_PREFIX + contestId);
        cacheService.delete("rank:preview:" + contestId);
    }

    /** 公示时固化获奖结果，保证后续改分不影响已公示名单。 */
    @Transactional
    public void snapshotWinners(Long contestId) {
        Contest contest = contestMapper.selectById(contestId);
        boolean offline = contest != null && "OFFLINE".equals(contest.getMode());
        RankResult result = computeAndCache(contestId);
        winnerMapper.delete(new QueryWrapper<Winner>().eq("contest_id", contestId));

        // 批量预加载选手/作品/用户，避免逐条获奖记录产生 N+1 查询
        java.util.Set<Long> signupIds = new java.util.HashSet<>();
        java.util.Set<Long> workIds = new java.util.HashSet<>();
        for (RankGroup group : result.getGroups()) {
            for (RankRow row : group.getItems()) {
                if (offline && row.getSignupId() != null) {
                    signupIds.add(row.getSignupId());
                } else if (row.getWorkId() != null) {
                    workIds.add(row.getWorkId());
                }
            }
        }
        Map<Long, Signup> signupMap = signupIds.isEmpty() ? Map.of()
                : signupMapper.selectBatchIds(signupIds).stream()
                        .collect(Collectors.toMap(Signup::getId, Function.identity(), (a, b) -> a));
        Map<Long, Work> workMap = workIds.isEmpty() ? Map.of()
                : workMapper.selectBatchIds(workIds).stream()
                        .collect(Collectors.toMap(Work::getId, Function.identity(), (a, b) -> a));
        java.util.Set<Long> userIds = new java.util.HashSet<>();
        signupMap.values().forEach(s -> {
            if (s.getUserId() != null) userIds.add(s.getUserId());
        });
        workMap.values().forEach(w -> {
            if (w.getUserId() != null) userIds.add(w.getUserId());
        });
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        for (RankGroup group : result.getGroups()) {
            for (RankRow row : group.getItems()) {
                Winner winner = new Winner();
                winner.setContestId(contestId);
                winner.setAwardId(group.getAwardId());
                if (offline) {
                    winner.setSignupId(row.getSignupId());
                } else {
                    winner.setWorkId(row.getWorkId());
                }
                winner.setRankNo(row.getRank());
                winner.setPublished(true);
                winner.setCreatedAt(LocalDateTime.now());
                winnerMapper.insert(winner);

                User user = null;
                if (offline && row.getSignupId() != null) {
                    Signup signup = signupMap.get(row.getSignupId());
                    if (signup != null) {
                        user = userMap.get(signup.getUserId());
                    }
                } else if (row.getWorkId() != null) {
                    Work work = workMap.get(row.getWorkId());
                    if (work != null) {
                        user = userMap.get(work.getUserId());
                    }
                }
                if (user != null) {
                    emailService.winnerNotice(user.getEmail(), user.getNickname(),
                            result.getContestTitle(), group.getAwardName());
                }
            }
        }
    }

    /** 管理员手动调整排名：按奖项与名次覆盖公示获奖名单。 */
    @Transactional
    public void adminUpdateWinners(Long contestId, List<com.btr.platform.dto.Requests.WinnerEditReq> items) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BizException(404, "赛事不存在");
        }
        boolean offline = "OFFLINE".equals(contest.getMode());
        winnerMapper.delete(new QueryWrapper<Winner>().eq("contest_id", contestId));
        if (items != null) {
            for (com.btr.platform.dto.Requests.WinnerEditReq item : items) {
                Award award = awardMapper.selectById(item.awardId());
                if (award == null || !award.getContestId().equals(contestId)) {
                    throw new BizException("奖项不存在或不属于该赛事");
                }
                Winner winner = new Winner();
                winner.setContestId(contestId);
                winner.setAwardId(item.awardId());
                if (offline) {
                    Signup signup = signupMapper.selectById(item.subjectId());
                    if (signup == null || !signup.getContestId().equals(contestId)) {
                        throw new BizException("选手报名记录不存在");
                    }
                    winner.setSignupId(item.subjectId());
                } else {
                    Work work = workMapper.selectById(item.subjectId());
                    if (work == null || !work.getContestId().equals(contestId)) {
                        throw new BizException("作品不存在");
                    }
                    winner.setWorkId(item.subjectId());
                }
                winner.setRankNo(item.rankNo());
                winner.setPublished(true);
                winner.setCreatedAt(LocalDateTime.now());
                winnerMapper.insert(winner);
            }
        }
        evict(contestId);
        if ("PUBLISHED".equals(contest.getStatus()) || "FINISHED".equals(contest.getStatus())) {
            computeAndCache(contestId);
        }
    }

    /** 公示获奖名单；未公示赛事仅管理员可查看（避免名单提前泄露）。 */
    public List<Winner> winners(Long contestId, boolean admin) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BizException(404, "赛事不存在");
        }
        boolean published = "PUBLISHED".equals(contest.getStatus()) || "FINISHED".equals(contest.getStatus());
        if (!published && !admin) {
            throw new BizException(403, "榜单尚未公示");
        }
        return winnerMapper.selectList(new QueryWrapper<Winner>()
                .eq("contest_id", contestId).orderByAsc("rank_no"));
    }

    public Map<Long, Long> scoreCountByWork(Long contestId) {
        Map<Long, Long> map = new HashMap<>();
        for (WorkScore score : scoreMapper.avgByContest(contestId)) {
            map.put(score.getWorkId(), score.getScoreCount());
        }
        return map;
    }

    public long scoredWorks(Long contestId) {
        return scoreMapper.countScoredWorks(contestId);
    }
}
