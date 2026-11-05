package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.common.BizException;
import com.btr.platform.dto.Requests.AssignReq;
import com.btr.platform.dto.Requests.ScoreReq;
import com.btr.platform.dto.Views.AssignView;
import com.btr.platform.dto.Views.JudgeProgress;
import com.btr.platform.dto.Views.JudgeTask;
import com.btr.platform.entity.Award;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.JudgeAssign;
import com.btr.platform.entity.Score;
import com.btr.platform.entity.Signup;
import com.btr.platform.entity.User;
import com.btr.platform.entity.Work;
import com.btr.platform.mapper.AwardMapper;
import com.btr.platform.mapper.ContestMapper;
import com.btr.platform.mapper.JudgeAssignMapper;
import com.btr.platform.mapper.ScoreMapper;
import com.btr.platform.mapper.SignupMapper;
import com.btr.platform.mapper.UserMapper;
import com.btr.platform.mapper.WorkMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final AwardMapper awardMapper;
    private final LogService logService;

    public JudgeService(JudgeAssignMapper assignMapper, WorkMapper workMapper, ContestMapper contestMapper,
                        SignupMapper signupMapper, ScoreMapper scoreMapper, UserMapper userMapper,
                        RankService rankService, AwardMapper awardMapper, LogService logService) {
        this.assignMapper = assignMapper;
        this.workMapper = workMapper;
        this.contestMapper = contestMapper;
        this.signupMapper = signupMapper;
        this.scoreMapper = scoreMapper;
        this.userMapper = userMapper;
        this.rankService = rankService;
        this.awardMapper = awardMapper;
        this.logService = logService;
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
        // 动态跟随：ALL 记录展开为该赛事当前全部已提交作品/非取消选手，评审期新作品自动进池
        List<JudgeAssign> expanded = new ArrayList<>();
        for (JudgeAssign assign : assigns) {
            if ("ALL".equals(assign.getAssignType())) {
                Contest contest = contestMapper.selectById(assign.getContestId());
                if (contest == null) {
                    continue;
                }
                if ("OFFLINE".equals(contest.getMode())) {
                    List<Signup> signups = signupMapper.selectList(new QueryWrapper<Signup>()
                            .eq("contest_id", assign.getContestId()).ne("status", "CANCELED"));
                    for (Signup s : signups) {
                        JudgeAssign a = new JudgeAssign();
                        a.setId(assign.getId());
                        a.setContestId(assign.getContestId());
                        a.setJudgeId(assign.getJudgeId());
                        a.setSignupId(s.getId());
                        a.setAssignType("ALL");
                        expanded.add(a);
                    }
                } else {
                    List<Work> works = workMapper.selectList(new QueryWrapper<Work>()
                            .eq("contest_id", assign.getContestId()).eq("status", "SUBMITTED"));
                    for (Work w : works) {
                        JudgeAssign a = new JudgeAssign();
                        a.setId(assign.getId());
                        a.setContestId(assign.getContestId());
                        a.setJudgeId(assign.getJudgeId());
                        a.setWorkId(w.getId());
                        a.setAssignType("ALL");
                        expanded.add(a);
                    }
                }
            } else {
                expanded.add(assign);
            }
        }
        if (expanded.isEmpty()) {
            return List.of();
        }
        List<Long> workIds = expanded.stream().map(JudgeAssign::getWorkId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        List<Long> signupIds = expanded.stream().map(JudgeAssign::getSignupId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, Work> works = workIds.isEmpty() ? Map.of() : workMapper.selectBatchIds(workIds).stream()
                .collect(Collectors.toMap(Work::getId, Function.identity()));
        Map<Long, Contest> contests = contestMapper.selectBatchIds(expanded.stream()
                        .map(JudgeAssign::getContestId).distinct().toList()).stream()
                .collect(Collectors.toMap(Contest::getId, Function.identity()));
        List<Long> concatIds = java.util.stream.Stream.concat(
                        works.values().stream().map(Work::getSignupId), signupIds.stream())
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, Signup> signups = concatIds.isEmpty() ? Map.of()
                : signupMapper.selectBatchIds(concatIds).stream()
                        .collect(Collectors.toMap(Signup::getId, Function.identity()));
        Map<String, Score> scores = scoreMapper.selectList(new QueryWrapper<Score>()
                        .eq("judge_id", judgeId)).stream()
                .collect(Collectors.toMap(s -> s.getWorkId() != null
                        ? "W:" + s.getWorkId() : "S:" + s.getSignupId(), Function.identity(), (a, b) -> a));

        List<JudgeTask> tasks = new ArrayList<>();
        for (JudgeAssign assign : expanded) {
            if (assign.getWorkId() != null) {
                Work work = works.get(assign.getWorkId());
                if (work == null || !"SUBMITTED".equals(work.getStatus())) {
                    continue;
                }
                JudgeTask task = new JudgeTask();
                // ALL 展开记录用负数唯一标记，避免多作品共用同一 assignId
                task.setAssignId("ALL".equals(assign.getAssignType()) ? -work.getId() : assign.getId());
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
                Score score = scores.get("W:" + work.getId());
                if (score != null) {
                    task.setScoreId(score.getId());
                    task.setScore(score.getScore());
                    task.setComment(score.getComment());
                }
                tasks.add(task);
            } else if (assign.getSignupId() != null) {
                Signup signup = signups.get(assign.getSignupId());
                if (signup == null) {
                    continue;
                }
                JudgeTask task = new JudgeTask();
                task.setAssignId("ALL".equals(assign.getAssignType()) ? -signup.getId() : assign.getId());
                task.setSignupId(signup.getId());
                task.setWorkTitle("选手：" + signup.getRealName());
                task.setWorkDescription(signup.getIntro());
                task.setContestId(assign.getContestId());
                Contest contest = contests.get(assign.getContestId());
                task.setContestTitle(contest == null ? "" : contest.getTitle());
                task.setRealName(signup.getRealName());
                task.setSchool(signup.getSchool());
                Score score = scores.get("S:" + signup.getId());
                if (score != null) {
                    task.setScoreId(score.getId());
                    task.setScore(score.getScore());
                    task.setComment(score.getComment());
                }
                tasks.add(task);
            }
        }
        tasks.sort(Comparator.comparing(JudgeTask::getContestId).reversed()
                .thenComparing(t -> t.getWorkId() != null ? t.getWorkId() : -t.getSignupId()));
        return tasks;
    }

    public List<JudgeProgress> progress(Long judgeId) {
        List<JudgeTask> tasks = tasks(judgeId, null);
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, List<JudgeTask>> byContest = tasks.stream()
                .collect(Collectors.groupingBy(JudgeTask::getContestId));
        Map<Long, Contest> contests = contestMapper.selectBatchIds(byContest.keySet()).stream()
                .collect(Collectors.toMap(Contest::getId, Function.identity()));
        List<JudgeProgress> list = new ArrayList<>();
        for (Map.Entry<Long, List<JudgeTask>> entry : byContest.entrySet()) {
            JudgeProgress progress = new JudgeProgress();
            progress.setContestId(entry.getKey());
            Contest contest = contests.get(entry.getKey());
            progress.setContestTitle(contest == null ? "" : contest.getTitle());
            progress.setAssigned(entry.getValue().size());
            progress.setScored(entry.getValue().stream().filter(t -> t.getScoreId() != null).count());
            progress.setPending(progress.getAssigned() - progress.getScored());
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
        List<Long> wIds = assigns.stream().map(JudgeAssign::getWorkId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        List<Long> sIds = assigns.stream().map(JudgeAssign::getSignupId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        QueryWrapper<Score> scoreQuery = new QueryWrapper<Score>()
                .in("judge_id", assigns.stream().map(JudgeAssign::getJudgeId).distinct().toList());
        scoreQuery.and(q -> {
            if (!wIds.isEmpty()) {
                q.in("work_id", wIds);
            }
            if (!sIds.isEmpty()) {
                q.or().in("signup_id", sIds);
            }
            if (wIds.isEmpty() && sIds.isEmpty()) {
                q.eq("work_id", -1L);
            }
        });
        Set<String> scoredIds = scoreMapper.selectList(scoreQuery).stream()
                .map(s -> s.getWorkId() != null ? "W:" + s.getWorkId() : "S:" + s.getSignupId())
                .collect(Collectors.toSet());

        Map<String, com.btr.platform.dto.Views.AdminAssignProject> grouped = new java.util.LinkedHashMap<>();
        // 整体分配（ALL）动态池：judge:contest → 该赛事当前全部作品/选手标记
        Map<String, Set<String>> dynamicPool = new java.util.HashMap<>();
        Set<String> countedAll = new HashSet<>();
        for (JudgeAssign assign : assigns) {
            if (!"ALL".equals(assign.getAssignType())) {
                continue;
            }
            String key = assign.getJudgeId() + ":" + assign.getContestId();
            if (dynamicPool.containsKey(key)) {
                continue;
            }
            Contest contest = contests.get(assign.getContestId());
            if (contest == null) {
                continue;
            }
            Set<String> marks = new HashSet<>();
            if ("OFFLINE".equals(contest.getMode())) {
                signupMapper.selectList(new QueryWrapper<Signup>()
                                .eq("contest_id", assign.getContestId()).ne("status", "CANCELED"))
                        .forEach(s -> marks.add("S:" + s.getId()));
            } else {
                workMapper.selectList(new QueryWrapper<Work>()
                                .eq("contest_id", assign.getContestId()).eq("status", "SUBMITTED"))
                        .forEach(w -> marks.add("W:" + w.getId()));
            }
            dynamicPool.put(key, marks);
        }
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
                        p.setContestMode(contest == null ? "ONLINE" : contest.getMode());
                        p.setCreatedAt(assign.getCreatedAt());
                        p.setAssignIds(new ArrayList<>());
                        return p;
                    });
            if ("ALL".equals(assign.getAssignType())) {
                // 整体分配按动态池统计一次
                if (countedAll.add(key)) {
                    Set<String> marks = dynamicPool.get(key);
                    project.setWorkCount(marks.size());
                    project.setScoredCount(marks.stream().filter(scoredIds::contains).count());
                    project.getAssignIds().add(assign.getId());
                }
                continue;
            }
            project.setWorkCount(project.getWorkCount() + 1);
            String mark = assign.getWorkId() != null ? "W:" + assign.getWorkId() : "S:" + assign.getSignupId();
            if (scoredIds.contains(mark)) {
                project.setScoredCount(project.getScoredCount() + 1);
            }
            project.getAssignIds().add(assign.getId());
        }
        return new ArrayList<>(grouped.values());
    }

    @Transactional
    public Score score(ScoreReq req, Long judgeId) {
        JudgeAssign assign;
        if (req.signupId() != null) {
            assign = assignMapper.selectOne(new QueryWrapper<JudgeAssign>()
                    .eq("judge_id", judgeId).eq("signup_id", req.signupId()).last("limit 1"));
        } else {
            if (req.workId() == null) {
                throw new BizException("缺少评审对象");
            }
            assign = assignMapper.selectOne(new QueryWrapper<JudgeAssign>()
                    .eq("judge_id", judgeId).eq("work_id", req.workId()).last("limit 1"));
        }
        if (assign == null) {
            // 动态跟随：作品/选手在整体分配（ALL）赛事下，评委可直接评审
            Contest c = null;
            if (req.signupId() != null) {
                Signup s = signupMapper.selectById(req.signupId());
                c = s == null ? null : contestMapper.selectById(s.getContestId());
            } else {
                Work w = workMapper.selectById(req.workId());
                c = w == null ? null : contestMapper.selectById(w.getContestId());
            }
            if (c != null) {
                long allCnt = assignMapper.selectCount(new QueryWrapper<JudgeAssign>()
                        .eq("judge_id", judgeId).eq("contest_id", c.getId()).eq("assign_type", "ALL"));
                if (allCnt > 0) {
                    assign = new JudgeAssign();
                    assign.setContestId(c.getId());
                    assign.setJudgeId(judgeId);
                    assign.setWorkId(req.signupId() == null ? req.workId() : null);
                    assign.setSignupId(req.signupId());
                }
            }
        }
        if (assign == null) {
            throw new BizException(403, "该评审对象未分配给你评审");
        }
        Contest contest = contestMapper.selectById(assign.getContestId());
        if (contest == null || !(Set.of("REVIEW", "FINISHED").contains(contest.getStatus()))) {
            throw new BizException("当前不在评审阶段，无法打分");
        }
        Score score;
        if (req.signupId() != null) {
            score = scoreMapper.selectOne(new QueryWrapper<Score>()
                    .eq("judge_id", judgeId).eq("signup_id", req.signupId()).last("limit 1"));
        } else {
            Work work = workMapper.selectById(req.workId());
            if (work == null || !"SUBMITTED".equals(work.getStatus())) {
                throw new BizException("作品不可评审");
            }
            score = scoreMapper.selectOne(new QueryWrapper<Score>()
                    .eq("judge_id", judgeId).eq("work_id", req.workId()).last("limit 1"));
        }
        boolean create = score == null;
        if (create) {
            score = new Score();
            score.setContestId(assign.getContestId());
            score.setWorkId(req.signupId() == null ? req.workId() : null);
            score.setSignupId(req.signupId());
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
        Contest contest = contestMapper.selectById(req.contestId());
        if (contest == null) {
            throw new BizException(404, "赛事不存在");
        }
        if (Boolean.TRUE.equals(contest.getResultsLocked())) {
            throw new BizException("成绩已固化锁定，不能调整分配");
        }
        List<Long> judgeIds = req.judgeIds();
        if ((judgeIds == null || judgeIds.isEmpty()) && req.judgeId() != null) {
            judgeIds = List.of(req.judgeId());
        }
        if (judgeIds == null || judgeIds.isEmpty()) {
            throw new BizException("请选择评委");
        }
        boolean offline = "OFFLINE".equals(contest.getMode());
        boolean whole = req.workIds() == null || req.workIds().isEmpty();
        for (Long judgeId : judgeIds) {
            User judge = userMapper.selectById(judgeId);
            if (judge == null || !"JUDGE".equals(judge.getRole())) {
                throw new BizException("评委账号不存在");
            }
            if (whole) {
                // 赛事整体分配（动态跟随）：同一评委同一赛事只保留一条 ALL 记录
                long exists = assignMapper.selectCount(new QueryWrapper<JudgeAssign>()
                        .eq("judge_id", judgeId).eq("contest_id", req.contestId()).eq("assign_type", "ALL"));
                if (exists == 0) {
                    JudgeAssign assign = new JudgeAssign();
                    assign.setContestId(req.contestId());
                    assign.setJudgeId(judgeId);
                    assign.setAssignType("ALL");
                    assign.setCreatedAt(LocalDateTime.now());
                    assignMapper.insert(assign);
                }
            } else if (offline) {
                List<Signup> signups = signupMapper.selectList(new QueryWrapper<Signup>()
                        .eq("contest_id", req.contestId()).in("id", req.workIds()));
                for (Signup signup : signups) {
                    long exists = assignMapper.selectCount(new QueryWrapper<JudgeAssign>()
                            .eq("judge_id", judgeId).eq("signup_id", signup.getId()));
                    if (exists == 0) {
                        JudgeAssign assign = new JudgeAssign();
                        assign.setContestId(req.contestId());
                        assign.setJudgeId(judgeId);
                        assign.setSignupId(signup.getId());
                        assign.setAssignType("ITEM");
                        assign.setCreatedAt(LocalDateTime.now());
                        assignMapper.insert(assign);
                    }
                }
            } else {
                List<Work> works = workMapper.selectList(new QueryWrapper<Work>()
                        .eq("contest_id", req.contestId()).in("id", req.workIds()));
                for (Work work : works) {
                    long exists = assignMapper.selectCount(new QueryWrapper<JudgeAssign>()
                            .eq("judge_id", judgeId).eq("work_id", work.getId()));
                    if (exists == 0) {
                        JudgeAssign assign = new JudgeAssign();
                        assign.setContestId(req.contestId());
                        assign.setJudgeId(judgeId);
                        assign.setWorkId(work.getId());
                        assign.setAssignType("ITEM");
                        assign.setCreatedAt(LocalDateTime.now());
                        assignMapper.insert(assign);
                    }
                }
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
        List<Long> wIds = assigns.stream().map(JudgeAssign::getWorkId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        List<Long> sIds = assigns.stream().map(JudgeAssign::getSignupId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, Work> works = wIds.isEmpty() ? Map.of() : workMapper.selectBatchIds(wIds).stream()
                .collect(Collectors.toMap(Work::getId, Function.identity()));
        Map<Long, Signup> signups = sIds.isEmpty() ? Map.of() : signupMapper.selectBatchIds(sIds).stream()
                .collect(Collectors.toMap(Signup::getId, Function.identity()));
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
            if (a.getWorkId() != null) {
                Work work = works.get(a.getWorkId());
                view.setWorkTitle(work == null ? "" : work.getTitle());
            } else if (a.getSignupId() != null) {
                Signup signup = signups.get(a.getSignupId());
                view.setWorkTitle("选手：" + (signup == null ? "" : signup.getRealName()));
            }
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

    /* ------------------------- 满血版：聚合算分 / 排名 / 改分 / 固化 ------------------------- */

    /** 管理端：作品成绩与排名（多评委聚合，AVG 均分 / TRIM 去极值） */
    public com.btr.platform.dto.Views.ScoreResultView scoreResults(Long contestId) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BizException(404, "赛事不存在");
        }
        boolean offline = "OFFLINE".equals(contest.getMode());
        List<Work> works = offline ? List.of() : workMapper.selectList(new QueryWrapper<Work>()
                .eq("contest_id", contestId).eq("status", "SUBMITTED"));
        List<Signup> signups = offline ? signupMapper.selectList(new QueryWrapper<Signup>()
                .eq("contest_id", contestId).ne("status", "CANCELED")) : List.of();
        List<Score> scores = scoreMapper.selectList(new QueryWrapper<Score>().eq("contest_id", contestId));
        Map<Long, List<Score>> bySubject = new java.util.HashMap<>();
        for (Score s : scores) {
            Long sid = s.getWorkId() != null ? s.getWorkId() : s.getSignupId();
            bySubject.computeIfAbsent(sid, k -> new ArrayList<>()).add(s);
        }
        Map<Long, User> judges = scores.stream().map(Score::getJudgeId).distinct().toList().isEmpty() ? Map.of()
                : userMapper.selectBatchIds(scores.stream().map(Score::getJudgeId).distinct().toList()).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        String mode = contest.getAggregateMode() == null ? "AVG" : contest.getAggregateMode();

        com.btr.platform.dto.Views.ScoreResultView view = new com.btr.platform.dto.Views.ScoreResultView();
        view.setContestId(contestId);
        view.setContestTitle(contest.getTitle());
        view.setContestStatus(contest.getStatus());
        view.setResultsLocked(Boolean.TRUE.equals(contest.getResultsLocked()));
        view.setFinalizedAt(contest.getFinalizedAt());
        view.setAggregateMode(mode);
        view.setJudgePerWork(contest.getJudgePerWork() == null ? 3 : contest.getJudgePerWork());
        List<com.btr.platform.dto.Views.ScoreResultRow> rows = new ArrayList<>();
        if (offline) {
            for (Signup signup : signups) {
                rows.add(buildRow(null, signup.getId(), "选手：" + signup.getRealName(), signup.getSchool(),
                        bySubject.get(signup.getId()), judges, mode));
            }
        } else {
            Map<Long, Signup> signupMap = works.isEmpty() ? Map.of()
                    : signupMapper.selectBatchIds(works.stream().map(Work::getSignupId)
                            .filter(java.util.Objects::nonNull).distinct().toList()).stream()
                            .collect(Collectors.toMap(Signup::getId, Function.identity()));
            for (Work work : works) {
                Signup signup = work.getSignupId() == null ? null : signupMap.get(work.getSignupId());
                rows.add(buildRow(work.getId(), null, work.getTitle(), signup == null ? "" : signup.getSchool(),
                        bySubject.get(work.getId()), judges, mode));
            }
        }
        // 排名：最终分降序，同分并列
        rows.sort(Comparator.comparing((com.btr.platform.dto.Views.ScoreResultRow r) -> {
            BigDecimal s = r.getAggregateScore();
            return s == null ? BigDecimal.valueOf(-1) : s;
        }).reversed());
        int cursor = 0;
        int lastRank = 0;
        BigDecimal prev = null;
        for (com.btr.platform.dto.Views.ScoreResultRow row : rows) {
            cursor++;
            BigDecimal cur = row.getAggregateScore();
            if (cur == null) {
                row.setRankNo(0);
                continue;
            }
            if (prev != null && cur.compareTo(prev) == 0) {
                row.setRankNo(lastRank);
            } else {
                lastRank = cursor;
                row.setRankNo(lastRank);
            }
            prev = cur;
        }
        view.setRows(rows);
        return view;
    }

    private com.btr.platform.dto.Views.ScoreResultRow buildRow(Long workId, Long signupId, String title, String school,
                                                               List<Score> scoreList, Map<Long, User> judges, String mode) {
        com.btr.platform.dto.Views.ScoreResultRow row = new com.btr.platform.dto.Views.ScoreResultRow();
        row.setWorkId(workId);
        row.setSignupId(signupId);
        row.setTitle(title == null ? "" : title);
        row.setSchool(school == null ? "" : school);
        List<Score> list = scoreList == null ? List.of() : scoreList;
        List<BigDecimal> eff = list.stream()
                .map(s -> s.getAdjustedScore() != null ? s.getAdjustedScore() : s.getScore())
                .filter(java.util.Objects::nonNull).toList();
        row.setJudgeCount(list.size());
        row.setScoredCount(eff.size());
        row.setAggregateScore(aggregate(eff, mode));
        if (!eff.isEmpty()) {
            BigDecimal max = eff.stream().max(Comparator.naturalOrder()).get();
            BigDecimal min = eff.stream().min(Comparator.naturalOrder()).get();
            row.setMaxGap(max.subtract(min));
            row.setReviewFlag(max.subtract(min).compareTo(BigDecimal.valueOf(15)) >= 0);
        } else {
            row.setMaxGap(null);
            row.setReviewFlag(false);
        }
        row.setAdjusted(list.stream().anyMatch(s -> s.getAdjustedScore() != null));
        List<com.btr.platform.dto.Views.JudgeScoreItem> items = new ArrayList<>();
        for (Score s : list) {
            com.btr.platform.dto.Views.JudgeScoreItem item = new com.btr.platform.dto.Views.JudgeScoreItem();
            item.setScoreId(s.getId());
            item.setJudgeId(s.getJudgeId());
            User judge = judges.get(s.getJudgeId());
            item.setJudgeName(judge == null ? "" : judge.getNickname());
            item.setScore(s.getScore());
            item.setAdjustedScore(s.getAdjustedScore());
            item.setComment(s.getComment());
            item.setAdjusted(s.getAdjustedScore() != null);
            item.setAdjustedAt(s.getAdjustedAt());
            items.add(item);
        }
        row.setJudgeScores(items);
        return row;
    }

    private BigDecimal aggregate(List<BigDecimal> values, String mode) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        if ("TRIM".equals(mode) && values.size() >= 3) {
            BigDecimal min = values.stream().min(Comparator.naturalOrder()).get();
            BigDecimal max = values.stream().max(Comparator.naturalOrder()).get();
            List<BigDecimal> trimmed = values.stream()
                    .filter(v -> v.compareTo(min) != 0 && v.compareTo(max) != 0).toList();
            if (!trimmed.isEmpty()) {
                values = trimmed;
            }
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }

    /** 管理员改最终分：覆盖对应评委的贡献分，排名/奖项自动重算，留痕 */
    @Transactional
    public void adjustScore(Long scoreId, BigDecimal score, String reason, Long adminId, String ip) {
        Score s = scoreMapper.selectById(scoreId);
        if (s == null) {
            throw new BizException(404, "评分记录不存在");
        }
        Contest contest = contestMapper.selectById(s.getContestId());
        if (contest == null) {
            throw new BizException(404, "赛事不存在");
        }
        if (Boolean.TRUE.equals(contest.getResultsLocked())) {
            throw new BizException("成绩已固化锁定，不能改分");
        }
        BigDecimal old = s.getAdjustedScore() != null ? s.getAdjustedScore() : s.getScore();
        s.setAdjustedScore(score);
        s.setAdjustReason(reason);
        s.setAdjustBy(adminId);
        s.setAdjustedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        scoreMapper.updateById(s);
        logService.record(adminId, "改分", "评分记录 #" + scoreId + "：" + old.toPlainString()
                + " → " + score.toPlainString() + "，原因：" + reason, ip);
        rankService.evict(s.getContestId());
    }

    /** 固化成绩：按聚合排名自动生成获奖名单并锁定（榜单/获奖/证书联动） */
    @Transactional
    public void finalizeResults(Long contestId, Long adminId, String ip) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BizException(404, "赛事不存在");
        }
        if (Boolean.TRUE.equals(contest.getResultsLocked())) {
            throw new BizException("成绩已固化，无需重复操作");
        }
        if (!Set.of("REVIEW", "FINISHED", "PUBLISHED").contains(contest.getStatus())) {
            throw new BizException("当前赛事状态不可固化成绩");
        }
        com.btr.platform.dto.Views.ScoreResultView view = scoreResults(contestId);
        List<Award> awards = awardMapper.selectList(new QueryWrapper<Award>()
                .eq("contest_id", contestId).orderByAsc("sort_order"));
        if (awards.isEmpty()) {
            throw new BizException("请先配置奖项再固化成绩");
        }
        boolean offline = "OFFLINE".equals(contest.getMode());
        List<com.btr.platform.dto.Views.ScoreResultRow> ranked = view.getRows().stream()
                .filter(r -> r.getRankNo() > 0)
                .sorted(Comparator.comparingInt(
                        (com.btr.platform.dto.Views.ScoreResultRow r) -> r.getRankNo())).toList();
        List<com.btr.platform.dto.Requests.WinnerEditReq> items = new ArrayList<>();
        int idx = 0;
        for (Award award : awards) {
            int quota = award.getQuota() == null ? 0 : award.getQuota();
            for (int k = 0; k < quota && idx < ranked.size(); k++, idx++) {
                com.btr.platform.dto.Views.ScoreResultRow row = ranked.get(idx);
                items.add(new com.btr.platform.dto.Requests.WinnerEditReq(
                        offline ? row.getSignupId() : row.getWorkId(), award.getId(), row.getRankNo()));
            }
        }
        rankService.adminUpdateWinners(contestId, items);
        contest.setResultsLocked(true);
        contest.setFinalizedAt(LocalDateTime.now());
        contestMapper.updateById(contest);
        logService.record(adminId, "固化成绩", "赛事「" + contest.getTitle() + "」成绩固化，自动生成获奖 "
                + items.size() + " 条", ip);
    }
}
