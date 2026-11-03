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
import java.util.Set;

@Service
public class ContestService {

    public static final Set<String> STATUSES = Set.of("DRAFT", "SIGNUP", "REVIEW", "PUBLISHED", "FINISHED");
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
            query.eq("is_public", true);
        }
        if (status != null && !status.isBlank()) {
            query.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.and(w -> w.like("title", keyword).or().like("summary", keyword));
        }
        query.orderByDesc("created_at");
        Page<Contest> result = contestMapper.selectPage(new Page<>(page, size), query);
        List<ContestCard> cards = result.getRecords().stream().map(c -> toCard(c, userId)).toList();
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
        apply(contest, req);
        if (req.isPublic() != null) {
            contest.setIsPublic(req.isPublic());
        }
        if (contest.getStatus() != null) {
            validateStatus(contest.getStatus());
        }
        contest.setUpdatedAt(LocalDateTime.now());
        contestMapper.updateById(contest);
        return contest;
    }

    @Transactional
    public Contest changeStatus(Long id, String status) {
        validateStatus(status);
        Contest contest = getOrThrow(id);
        contest.setStatus(status);
        contest.setUpdatedAt(LocalDateTime.now());
        contestMapper.updateById(contest);
        if ("REVIEW".equals(status) && "ONLINE".equals(contest.getMode())) {
            Work lock = new Work();
            lock.setLocked(true);
            workMapper.update(lock, new QueryWrapper<Work>().eq("contest_id", id));
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
        ContestCard card = new ContestCard();
        card.setId(contest.getId());
        card.setTitle(contest.getTitle());
        card.setCover(contest.getCover());
        card.setSummary(contest.getSummary());
        card.setDescription(contest.getDescription());
        card.setRules(contest.getRules());
        card.setPrizeAmount(contest.getPrizeAmount());
        card.setMode(contest.getMode() == null ? "ONLINE" : contest.getMode());
        card.setSignupStart(contest.getSignupStart());
        card.setSignupEnd(contest.getSignupEnd());
        card.setReviewStart(contest.getReviewStart());
        card.setReviewEnd(contest.getReviewEnd());
        card.setStatus(contest.getStatus());
        card.setIsPublic(contest.getIsPublic());
        card.setCreatedAt(contest.getCreatedAt());
        card.setSignupCount(signupMapper.selectCount(new QueryWrapper<Signup>()
                .eq("contest_id", contest.getId()).ne("status", "CANCELED")));
        card.setWorkCount(workMapper.selectCount(new QueryWrapper<Work>()
                .eq("contest_id", contest.getId()).eq("status", "SUBMITTED")));
        return card;
    }
}
