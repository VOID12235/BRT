package com.btr.platform.dto;

import com.btr.platform.entity.Award;
import com.btr.platform.entity.Signup;
import com.btr.platform.entity.Work;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class Views {

    private Views() {
    }

    @Data
    public static class WorkScore {
        private Long workId;
        private BigDecimal avgScore;
        private Long scoreCount;
    }

    @Data
    public static class ContestCard {
        private Long id;
        private String title;
        private String cover;
        private String summary;
        private String description;
        private String rules;
        /** 赛事奖金总额（元） */
        private BigDecimal prizeAmount;
        private LocalDateTime signupStart;
        private LocalDateTime signupEnd;
        private LocalDateTime reviewStart;
        private LocalDateTime reviewEnd;
        private String status;
        private Boolean isPublic;
        private LocalDateTime createdAt;
        private long signupCount;
        private long workCount;
    }

    @Data
    public static class ContestDetail {
        private ContestCard contest;
        private List<Award> awards;
        private Signup mySignup;
        private Work myWork;
        private long judgeCount;
    }

    @Data
    public static class UserView {
        private Long id;
        private String account;
        private String nickname;
        private String email;
        private String phone;
        private String role;
        private String status;
        private String avatar;
        private LocalDateTime createdAt;
    }

    @Data
    public static class AuthResult {
        private String accessToken;
        private String refreshToken;
        private UserView user;
    }

    @Data
    public static class SignupView {
        private Long id;
        private Long contestId;
        private String contestTitle;
        private Long userId;
        private String account;
        private String nickname;
        private String realName;
        private String school;
        private String contact;
        private String intro;
        private String status;
        private LocalDateTime createdAt;
        private Long workId;
        private String workTitle;
    }

    @Data
    public static class WorkView {
        private Long id;
        private Long contestId;
        private String contestTitle;
        private Long signupId;
        private Long userId;
        private String author;
        private String school;
        private String title;
        private String description;
        private String fileUrl;
        private String fileName;
        private String status;
        private Boolean locked;
        private LocalDateTime submittedAt;
        private long scoreCount;
        private BigDecimal avgScore;
    }

    @Data
    public static class JudgeTask {
        private Long assignId;
        private Long workId;
        private String workTitle;
        private String workDescription;
        private String fileUrl;
        private String fileName;
        private LocalDateTime submittedAt;
        private Long contestId;
        private String contestTitle;
        private String realName;
        private String school;
        private Long scoreId;
        private BigDecimal score;
        private String comment;
    }

    @Data
    public static class AssignView {
        private Long id;
        private Long contestId;
        private String contestTitle;
        private Long judgeId;
        private String judgeName;
        private Long workId;
        private String workTitle;
        private LocalDateTime createdAt;
    }

    @Data
    public static class JudgeProgress {
        private Long contestId;
        private String contestTitle;
        private long assigned;
        private long scored;
    }

    @Data
    public static class RankRow {
        private int rank;
        private Long workId;
        private String workTitle;
        private String author;
        private String school;
        private BigDecimal avgScore;
        private long scoreCount;
        private String awardName;
        /** 该获奖者所得奖金（元），未获奖为 null */
        private BigDecimal prizeAmount;
    }

    @Data
    public static class RankGroup {
        private Long awardId;
        private String awardName;
        private Integer quota;
        private BigDecimal prizeAmount;
        private List<RankRow> items;
    }

    @Data
    public static class RankResult {
        private Long contestId;
        private String contestTitle;
        private String status;
        private boolean published;
        private LocalDateTime updatedAt;
        private List<RankRow> rows;
        private List<RankGroup> groups;
        private long workCount;
        private long judgeCount;
        private long scoredCount;
    }

    @Data
    public static class TrendPoint {
        private String date;
        private long count;
    }

    @Data
    public static class Dashboard {
        private long users;
        private long players;
        private long judges;
        private long contests;
        private long signups;
        private long works;
        private long pendingReviews;
        private long notices;
        private List<TrendPoint> trend;
        private List<SignupView> recentSignups;
    }

    @Data
    public static class UploadResult {
        private Long id;
        private String url;
        private String name;
        private long size;
    }

    /* ------------------------- 系统配置视图 ------------------------- */

    @Data
    public static class MailConfig {
        private String host;
        private Integer port;
        private String username;
        /** 回显时一律为脱敏值，不暴露真实密码 */
        private String password;
        private String from;
        private Boolean ssl;
        private Boolean enabled;
    }

    @Data
    public static class DbConfig {
        private String host;
        private Integer port;
        private String database;
        private String username;
        /** 回显时一律为脱敏值，不暴露真实密码 */
        private String password;
    }

    @Data
    public static class RedisConfig {
        private String host;
        private Integer port;
        /** 回显时一律为脱敏值，不暴露真实密码 */
        private String password;
        private Integer database;
    }

    @Data
    public static class SiteConfig {
        private String name;
        private String logoUrl;
        private String footer;
    }

    @Data
    public static class SwitchesConfig {
        private Boolean registerEnabled;
        private Boolean captchaEnabled;
    }

    @Data
    public static class TemplateConfig {
        private String key;
        private String content;
        private String description;
    }

    @Data
    public static class UploadConfig {
        private Integer maxSizeMb;
        private String allowedTypes;
    }

    @Data
    public static class SecurityConfig {
        private Integer loginFailLimit;
        private Integer loginLockMinutes;
    }

    @Data
    public static class LogView {
        private Long id;
        private Long userId;
        private String account;
        private String nickname;
        private String action;
        private String target;
        private String ip;
        private LocalDateTime createdAt;
    }

    @Data
    public static class MailRecordView {
        private Long id;
        private String recipient;
        private String template;
        private String subject;
        private String status;
        private String error;
        private LocalDateTime createdAt;
    }
}
