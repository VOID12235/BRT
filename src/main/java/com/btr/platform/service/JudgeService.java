package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.common.BizException;
import com.btr.platform.dto.Requests.AssignReq;
import com.btr.platform.dto.Requests.ScoreReq;
import com.btr.platform.dto.Views.AssignView;
import com.btr.platform.dto.Views.JudgeProgress;
import com.btr.platform.dto.Views.JudgeTask;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.JudgeAssign;
import com.btr.platform.entity.Score;
import com.btr.platform.entity.Signup;
import com.btr.platform.entity.User;
import com.btr.platform.entity.Work;
import com.btr.platform.mapper.ContestMapper;
import com.btr.platform.mapper.JudgeAssignMapper;
import com.btr.platform.mapper.ScoreMapper;
import com.btr.platform.mapper.SignupMapper;
import com.btr.platform.mapper.UserMapper;
import com.btr.platform.mapper.WorkMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JudgeService {

    private final JudgeAssignMapper assignMapper;
    private final WorkMapper workMapper;
    private final ContestMapper contestMapper;
    private final SignupMapper signupMapper;
    private final ScoreMapper scoreMapper;
    private final UserMapper userMapper;
    private final RankService rankService;

    public JudgeService(JudgeAssignMapper assignMapper, WorkMapper workMapper, ContestMapper contestMapper,
                        SignupMapper signupMapper, ScoreMapper scoreMapper, UserMapper userMapper,
                        RankService rankService) {
        this.assignMapper = assignMapper;
        this.workMapper = workMapper;
        this.contestMapper = contestMapper;
        this.signupMapper = signupMapper;
        this.scoreMapper = scoreMapper;
        this.userMapper = userMapper;
        this.rankService = rankService;
    }

    public List<JudgeTask> tasks(Long judgeId, Long contestId) {
        QueryWrapper<JudgeAssign> query = new QueryWrapper<JudgeAssign>().eq("judge_id", judgeId);
        if (contestId != null) {
            query.eq("contest_id", contestId);
        }
        query.orderByDesc("contest_id");
        List<JudgeAssign> assigns = assignMapper.selectList(query);
        if (assigns.isEmpty()) {
            return List.of();
        }
        List<Long> workIds = assigns.stream().map(JudgeAssign::getWorkId).distinct().toList();
        Map<Long, Work> works = workMapper.selectBatchIds(workIds).stream()
                .collect(Collectors.toMap(Work::getId, Function.identity()));
        Map<Long, Contest> contests = contestMapper.selectBatchIds(assigns.stream()
                        .map(JudgeAssign::getContestId).distinct().toList()).stream()
                .collect(Collectors.toMap(Contest::getId, Function.identity()));
        Map<Long, Signup> signups = signupMapper.selectBatchIds(works.values().stream()
                        .map(Work::getSignupId).distinct().toList()).stream()
                .collect(Collectors.toMap(Signup::getId, Function.identity()));
        Map<Long, Score> scores = scoreMapper.selectList(new QueryWrapper<Score>()
                        .eq("judge_id", judgeId).in("work_id", workIds)).stream()
                .collect(Collectors.toMap(Score::getWorkId, Function.identity(), (a, b) -> a));

        List<JudgeTask> tasks = new ArrayList<>();
        for (JudgeAssign assign : assigns) {
            Work work = works.get(assign.getWorkId());
            if (work == null || !"SUBMITTED".equals(work.getStatus())) {
                continue;
            }
            JudgeTask task = new JudgeTask();
            task.setAssignId(assign.getId());
            task.setWorkId(work.getId());
            task.setWorkTitle(work.getTitle());
            task.setWorkDescription(work.getDescription());
            task.setFileUrl(work.getFileUrl());
            task.setFileName(work.getFileName());
            task.setSubmittedAt(work.getSubmittedAt());
            task.setContestId(assign.getContestId());
            Contest contest = contests.get(assign.getContestId());
            task.setContestTitle(contest == null ? "" : contest.getTitle());
            Signup signup = signups.get(work.getSignupId());
            task.setRealName(signup == null ? "" : signup.getRealName());
            task.setSchool(signup == null ? "" : signup.getSchool());
            Score score = scores.get(work.getId());
            if (score != null) {
                task.setScoreId(score.getId());
                task.setScore(score.getScore());
                task.setComment(score.getComment());
            }
            tasks.add(task);
        }
        tasks.sort(Comparator.comparing(JudgeTask::getContestId).reversed()
                .thenComparing(JudgeTask::getWorkId));
        return tasks;
    }

    public List<JudgeProgress> progress(Long judgeId) {
        List<JudgeAssign> assigns = assignMapper.selectList(new QueryWrapper<JudgeAssign>()
                .eq("judge_id", judgeId));
        if (assigns.isEmpty()) {
            return List.of();
        }
        Set<Long> scored = scoreMapper.selectList(new QueryWrapper<Score>()
                        .eq("judge_id", judgeId)).stream()
                .map(Score::getWorkId).collect(Collectors.toSet());
        Map<Long, List<JudgeAssign>> byContest = assigns.stream()
                .collect(Collectors.groupingBy(JudgeAssign::getContestId));
        Map<Long, Contest> contests = contestMapper.selectBatchIds(byContest.keySet()).stream()
                .collect(Collectors.toMap(Contest::getId, Function.identity()));
        List<JudgeProgress> list = new ArrayList<>();
        for (Map.Entry<Long, List<JudgeAssign>> entry : byContest.entrySet()) {
            JudgeProgress progress = new JudgeProgress();
            progress.setContestId(entry.getKey());
            Contest contest = contests.get(entry.getKey());
            progress.setContestTitle(contest == null ? "" : contest.getTitle());
            progress.setAssigned(entry.getValue().size());
            progress.setScored(entry.getValue().stream()
                    .filter(a -> scored.contains(a.getWorkId())).count());
            list.add(progress);
        }
        list.sort(Comparator.comparing(JudgeProgress::getContestId).reversed());
        return list;
    }

    public List<Contest> contests(Long judgeId) {
        Set<Long> ids = assignMapper.selectList(new QueryWrapper<JudgeAssign>()
                        .eq("judge_id", judgeId)).stream()
                .map(JudgeAssign::getContestId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return List.of();
        }
        return contestMapper.selectBatchIds(ids).stream()
                .sorted(Comparator.comparing(Contest::getId).reversed()).toList();
    }

    /** 评委端项目视图：按赛事聚合分配与评分进度 */
    public List<com.btr.platform.dto.Views.JudgeProject> projects(Long judgeId) {
        List<JudgeTask> tasks = tasks(judgeId, null);
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, List<JudgeTask>> byContest = tasks.stream()
                .collect(Collectors.groupingBy(JudgeTask::getContestId));
        Map<Long, Contest> contests = contestMapper.selectBatchIds(byContest.keySet()).stream()
                .collect(Collectors.toMap(Contest::getId, Function.identity()));
        List<com.btr.platform.dto.Views.JudgeProject> list = new ArrayList<>();
        for (Map.Entry<Long, List<JudgeTask>> entry : byContest.entrySet()) {
            List<JudgeTask> items = entry.getValue();
            Contest contest = contests.get(entry.getKey());
            com.btr.platform.dto.Views.JudgeProject project = new com.btr.platform.dto.Views.JudgeProject();
            project.setContestId(entry.getKey());
            project.setContestTitle(contest == null ? "" : contest.getTitle());
            project.setContestStatus(contest == null ? "" : contest.getStatus());
            project.setCover(contest == null ? null : contest.getCover());
            project.setAssigned(items.size());
            long scored = items.stream().filter(t -> t.getScore() != null).count();
            project.setScored(scored);
            project.setPending(items.size() - scored);
            list.add(project);
        }
        list.sort(Comparator.comparing(com.btr.platform.dto.Views.JudgeProject::getContestId).reversed());
        return list;
    }

    /** 管理端项目级分配视图：按「评委 × 赛事」聚合 */
    public List<com.btr.platform.dto.Views.AdminAssignProject> adminProjects(Long contestId) {
        QueryWrapper<JudgeAssign> query = new QueryWrapper<>();
        if (contestId != null) {
            query.eq("contest_id", contestId);
        }
        List<JudgeAssign> assigns = assignMapper.selectList(query.orderByDesc("created_at"));
        if (assigns.isEmpty()) {
            return List.of();
        }
        Map<Long, User> judges = userMapper.selectBatchIds(assigns.stream()
                        .map(JudgeAssign::getJudgeId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Contest> contests = contestMapper.selectBatchIds(assigns.stream()
                        .map(JudgeAssign::getContestId).distinct().toList()).stream()
                .collect(Collectors.toMap(Contest::getId, Function.identity()));
        Set<String> scoredWorkIds = scoreMapper.selectList(new QueryWrapper<Score>()
                        .in("judge_id", assigns.stream().map(JudgeAssign::getJudgeId).distinct().toList())
                        .in("work_id", assigns.stream().map(JudgeAssign::getWorkId).distinct().toList())).stream()
                .map(s -> s.getJudgeId() + ":" + s.getWorkId()).collect(Collectors.toSet());

        Map<String, com.btr.platform.dto.Views.AdminAssignProject> grouped = new java.util.LinkedHashMap<>();
        for (JudgeAssign assign : assigns) {
            String key = assign.getJudgeId() + ":" + assign.getContestId();
            com.btr.platform.dto.Views.AdminAssignProject project =
                    grouped.computeIfAbsent(key, k -> {
                        com.btr.platform.dto.Views.AdminAssignProject p =
                                new com.btr.platform.dto.Views.AdminAssignProject();
                        p.setJudgeId(assign.getJudgeId());
                        User judge = judges.get(assign.getJudgeId());
                        p.setJudgeName(judge == null ? "" : judge.getNickname());
                        p.setContestId(assign.getContestId());
                        Contest contest = contests.get(assign.getContestId());
                        p.setContestTitle(contest == null ? "" : contest.getTitle());
                        p.setContestStatus(contest == null ? "" : contest.getStatus());
                        p.setCreatedAt(assign.getCreatedAt());
                        p.setAssignIds(new ArrayList<>());
                        return p;
                    });
            project.setWorkCount(project.getWorkCount() + 1);
            if (scoredWorkIds.contains(assign.getJudgeId() + ":" + assign.getWorkId())) {
                project.setScoredCount(project.getScoredCount() + 1);
            }
            project.getAssignIds().add(assign.getId());
        }
        return new ArrayList<>(grouped.values());
    }

    @Transactional
    public Score score(ScoreReq req, Long judgeId) {
        JudgeAssign assign = assignMapper.selectOne(new QueryWrapper<JudgeAssign>()
                .eq("judge_id", judgeId).eq("work_id", req.workId()).last("limit 1"));
        if (assign == null) {
            throw new BizException(403, "该作品未分配给你评审");
        }
        Contest contest = contestMapper.selectById(assign.getContestId());
        if (contest == null || !"REVIEW".equals(contest.getStatus())) {
            throw new BizException("当前不在评审阶段，无法打分");
        }
        Work work = workMapper.selectById(req.workId());
        if (work == null || !"SUBMITTED".equals(work.getStatus())) {
            throw new BizException("作品不可评审");
        }
        Score score = scoreMapper.selectOne(new QueryWrapper<Score>()
                .eq("judge_id", judgeId).eq("work_id", req.workId()).last("limit 1"));
        boolean create = score == null;
        if (create) {
            score = new Score();
            score.setContestId(assign.getContestId());
            score.setWorkId(req.workId());
            score.setJudgeId(judgeId);
            score.setCreatedAt(LocalDateTime.now());
        }
        score.setScore(req.score());
        score.setComment(req.comment());
        score.setUpdatedAt(LocalDateTime.now());
        if (create) {
            scoreMapper.insert(score);
        } else {
            scoreMapper.updateById(score);
        }
        rankService.evict(assign.getContestId());
        return score;
    }

    @Transactional
    public void assign(AssignReq req) {
        User judge = userMapper.selectById(req.judgeId());
        if (judge == null || !"JUDGE".equals(judge.getRole())) {
            throw new BizException("请选择评委账号");
        }
        Contest contest = contestMapper.selectById(req.contestId());
        if (contest == null) {
            throw new BizException(404, "赛事不存在");
        }
        List<Work> works;
        if (req.workIds() == null || req.workIds().isEmpty()) {
            works = workMapper.selectList(new QueryWrapper<Work>()
                    .eq("contest_id", req.contestId()).eq("status", "SUBMITTED"));
        } else {
            works = workMapper.selectList(new QueryWrapper<Work>()
                    .eq("contest_id", req.contestId()).in("id", req.workIds()));
        }
        for (Work work : works) {
            long exists = assignMapper.selectCount(new QueryWrapper<JudgeAssign>()
                    .eq("judge_id", req.judgeId()).eq("work_id", work.getId()));
            if (exists == 0) {
                JudgeAssign assign = new JudgeAssign();
                assign.setContestId(req.contestId());
                assign.setJudgeId(req.judgeId());
                assign.setWorkId(work.getId());
                assign.setCreatedAt(LocalDateTime.now());
                assignMapper.insert(assign);
            }
        }
        rankService.evict(req.contestId());
    }

    @Transactional
    public void unassign(Long assignId) {
        JudgeAssign assign = assignMapper.selectById(assignId);
        if (assign == null) {
            throw new BizException(404, "分配记录不存在");
        }
        assignMapper.deleteById(assignId);
        rankService.evict(assign.getContestId());
    }

    public List<AssignView> assignments(Long contestId) {
        QueryWrapper<JudgeAssign> query = new QueryWrapper<>();
        if (contestId != null) {
            query.eq("contest_id", contestId);
        }
        List<JudgeAssign> assigns = assignMapper.selectList(query.orderByDesc("created_at"));
        if (assigns.isEmpty()) {
            return List.of();
        }
        Map<Long, User> judges = userMapper.selectBatchIds(assigns.stream()
                        .map(JudgeAssign::getJudgeId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Work> works = workMapper.selectBatchIds(assigns.stream()
                        .map(JudgeAssign::getWorkId).distinct().toList()).stream()
                .collect(Collectors.toMap(Work::getId, Function.identity()));
        Map<Long, Contest> contests = contestMapper.selectBatchIds(assigns.stream()
                        .map(JudgeAssign::getContestId).distinct().toList()).stream()
                .collect(Collectors.toMap(Contest::getId, Function.identity()));
        return assigns.stream().map(a -> {
            AssignView view = new AssignView();
            view.setId(a.getId());
            view.setContestId(a.getContestId());
            Contest contest = contests.get(a.getContestId());
            view.setContestTitle(contest == null ? "" : contest.getTitle());
            view.setJudgeId(a.getJudgeId());
            User judge = judges.get(a.getJudgeId());
            view.setJudgeName(judge == null ? "" : judge.getNickname());
            view.setWorkId(a.getWorkId());
            Work work = works.get(a.getWorkId());
            view.setWorkTitle(work == null ? "" : work.getTitle());
            view.setCreatedAt(a.getCreatedAt());
            return view;
        }).toList();
    }

    public List<User> judges() {
        return userMapper.selectList(new QueryWrapper<User>()
                .eq("role", "JUDGE").orderByDesc("created_at"));
    }

    public Set<Long> assignedWorkIds(Long judgeId, Long contestId) {
        return assignMapper.selectList(new QueryWrapper<JudgeAssign>()
                        .eq("judge_id", judgeId).eq("contest_id", contestId)).stream()
                .map(JudgeAssign::getWorkId).collect(Collectors.toCollection(HashSet::new));
    }
}
