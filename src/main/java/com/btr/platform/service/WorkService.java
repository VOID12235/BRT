package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btr.platform.common.BizException;
import com.btr.platform.common.PageResult;
import com.btr.platform.security.SecurityUtils;
import com.btr.platform.dto.Requests.WorkReq;
import com.btr.platform.dto.Views.WorkScore;
import com.btr.platform.dto.Views.WorkView;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.Signup;
import com.btr.platform.entity.UploadFile;
import com.btr.platform.entity.Work;
import com.btr.platform.mapper.ContestMapper;
import com.btr.platform.mapper.ScoreMapper;
import com.btr.platform.mapper.SignupMapper;
import com.btr.platform.mapper.UploadFileMapper;
import com.btr.platform.mapper.WorkMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkService {

    private final WorkMapper workMapper;
    private final SignupMapper signupMapper;
    private final ContestMapper contestMapper;
    private final ScoreMapper scoreMapper;
    private final UploadFileMapper uploadFileMapper;
    private final RankService rankService;

    public WorkService(WorkMapper workMapper, SignupMapper signupMapper, ContestMapper contestMapper,
                       ScoreMapper scoreMapper, UploadFileMapper uploadFileMapper, RankService rankService) {
        this.workMapper = workMapper;
        this.signupMapper = signupMapper;
        this.contestMapper = contestMapper;
        this.scoreMapper = scoreMapper;
        this.uploadFileMapper = uploadFileMapper;
        this.rankService = rankService;
    }

    @Transactional
    public WorkView submit(WorkReq req, Long userId) {
        Signup signup = signupMapper.selectById(req.signupId());
        if (signup == null || !signup.getUserId().equals(userId)) {
            throw new BizException(404, "报名记录不存在");
        }
        if (!"APPROVED".equals(signup.getStatus())) {
            throw new BizException("报名尚未通过审核，暂不能提交作品");
        }
        Contest contest = contestMapper.selectById(signup.getContestId());
        if (contest == null || !"REVIEW".equals(contest.getStatus())) {
            throw new BizException("作品提交仅在赛事「进行中」阶段开放，报名阶段请先完成报名，打分后作品将锁定");
        }
        if ("OFFLINE".equals(contest.getMode())) {
            throw new BizException("线下比赛不提交作品，报名后由评委按选手表现打分");
        }
        // 文件归属校验：fileUrl 必须是本人通过上传接口产生的文件，防止提交他人作品文件
        validateOwnFile(req.fileUrl(), userId);
        validateOwnFile(req.cover(), userId);
        Work work = workMapper.selectOne(new QueryWrapper<Work>()
                .eq("signup_id", req.signupId()).last("limit 1"));
        if (work != null && Boolean.TRUE.equals(work.getLocked())) {
            throw new BizException("作品已锁定，不可修改");
        }
        if (work == null) {
            work = new Work();
            work.setSignupId(req.signupId());
            work.setContestId(signup.getContestId());
            work.setUserId(userId);
            work.setSubmittedAt(LocalDateTime.now());
        }
        work.setTitle(req.title());
        work.setDescription(req.description());
        work.setFileUrl(req.fileUrl());
        work.setFileName(req.fileName());
        work.setCover(req.cover());
        work.setStatus("SUBMITTED");
        work.setLocked(false);
        work.setUpdatedAt(LocalDateTime.now());
        if (work.getId() == null) {
            workMapper.insert(work);
        } else {
            workMapper.updateById(work);
        }
        rankService.evict(work.getContestId());
        return toView(work, contest, signup, null);
    }

    @Transactional
    public void withdraw(Long id, Long userId) {
        Work work = workMapper.selectById(id);
        if (work == null || !work.getUserId().equals(userId)) {
            throw new BizException(404, "作品不存在");
        }
        if (Boolean.TRUE.equals(work.getLocked())) {
            throw new BizException("作品已锁定，不可撤回");
        }
        Contest contest = contestMapper.selectById(work.getContestId());
        if (contest == null || !(Set.of("SIGNUP", "REVIEW").contains(contest.getStatus()))) {
            throw new BizException("赛事当前不在投稿期，不可撤回作品");
        }
        work.setStatus("WITHDRAWN");
        work.setUpdatedAt(LocalDateTime.now());
        workMapper.updateById(work);
        rankService.evict(work.getContestId());
    }

    public List<WorkView> mine(Long userId) {
        List<Work> works = workMapper.selectList(new QueryWrapper<Work>()
                .eq("user_id", userId).orderByDesc("updated_at"));
        List<Long> contestIds = works.stream().map(Work::getContestId).distinct().toList();
        Map<Long, Contest> contests = contestIds.isEmpty() ? Map.of()
                : contestMapper.selectBatchIds(contestIds).stream()
                        .collect(Collectors.toMap(Contest::getId, Function.identity()));
        List<Long> signupIds = works.stream().map(Work::getSignupId).distinct().toList();
        Map<Long, Signup> signups = signupIds.isEmpty() ? Map.of()
                : signupMapper.selectBatchIds(signupIds).stream()
                        .collect(Collectors.toMap(Signup::getId, Function.identity()));
        Map<Long, WorkScore> scores = new java.util.HashMap<>();
        List<Long> workIds = works.stream().map(Work::getId).toList();
        if (!workIds.isEmpty()) {
            scoreMapper.avgByWorkIds(workIds).forEach(score -> scores.put(score.getWorkId(), score));
        }
        return works.stream().map(w -> toView(w, contests.get(w.getContestId()),
                signups.get(w.getSignupId()), scores.get(w.getId()))).toList();
    }

    public PageResult<WorkView> publicList(Long contestId, long page, long size) {
        QueryWrapper<Work> query = new QueryWrapper<Work>().eq("status", "SUBMITTED");
        // 管理员 / 评委在评审各阶段均可查看作品（不含分数）；公众仅在已公示 / 已结束且公开赛事中可见，
        // 避免评审阶段作品提前泄露给选手与其他用户
        var current = SecurityUtils.currentOrNull();
        boolean privileged = current != null && ("ADMIN".equals(current.role()) || "JUDGE".equals(current.role()));
        if (!privileged) {
            query.inSql("contest_id",
                    "SELECT id FROM contest WHERE status IN ('PUBLISHED','FINISHED') AND is_public = TRUE");
        }
        if (contestId != null) {
            query.eq("contest_id", contestId);
        }
        query.orderByDesc("submitted_at");
        Page<Work> result = workMapper.selectPage(new Page<>(page, size), query);
        return PageResult.of(enrich(result.getRecords()), result.getTotal(), page, size);
    }

    public PageResult<WorkView> adminList(Long contestId, String keyword, long page, long size) {
        QueryWrapper<Work> query = new QueryWrapper<>();
        if (contestId != null) {
            query.eq("contest_id", contestId);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.and(w -> w.like("title", keyword).or().like("description", keyword));
        }
        query.orderByDesc("submitted_at");
        Page<Work> result = workMapper.selectPage(new Page<>(page, size), query);
        return PageResult.of(enrich(result.getRecords()), result.getTotal(), page, size);
    }

    private List<WorkView> enrich(List<Work> works) {
        if (works.isEmpty()) {
            return List.of();
        }
        List<Long> contestIds = works.stream()
                .map(Work::getContestId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, Contest> contests = contestIds.isEmpty() ? Map.of()
                : contestMapper.selectBatchIds(contestIds).stream()
                        .collect(Collectors.toMap(Contest::getId, Function.identity()));
        List<Long> signupIds = works.stream()
                .map(Work::getSignupId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, Signup> signups = signupIds.isEmpty() ? Map.of()
                : signupMapper.selectBatchIds(signupIds).stream()
                        .collect(Collectors.toMap(Signup::getId, Function.identity()));
        Map<Long, WorkScore> scores = new java.util.HashMap<>();
        List<Long> workIds = works.stream().map(Work::getId).toList();
        if (!workIds.isEmpty()) {
            scoreMapper.avgByWorkIds(workIds).forEach(score -> scores.put(score.getWorkId(), score));
        }
        return works.stream().map(w -> toView(w, contests.get(w.getContestId()),
                signups.get(w.getSignupId()), scores.get(w.getId()))).toList();
    }

    @Transactional
    public void lock(Long id, boolean locked) {
        Work work = workMapper.selectById(id);
        if (work == null) {
            throw new BizException(404, "作品不存在");
        }
        work.setLocked(locked);
        work.setUpdatedAt(LocalDateTime.now());
        workMapper.updateById(work);
    }

    public Work getOrThrow(Long id) {
        Work work = workMapper.selectById(id);
        if (work == null) {
            throw new BizException(404, "作品不存在");
        }
        return work;
    }

    /**
     * 校验文件 URL 归属：作品文件/封面必须是当前用户本人上传（upload_file.uploader_id）。
     * 仅接受平台上传接口生成的文件地址，拒绝任意外部 URL。
     */
    private void validateOwnFile(String url, Long userId) {
        if (url == null || url.isBlank()) {
            return;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^/api/v1/files/(\\d+)/[0-9a-fA-F]{32}$").matcher(url);
        if (!m.matches()) {
            throw new BizException("文件地址无效，请重新上传");
        }
        Long fileId = Long.valueOf(m.group(1));
        UploadFile file = uploadFileMapper.selectById(fileId);
        if (file == null || !userId.equals(file.getUploaderId())) {
            throw new BizException("只能使用本人上传的文件");
        }
    }

    private WorkView toView(Work work, Contest contest, Signup signup, WorkScore score) {
        WorkView view = new WorkView();
        view.setId(work.getId());
        view.setContestId(work.getContestId());
        view.setContestTitle(contest == null ? "" : contest.getTitle());
        view.setSignupId(work.getSignupId());
        view.setUserId(work.getUserId());
        view.setAuthor(signup == null ? "" : signup.getRealName());
        view.setSchool(signup == null ? "" : signup.getSchool());
        view.setTitle(work.getTitle());
        view.setDescription(work.getDescription());
        view.setFileUrl(work.getFileUrl());
        view.setFileName(work.getFileName());
        view.setCover(work.getCover());
        view.setStatus(work.getStatus());
        view.setLocked(work.getLocked());
        view.setSubmittedAt(work.getSubmittedAt());
        if (score != null) {
            view.setScoreCount(score.getScoreCount() == null ? 0 : score.getScoreCount());
            view.setAvgScore(score.getAvgScore());
        }
        return view;
    }
}
