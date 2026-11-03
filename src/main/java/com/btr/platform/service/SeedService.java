package com.btr.platform.service;

import com.btr.platform.entity.Award;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.JudgeAssign;
import com.btr.platform.entity.Notice;
import com.btr.platform.entity.Score;
import com.btr.platform.entity.Signup;
import com.btr.platform.entity.User;
import com.btr.platform.entity.Work;
import com.btr.platform.mapper.AwardMapper;
import com.btr.platform.mapper.ContestMapper;
import com.btr.platform.mapper.JudgeAssignMapper;
import com.btr.platform.mapper.NoticeMapper;
import com.btr.platform.mapper.ScoreMapper;
import com.btr.platform.mapper.SignupMapper;
import com.btr.platform.mapper.UserMapper;
import com.btr.platform.mapper.WorkMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SeedService implements CommandLineRunner {

    private final UserMapper userMapper;
    private final ContestMapper contestMapper;
    private final SignupMapper signupMapper;
    private final WorkMapper workMapper;
    private final ScoreMapper scoreMapper;
    private final AwardMapper awardMapper;
    private final JudgeAssignMapper assignMapper;
    private final NoticeMapper noticeMapper;
    private final RankService rankService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-demo:true}")
    private boolean seedDemo;

    public SeedService(UserMapper userMapper, ContestMapper contestMapper, SignupMapper signupMapper,
                       WorkMapper workMapper, ScoreMapper scoreMapper, AwardMapper awardMapper,
                       JudgeAssignMapper assignMapper, NoticeMapper noticeMapper, RankService rankService,
                       PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.contestMapper = contestMapper;
        this.signupMapper = signupMapper;
        this.workMapper = workMapper;
        this.scoreMapper = scoreMapper;
        this.awardMapper = awardMapper;
        this.assignMapper = assignMapper;
        this.noticeMapper = noticeMapper;
        this.rankService = rankService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedDemo || userMapper.selectCount(null) > 0) {
            return;
        }
        log.info("初始化 BTR 演示数据…");
        LocalDateTime now = LocalDateTime.now();

        User admin = user("admin", "admin123", "平台管理员", "admin@btr.dev", "ADMIN");
        User judge = user("judge", "judge123", "陈评委", "judge@btr.dev", "JUDGE");
        User judge2 = user("judge2", "judge123", "林评委", "judge2@btr.dev", "JUDGE");
        User player = user("player", "player123", "苏晓", "player@btr.dev", "PLAYER");
        User luna = user("luna", "player123", "陆娜", "luna@btr.dev", "PLAYER");
        User kai = user("kai", "player123", "周凯", "kai@btr.dev", "PLAYER");
        User mia = user("mia", "player123", "米娅", "mia@btr.dev", "PLAYER");

        Contest c1 = contest("2026 BTR 全国大学生算法挑战赛", "/assets/img/cover-1.svg",
                "面向全国高校的算法实战赛，个人赛制，三轮积分排名。",
                "以算法思维解决真实工程问题。比赛分为热身、正赛与决赛三轮，覆盖数据结构、图论、动态规划与工程实现。",
                "1. 比赛为个人赛，禁止组队与代打；\n2. 每轮 3 小时，允许携带纸质资料；\n3. 代码需通过平台评测机提交；\n4. 严禁抄袭，违规者取消成绩。",
                now.minusDays(12), now.plusDays(8), now.plusDays(9), now.plusDays(16), "SIGNUP", true);
        Contest c2 = contest("城市青年摄影大赛 · 秋季赛", "/assets/img/cover-2.svg",
                "用一张照片，记录城市在秋天里的呼吸。",
                "本届秋季赛以「城市呼吸」为主题，鼓励创作者观察街角、光线与人的关系。单幅与组图均可参赛。",
                "1. 作品须为本人原创；\n2. 单幅或 4-8 张组图；\n3. 允许基础调色，不接受合成；\n4. 提交 JPG/PNG，长边不小于 2000px。",
                now.minusDays(40), now.minusDays(5), now.minusDays(4), now.plusDays(6), "REVIEW", true);
        Contest c3 = contest("BTR 校园原创音乐榜", "/assets/img/cover-3.svg",
                "把青春写进歌里，让更多人听见。",
                "面向校园音乐人的原创作品征集，风格不限。由专业评委与大众评审共同选出年度榜单。",
                "1. 原创词曲，未公开发行；\n2. 时长 2-6 分钟；\n3. 提交音频与歌词；\n4. 禁止使用未授权采样。",
                now.minusDays(70), now.minusDays(40), now.minusDays(35), now.minusDays(20), "PUBLISHED", true);
        Contest c4 = contest("2025 年度设计新锐奖", "/assets/img/cover-4.svg",
                "寻找下一位改变日常的设计师。",
                "覆盖视觉传达、产品设计与空间设计三个方向，评审团由独立设计师与品牌主理人组成。",
                "1. 提交完整作品集；\n2. 作品须为近两年内创作；\n3. 允许团队参赛，人数不超过 3 人；\n4. 进入终审需提交源文件。",
                now.minusDays(160), now.minusDays(120), now.minusDays(110), now.minusDays(90), "FINISHED", true);
        Contest c5 = contest("AI 创意短片征集", "/assets/img/cover-1.svg",
                "用 AI 讲一个只有你能讲的故事。",
                "筹备中的 AI 影像单元，鼓励创作者探索生成式工具与叙事的边界。",
                "细则筹备中，敬请期待。",
                now.plusDays(10), now.plusDays(40), now.plusDays(41), now.plusDays(55), "DRAFT", false);
        Contest c6 = contest("开源硬件创客马拉松", "/assets/img/cover-2.svg",
                "48 小时，把想法焊成能跑的原型。",
                "面向创客与硬件爱好者的线下黑客松，提供开发板、传感器与 3D 打印支持。",
                "1. 3 人以内组队；\n2. 现场完成原型；\n3. 代码与硬件设计开源；\n4. 决赛进行 5 分钟路演。",
                now.minusDays(3), now.plusDays(20), now.plusDays(21), now.plusDays(28), "SIGNUP", true);

        awards(c1);
        awards(c2);
        awards(c3);
        awards(c4);
        awards(c6);

        Signup s1 = signup(c1, player, "苏晓", "南京大学", "13800000001");
        Signup s2 = signup(c1, luna, "陆娜", "中国美术学院", "13800000002");
        Signup s3 = signup(c1, kai, "周凯", "同济大学", "13800000003");

        Signup s4 = signup(c2, player, "苏晓", "南京大学", "13800000001");
        Signup s5 = signup(c2, luna, "陆娜", "中国美术学院", "13800000002");
        Signup s6 = signup(c2, kai, "周凯", "同济大学", "13800000003");
        Signup s7 = signup(c2, mia, "米娅", "复旦大学", "13800000004");

        Signup s8 = signup(c3, player, "苏晓", "南京大学", "13800000001");
        Signup s9 = signup(c3, luna, "陆娜", "中国美术学院", "13800000002");
        Signup s10 = signup(c3, kai, "周凯", "同济大学", "13800000003");

        Signup s11 = signup(c4, luna, "陆娜", "中国美术学院", "13800000002");
        Signup s12 = signup(c4, mia, "米娅", "复旦大学", "13800000004");

        Signup s13 = signup(c6, player, "苏晓", "南京大学", "13800000001");
        Signup s14 = signup(c6, kai, "周凯", "同济大学", "13800000003");

        Work w1 = work(s1, c1, player, "路径压缩的另一种写法", "并查集在极端数据下的优化实践。", now.minusDays(2));
        Work w2 = work(s4, c2, player, "雾中城市", "清晨六点的十字路口，雾气把楼宇揉成一片灰色。", now.minusDays(8));
        Work w3 = work(s5, c2, luna, "夜行", "末班地铁里，每个人都在赶往自己的光。", now.minusDays(7));
        Work w4 = work(s6, c2, kai, "边界", "城市与江面的交界，在日落时短暂消失。", now.minusDays(7));
        Work w5 = work(s7, c2, mia, "光落在旧墙上", "老城区改造前最后一次散步。", now.minusDays(6));
        Work w6 = work(s8, c3, player, "把夏天留在这里", "民谣 / 3:42，写给毕业前的最后一个夏天。", now.minusDays(45));
        Work w7 = work(s9, c3, luna, "潮汐来信", "独立流行 / 4:05，关于一封没有寄出的信。", now.minusDays(44));
        Work w8 = work(s10, c3, kai, "北纬三十一度", "后摇 / 5:18，记录一次长途骑行。", now.minusDays(43));
        Work w9 = work(s11, c4, luna, "慢下来 · 城市公共座椅计划", "产品设计 / 可移动的社区座椅系统。", now.minusDays(125));
        Work w10 = work(s12, c4, mia, "纸上的风", "视觉传达 / 以风为线索的书籍设计。", now.minusDays(124));
        Work w11 = work(s14, c6, kai, "桌面级空气质量站", "基于 ESP32 的桌面监测站，3D 打印外壳。", now.minusDays(1));

        assign(c2, judge, List.of(w2, w3, w4, w5));
        assign(c2, judge2, List.of(w2, w3, w4, w5));
        assign(c3, judge, List.of(w6, w7, w8));
        assign(c3, judge2, List.of(w6, w7, w8));
        assign(c4, judge, List.of(w9, w10));
        assign(c4, judge2, List.of(w9, w10));

        score(c2, w2, judge, "86", "雾气层次很好，主体可以再明确。");
        score(c2, w2, judge2, "90", "情绪准确，构图克制。");
        score(c2, w3, judge, "92", "光影叙事完整，完成度高。");
        score(c2, w3, judge2, "94", "有电影感，细节耐看。");
        score(c2, w4, judge, "78", "想法不错，但画面略显松散。");
        score(c2, w4, judge2, "82", "色彩处理有想法。");
        score(c2, w5, judge, "88", "题材有温度，建议精简画面。");
        score(c2, w5, judge2, "86", "记录性强，情绪自然。");

        score(c3, w6, judge, "90", "旋律自然，词有画面感。");
        score(c3, w6, judge2, "92", "副歌记忆点强。");
        score(c3, w7, judge, "95", "编曲完整，气质统一。");
        score(c3, w7, judge2, "93", "人声处理很舒服。");
        score(c3, w8, judge, "84", "结构可以再紧凑一些。");
        score(c3, w8, judge2, "86", "氛围铺陈到位。");

        score(c4, w9, judge, "88", "调研扎实，落地性强。");
        score(c4, w9, judge2, "90", "细节考虑周到。");
        score(c4, w10, judge, "91", "视觉语言成熟。");
        score(c4, w10, judge2, "89", "整体完成度很高。");

        rankService.snapshotWinners(c3.getId());
        rankService.snapshotWinners(c4.getId());

        notice("2026 赛季报名通道开启", "2026 BTR 全国大学生算法挑战赛报名已开启，报名截止至本月 20 日。请选手在个人中心完善信息后提交报名。", true, now.minusDays(3));
        notice("城市青年摄影大赛评审进行中", "秋季赛已进入评审阶段，评委可在工作台查看分配作品。评审结果将于评审结束后统一公示。", false, now.minusDays(1));
        notice("校园原创音乐榜获奖名单公示", "BTR 校园原创音乐榜获奖名单已公示，恭喜所有获奖选手。证书与奖品将在 7 个工作日内寄出。", true, now.minusDays(10));
        notice("平台服务升级公告", "为提升访问体验，平台将于本周日 02:00-04:00 进行例行维护，期间可能短暂无法访问。", false, now.minusDays(15));

        log.info("演示数据初始化完成：{} 位用户，{} 个赛事", 7, 6);
    }

    private User user(String account, String password, String nickname, String email, String role) {
        User user = new User();
        user.setAccount(account);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone("13800000000");
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now().minusDays(30));
        user.setUpdatedAt(LocalDateTime.now().minusDays(30));
        userMapper.insert(user);
        return user;
    }

    private Contest contest(String title, String cover, String summary, String description, String rules,
                            LocalDateTime signupStart, LocalDateTime signupEnd,
                            LocalDateTime reviewStart, LocalDateTime reviewEnd, String status, boolean isPublic) {
        Contest contest = new Contest();
        contest.setTitle(title);
        contest.setCover(cover);
        contest.setSummary(summary);
        contest.setDescription(description);
        contest.setRules(rules);
        contest.setSignupStart(signupStart);
        contest.setSignupEnd(signupEnd);
        contest.setReviewStart(reviewStart);
        contest.setReviewEnd(reviewEnd);
        contest.setStatus(status);
        contest.setIsPublic(isPublic);
        contest.setCreatedAt(signupStart.minusDays(5));
        contest.setUpdatedAt(LocalDateTime.now());
        contestMapper.insert(contest);
        return contest;
    }

    private void awards(Contest contest) {
        award(contest, "一等奖", 1, "85", 1);
        award(contest, "二等奖", 2, "75", 2);
        award(contest, "三等奖", 3, "60", 3);
    }

    private void award(Contest contest, String name, int quota, String minScore, int order) {
        Award award = new Award();
        award.setContestId(contest.getId());
        award.setName(name);
        award.setQuota(quota);
        award.setMinScore(new BigDecimal(minScore));
        award.setSortOrder(order);
        awardMapper.insert(award);
    }

    private Signup signup(Contest contest, User user, String realName, String school, String contact) {
        Signup signup = new Signup();
        signup.setContestId(contest.getId());
        signup.setUserId(user.getId());
        signup.setRealName(realName);
        signup.setSchool(school);
        signup.setContact(contact);
        signup.setIntro("热爱创作，期待与更多同好交流。");
        signup.setStatus("APPROVED");
        signup.setCreatedAt(contest.getSignupStart() == null ? LocalDateTime.now()
                : contest.getSignupStart().plusDays(1));
        signupMapper.insert(signup);
        return signup;
    }

    private Work work(Signup signup, Contest contest, User user, String title, String description, LocalDateTime time) {
        Work work = new Work();
        work.setSignupId(signup.getId());
        work.setContestId(contest.getId());
        work.setUserId(user.getId());
        work.setTitle(title);
        work.setDescription(description);
        work.setFileName("demo.pdf");
        work.setStatus("SUBMITTED");
        work.setLocked(!"SIGNUP".equals(contest.getStatus()));
        work.setSubmittedAt(time);
        work.setUpdatedAt(time);
        workMapper.insert(work);
        return work;
    }

    private void assign(Contest contest, User judge, List<Work> works) {
        for (Work work : works) {
            JudgeAssign assign = new JudgeAssign();
            assign.setContestId(contest.getId());
            assign.setJudgeId(judge.getId());
            assign.setWorkId(work.getId());
            assign.setCreatedAt(LocalDateTime.now().minusDays(2));
            assignMapper.insert(assign);
        }
    }

    private void score(Contest contest, Work work, User judge, String value, String comment) {
        Score score = new Score();
        score.setContestId(contest.getId());
        score.setWorkId(work.getId());
        score.setJudgeId(judge.getId());
        score.setScore(new BigDecimal(value));
        score.setComment(comment);
        score.setCreatedAt(LocalDateTime.now().minusDays(1));
        score.setUpdatedAt(LocalDateTime.now().minusDays(1));
        scoreMapper.insert(score);
    }

    private void notice(String title, String content, boolean pinned, LocalDateTime time) {
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setPinned(pinned);
        notice.setPublishedAt(time);
        noticeMapper.insert(notice);
    }
}
