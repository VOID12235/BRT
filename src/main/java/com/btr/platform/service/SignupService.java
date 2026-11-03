package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btr.platform.common.BizException;
import com.btr.platform.common.PageResult;
import com.btr.platform.dto.Requests.SignupReq;
import com.btr.platform.dto.Views.SignupView;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.Signup;
import com.btr.platform.entity.User;
import com.btr.platform.entity.Work;
import com.btr.platform.mapper.ContestMapper;
import com.btr.platform.mapper.SignupMapper;
import com.btr.platform.mapper.UserMapper;
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
public class SignupService {

    private static final Set<String> STATUSES = Set.of("PENDING", "APPROVED", "CANCELED");

    private final SignupMapper signupMapper;
    private final ContestMapper contestMapper;
    private final WorkMapper workMapper;
    private final UserMapper userMapper;
    private final EmailService emailService;

    public SignupService(SignupMapper signupMapper, ContestMapper contestMapper, WorkMapper workMapper,
                         UserMapper userMapper, EmailService emailService) {
        this.signupMapper = signupMapper;
        this.contestMapper = contestMapper;
        this.workMapper = workMapper;
        this.userMapper = userMapper;
        this.emailService = emailService;
    }

    @Transactional
    public SignupView signup(SignupReq req, User user) {
        Contest contest = contestMapper.selectById(req.contestId());
        if (contest == null) {
            throw new BizException(404, "赛事不存在");
        }
        if (!"SIGNUP".equals(contest.getStatus())) {
            throw new BizException("该赛事当前不在报名阶段");
        }
        LocalDateTime now = LocalDateTime.now();
        if (contest.getSignupStart() != null && now.isBefore(contest.getSignupStart())) {
            throw new BizException("报名尚未开始");
        }
        if (contest.getSignupEnd() != null && now.isAfter(contest.getSignupEnd())) {
            throw new BizException("报名已截止");
        }
        Signup exists = signupMapper.selectOne(new QueryWrapper<Signup>()
                .eq("contest_id", req.contestId()).eq("user_id", user.getId()).last("limit 1"));
        if (exists != null && !"CANCELED".equals(exists.getStatus())) {
            throw new BizException("你已报名该赛事");
        }
        Signup signup = exists == null ? new Signup() : exists;
        signup.setContestId(req.contestId());
        signup.setUserId(user.getId());
        signup.setRealName(req.realName());
        signup.setSchool(req.school());
        signup.setContact(req.contact());
        signup.setIntro(req.intro());
        signup.setStatus("APPROVED");
        signup.setCreatedAt(exists == null ? LocalDateTime.now() : exists.getCreatedAt());
        if (exists == null) {
            signupMapper.insert(signup);
        } else {
            signupMapper.updateById(signup);
        }
        emailService.signupReceipt(user.getEmail(), user.getNickname(), contest.getTitle());
        return toView(signup, contest, user, null);
    }

    @Transactional
    public void cancel(Long id, Long userId) {
        Signup signup = signupMapper.selectById(id);
        if (signup == null || !signup.getUserId().equals(userId)) {
            throw new BizException(404, "报名记录不存在");
        }
        Contest contest = contestMapper.selectById(signup.getContestId());
        if (contest == null || !"SIGNUP".equals(contest.getStatus())) {
            throw new BizException("报名阶段已结束，无法取消");
        }
        Work work = workMapper.selectOne(new QueryWrapper<Work>().eq("signup_id", id).last("limit 1"));
        if (work != null && "SUBMITTED".equals(work.getStatus())) {
            throw new BizException("请先撤回作品，再取消报名");
        }
        signup.setStatus("CANCELED");
        signupMapper.updateById(signup);
    }

    public List<SignupView> mine(Long userId) {
        List<Signup> signups = signupMapper.selectList(new QueryWrapper<Signup>()
                .eq("user_id", userId).orderByDesc("created_at"));
        Map<Long, Contest> contests = contestMapper.selectBatchIds(signups.stream()
                        .map(Signup::getContestId).distinct().toList()).stream()
                .collect(Collectors.toMap(Contest::getId, Function.identity()));
        User user = userMapper.selectById(userId);
        return signups.stream().map(s -> {
            Work work = workMapper.selectOne(new QueryWrapper<Work>()
                    .eq("signup_id", s.getId()).last("limit 1"));
            return toView(s, contests.get(s.getContestId()), user, work);
        }).toList();
    }

    public PageResult<SignupView> adminList(Long contestId, String status, long page, long size) {
        QueryWrapper<Signup> query = new QueryWrapper<>();
        if (contestId != null) {
            query.eq("contest_id", contestId);
        }
        if (status != null && !status.isBlank()) {
            query.eq("status", status);
        }
        query.orderByDesc("created_at");
        Page<Signup> result = signupMapper.selectPage(new Page<>(page, size), query);
        List<Signup> records = result.getRecords();
        Map<Long, Contest> contests = records.isEmpty() ? Map.of() : contestMapper.selectBatchIds(records.stream()
                        .map(Signup::getContestId).distinct().toList()).stream()
                .collect(Collectors.toMap(Contest::getId, Function.identity()));
        Map<Long, User> users = records.isEmpty() ? Map.of() : userMapper.selectBatchIds(records.stream()
                        .map(Signup::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<SignupView> views = records.stream().map(s -> {
            Work work = workMapper.selectOne(new QueryWrapper<Work>()
                    .eq("signup_id", s.getId()).last("limit 1"));
            return toView(s, contests.get(s.getContestId()), users.get(s.getUserId()), work);
        }).toList();
        return PageResult.of(views, result.getTotal(), page, size);
    }

    @Transactional
    public void adminStatus(Long id, String status) {
        if (!STATUSES.contains(status)) {
            throw new BizException("非法报名状态");
        }
        Signup signup = signupMapper.selectById(id);
        if (signup == null) {
            throw new BizException(404, "报名记录不存在");
        }
        signup.setStatus(status);
        signupMapper.updateById(signup);
    }

    public Signup getOrThrow(Long id) {
        Signup signup = signupMapper.selectById(id);
        if (signup == null) {
            throw new BizException(404, "报名记录不存在");
        }
        return signup;
    }

    private SignupView toView(Signup signup, Contest contest, User user, Work work) {
        SignupView view = new SignupView();
        view.setId(signup.getId());
        view.setContestId(signup.getContestId());
        view.setContestTitle(contest == null ? "" : contest.getTitle());
        view.setUserId(signup.getUserId());
        view.setAccount(user == null ? "" : user.getAccount());
        view.setNickname(user == null ? "" : user.getNickname());
        view.setRealName(signup.getRealName());
        view.setSchool(signup.getSchool());
        view.setContact(signup.getContact());
        view.setIntro(signup.getIntro());
        view.setStatus(signup.getStatus());
        view.setCreatedAt(signup.getCreatedAt());
        if (work != null) {
            view.setWorkId(work.getId());
            view.setWorkTitle(work.getTitle());
        }
        return view;
    }
}
