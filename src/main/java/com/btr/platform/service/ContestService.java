package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btr.platform.common.BizException;
import com.btr.platform.common.PageResult;
import com.btr.platform.dto.Requests.AwardReq;
import com.btr.platform.dto.Requests.ContestReq;
import com.btr.platform.dto.Views.ContestCard;
import com.btr.platform.dto.Views.ContestDetail;
import com.btr.platform.entity.Award;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.Signup;
import com.btr.platform.entity.Work;
import com.btr.platform.mapper.AwardMapper;
import com.btr.platform.mapper.ContestMapper;
import com.btr.platform.mapper.ScoreMapper;
import com.btr.platform.mapper.SignupMapper;
import com.btr.platform.mapper.WorkMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ContestService {

    public static final Set<String> STATUSES = Set.of("DRAFT", "SIGNUP", "REVIEW", "JUDGING", "FINISHED", "PUBLISHED");
    public static final Set<String> MODES = Set.of("ONLINE", "OFFLINE");

    private final ContestMapper contestMapper;
    private final AwardMapper awardMapper;
    private final SignupMapper signupMapper;
    private final WorkMapper workMapper;
    private final ScoreMapper scoreMapper;
    private final RankService rankService;

    public ContestService(ContestMapper contestMapper, AwardMapper awardMapper, SignupMapper signupMapper,
                          WorkMapper workMapper, ScoreMapper scoreMapper, RankService rankService) {
        this.contestMapper = contestMapper;
        this.awardMapper = awardMapper;
        this.signupMapper = signupMapper;
        this.workMapper = workMapper;
        this.scoreMapper = scoreMapper;
        this.rankService = rankService;
    }

    public PageResult<ContestCard> list(String status, String keyword, long page, long size,
                                        boolean includeAll, Long userId) {
        QueryWrapper<Contest> query = new QueryWrapper<>();
        if (!includeAll) {
            query.eq("is_public", true).ne("status", "DRAFT");
        }
        if (status != null && !status.isBlank()) {
            query.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.and(w -> w.like("title", keyword).or().like("summary", keyword));
        }
        query.orderByDesc("created_at");
        Page<Contest> result = contestMapper.selectPage(new Page<>(page, size), query);
        List<ContestCard> cards = toCards(result.getRecords());
        return PageResult.of(cards, result.getTotal(), page, size);
    }

    public ContestDetail detail(Long id, Long userId, boolean includeAll) {
        Contest contest = contestMapper.selectById(id);
        if (contest == null || (!Boolean.TRUE.equals(contest.getIsPublic()) && !includeAll)) {
            throw new BizException(404, "赛事不存在或未公开");
        }
        ContestDetail detail = new ContestDetail();
        detail.setContest(toCard(contest, userId));
        detail.setAwards(awardMapper.selectList(new QueryWrapper<Award>()
                .eq("contest_id", id).orderByAsc("sort_order")));
        detail.setJudgeCount(scoreMapper.judgeCount(id));
        if (userId != null) {
            Signup signup = signupMapper.selectOne(new QueryWrapper<Signup>()
                    .eq("contest_id", id).eq("user_id", userId).last("limit 1"));
            detail.setMySignup(signup);
            if (signup != null) {
                detail.setMyWork(workMapper.selectOne(new QueryWrapper<Work>()
                        .eq("signup_id", signup.getId()).last("limit 1")));
            }
        }
        return detail;
    }

    public Contest getOrThrow(Long id) {
        Contest contest = contestMapper.selectById(id);
        if (contest == null) {
            throw new BizException(404, "赛事不存在");
        }
        return contest;
    }

    @Transactional
    public Contest create(ContestReq req) {
        Contest contest = new Contest();
        apply(contest, req);
        if (contest.getStatus() == null) {
            contest.setStatus("DRAFT");
        }
        validateStatus(contest.getStatus());
        contest.setIsPublic(req.isPublic() == null ? Boolean.TRUE : req.isPublic());
        contest.setCreatedAt(LocalDateTime.now());
        contest.setUpdatedAt(LocalDateTime.now());
        contestMapper.insert(contest);
        return contest;
    }

    @Transactional
    public Contest update(Long id, ContestReq req) {
        Contest contest = getOrThrow(id);
        String newStatus = req.status();
        // 状态变更必须走 changeStatus，以触发锁定/解锁/获奖快照等副作用，防止绕过状态机
        if (newStatus != null && !newStatus.isBlank() && !newStatus.equals(contest.getStatus())) {
            changeStatus(id, newStatus);
            contest = getOrThrow(id);
        }
        apply(contest, req);
        if (req.isPublic() != null) {
            contest.setIsPublic(req.isPublic());
        }
        contest.setUpdatedAt(LocalDateTime.now());
        contestMapper.updateById(contest);
        return contest;
    }

    /** 允许的状态流转；同状态视为幂等。禁止跨阶段跳跃（如 DRAFT 直接 PUBLISHED）。
     *  业务流：报名 → 进行中（可提交/修改作品）→ 打分中（评委评审，作品锁定）→ 已结束（作品锁定，公布结果）→ 已公示（榜单+证书固化） */
    private static final java.util.Map<String, java.util.Set<String>> TRANSITIONS = java.util.Map.of(
            "DRAFT", java.util.Set.of("SIGNUP"),
            "SIGNUP", java.util.Set.of("DRAFT", "REVIEW"),
            "REVIEW", java.util.Set.of("SIGNUP", "JUDGING", "FINISHED"),
            "JUDGING", java.util.Set.of("REVIEW", "FINISHED"),
            "PUBLISHED", java.util.Set.of("FINISHED"),
            "FINISHED", java.util.Set.of("PUBLISHED"));

    private void assertTransition(String from, String to) {
        if (from == null || from.equals(to)) {
            return;
        }
        java.util.Set<String> allowed = TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new BizException("非法状态流转：" + from + " → " + to);
        }
    }

    @Transactional
    public Contest changeStatus(Long id, String status) {
        validateStatus(status);
        Contest contest = getOrThrow(id);
        assertTransition(contest.getStatus(), status);
        String from = contest.getStatus();
        contest.setStatus(status);
        contest.setUpdatedAt(LocalDateTime.now());
        contestMapper.updateById(contest);
        // 进入「进行中」不再锁定作品：选手在比赛进行中可反复提交/修改作品（仅锁定已结束的）
        // 进入「打分中」：锁定全部作品，评委开始评审，选手不可再修改
        // 进入「已结束」：锁定全部作品，作品进入评审收尾阶段不可再改
        if (("JUDGING".equals(status) || "FINISHED".equals(status))
                && !"JUDGING".equals(from) && !"FINISHED".equals(from)
                && "ONLINE".equals(contest.getMode())) {
            Work lock = new Work();
            lock.setLocked(true);
            workMapper.update(lock, new QueryWrapper<Work>().eq("contest_id", id));
        }
        // 评审回退到报名：解除作品锁定，允许选手继续修改
        if ("SIGNUP".equals(status) && "REVIEW".equals(from)) {
            Work unlock = new Work();
            unlock.setLocked(false);
            workMapper.update(unlock, new QueryWrapper<Work>().eq("contest_id", id));
        }
        // 已结束回退到进行中：解除作品锁定，允许选手继续修改
        if ("REVIEW".equals(status) && "FINISHED".equals(from)) {
            Work unlock = new Work();
            unlock.setLocked(false);
            workMapper.update(unlock, new QueryWrapper<Work>().eq("contest_id", id));
        }
        // 打分中回退到进行中：解除作品锁定，允许选手继续修改
        if ("REVIEW".equals(status) && "JUDGING".equals(from)) {
            Work unlock = new Work();
            unlock.setLocked(false);
            workMapper.update(unlock, new QueryWrapper<Work>().eq("contest_id", id));
        }
        if ("PUBLISHED".equals(status)) {
            rankService.snapshotWinners(id);
        }
        return contest;
    }

    @Transactional
    public void delete(Long id) {
        long signups = signupMapper.selectCount(new QueryWrapper<Signup>().eq("contest_id", id));
        if (signups > 0) {
            throw new BizException("该赛事已有报名记录，不能删除，可改为下架或结束");
        }
        contestMapper.deleteById(id);
        awardMapper.delete(new QueryWrapper<Award>().eq("contest_id", id));
    }

    public List<Award> awards(Long contestId) {
        return awardMapper.selectList(new QueryWrapper<Award>()
                .eq("contest_id", contestId).orderByAsc("sort_order"));
    }

    @Transactional
    public Award addAward(Long contestId, AwardReq req) {
        getOrThrow(contestId);
        Award award = new Award();
        award.setContestId(contestId);
        award.setName(req.name());
        award.setQuota(req.quota());
        award.setPrizeAmount(req.prizeAmount());
        award.setGift(req.gift());
        award.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        awardMapper.insert(award);
        rankService.evict(contestId);
        return award;
    }

    @Transactional
    public Award updateAward(Long contestId, Long awardId, AwardReq req) {
        Award award = awardMapper.selectById(awardId);
        if (award == null || !award.getContestId().equals(contestId)) {
            throw new BizException(404, "奖项不存在");
        }
        award.setName(req.name());
        award.setQuota(req.quota());
        award.setPrizeAmount(req.prizeAmount());
        award.setGift(req.gift());
        award.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        awardMapper.updateById(award);
        rankService.evict(contestId);
        return award;
    }

    @Transactional
    public void deleteAward(Long contestId, Long awardId) {
        Award award = awardMapper.selectById(awardId);
        if (award == null || !award.getContestId().equals(contestId)) {
            throw new BizException(404, "奖项不存在");
        }
        awardMapper.deleteById(awardId);
        rankService.evict(contestId);
    }

    private void apply(Contest contest, ContestReq req) {
        contest.setTitle(req.title());
        contest.setCover(req.cover());
        contest.setSummary(req.summary());
        contest.setDescription(req.description());
        contest.setRules(req.rules());
        contest.setPrizeAmount(req.prizeAmount());
        if (req.mode() != null && !req.mode().isBlank()) {
            if (!MODES.contains(req.mode())) {
                throw new BizException("非法比赛类型：" + req.mode());
            }
            contest.setMode(req.mode());
        } else if (contest.getMode() == null) {
            contest.setMode("ONLINE");
        }
        contest.setSignupStart(req.signupStart());
        contest.setSignupEnd(req.signupEnd());
        contest.setReviewStart(req.reviewStart());
        contest.setReviewEnd(req.reviewEnd());
        // 视频字段：仅在有值时写入，避免普通编辑覆盖清空
        if (req.videoUrl() != null) {
            contest.setVideoUrl(req.videoUrl().isBlank() ? null : req.videoUrl());
        }
        if (req.videoName() != null) {
            contest.setVideoName(req.videoName().isBlank() ? null : req.videoName());
        }
        if (req.status() != null && !req.status().isBlank()) {
            contest.setStatus(req.status());
        }
    }

    private void validateStatus(String status) {
        if (status == null || !STATUSES.contains(status)) {
            throw new BizException("非法赛事状态：" + status);
        }
    }

    public ContestCard toCard(Contest contest, Long userId) {
        long signupCount = signupMapper.selectCount(new QueryWrapper<Signup>()
                .eq("contest_id", contest.getId()).ne("status", "CANCELED"));
        long workCount = workMapper.selectCount(new QueryWrapper<Work>()
                .eq("contest_id", contest.getId()).eq("status", "SUBMITTED"));
        return toCard(contest, signupCount, workCount);
    }

    /** 批量组装赛事卡片，避免列表页逐条统计产生 N+1 查询。 */
    private List<ContestCard> toCards(List<Contest> contests) {
        if (contests.isEmpty()) {
            return List.of();
        }
        List<Long> ids = contests.stream().map(Contest::getId).toList();
        Map<Long, Long> signupCounts = parseCounts(signupMapper.selectMaps(new QueryWrapper<Signup>()
                .select("contest_id", "COUNT(*) AS cnt")
                .in("contest_id", ids).ne("status", "CANCELED").groupBy("contest_id")));
        Map<Long, Long> workCounts = parseCounts(workMapper.selectMaps(new QueryWrapper<Work>()
                .select("contest_id", "COUNT(*) AS cnt")
                .in("contest_id", ids).eq("status", "SUBMITTED").groupBy("contest_id")));
        return contests.stream()
                .map(c -> toCard(c, signupCounts.getOrDefault(c.getId(), 0L),
                        workCounts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    private Map<Long, Long> parseCounts(List<Map<String, Object>> rows) {
        Map<Long, Long> counts = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            Long contestId = numeric(row, "contestid");
            Long count = numeric(row, "cnt");
            if (contestId != null) {
                counts.put(contestId, count == null ? 0L : count);
            }
        }
        return counts;
    }

    private Long numeric(Map<String, Object> row, String hint) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey().replace("_", "").toLowerCase(java.util.Locale.ROOT);
            if (key.equals(hint) && entry.getValue() instanceof Number number) {
                return number.longValue();
            }
        }
        return null;
    }

    private ContestCard toCard(Contest contest, long signupCount, long workCount) {
        ContestCard card = new ContestCard();
        card.setId(contest.getId());
        card.setTitle(contest.getTitle());
        card.setCover(contest.getCover());
        card.setSummary(contest.getSummary());
        card.setDescription(contest.getDescription());
        card.setRules(contest.getRules());
        card.setPrizeAmount(contest.getPrizeAmount());
        card.setMode(contest.getMode() == null ? "ONLINE" : contest.getMode());
        card.setVideoUrl(contest.getVideoUrl());
        card.setVideoName(contest.getVideoName());
        card.setSignupStart(contest.getSignupStart());
        card.setSignupEnd(contest.getSignupEnd());
        card.setReviewStart(contest.getReviewStart());
        card.setReviewEnd(contest.getReviewEnd());
        card.setStatus(contest.getStatus());
        card.setIsPublic(contest.getIsPublic());
        card.setCreatedAt(contest.getCreatedAt());
        card.setSignupCount(signupCount);
        card.setWorkCount(workCount);
        return card;
    }
}
