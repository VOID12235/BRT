package com.btr.platform.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class Requests {

    private Requests() {
    }

    public record RegisterReq(
            @NotBlank(message = "请输入账号")
            @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "账号为 4-32 位字母、数字或下划线")
            String account,
            @NotBlank(message = "请输入密码")
            @Size(min = 6, max = 32, message = "密码长度为 6-32 位")
            String password,
            @NotBlank(message = "请输入昵称")
            @Size(max = 32, message = "昵称最长 32 个字符")
            String nickname,
            @Email(message = "邮箱格式不正确") String email,
            String phone) {
    }

    public record LoginReq(
            @NotBlank(message = "请输入账号") String account,
            @NotBlank(message = "请输入密码") String password,
            String captchaKey,
            String captchaCode) {
    }

    public record RefreshReq(@NotBlank(message = "缺少 refreshToken") String refreshToken) {
    }

    public record ContestReq(
            @NotBlank(message = "请输入赛事名称") @Size(max = 160, message = "赛事名称过长") String title,
            String cover,
            @Size(max = 500, message = "简介最长 500 字") String summary,
            String description,
            String rules,
            LocalDateTime signupStart,
            LocalDateTime signupEnd,
            LocalDateTime reviewStart,
            LocalDateTime reviewEnd,
            String status,
            Boolean isPublic) {
    }

    public record StatusReq(@NotBlank(message = "缺少状态") String status) {
    }

    public record SignupReq(
            @NotNull(message = "请选择赛事") Long contestId,
            @NotBlank(message = "请填写真实姓名") @Size(max = 64, message = "姓名过长") String realName,
            @Size(max = 128, message = "学校名称过长") String school,
            @NotBlank(message = "请填写联系方式") @Size(max = 64, message = "联系方式过长") String contact,
            @Size(max = 1000, message = "自我介绍最长 1000 字") String intro) {
    }

    public record SignupStatusReq(@NotBlank(message = "缺少状态") String status) {
    }

    public record WorkReq(
            @NotNull(message = "请选择报名记录") Long signupId,
            @NotBlank(message = "请输入作品名称") @Size(max = 160, message = "作品名称过长") String title,
            @Size(max = 2000, message = "作品描述最长 2000 字") String description,
            String fileUrl,
            String fileName) {
    }

    public record ScoreReq(
            @NotNull(message = "缺少作品") Long workId,
            @NotNull(message = "请输入分数")
            @DecimalMin(value = "0", message = "分数不能低于 0")
            @DecimalMax(value = "100", message = "分数不能高于 100")
            BigDecimal score,
            @NotBlank(message = "请填写评语") @Size(max = 1000, message = "评语最长 1000 字") String comment) {
    }

    public record AwardReq(
            @NotBlank(message = "请输入奖项名称") @Size(max = 64, message = "奖项名称过长") String name,
            @NotNull(message = "请填写名额") @Min(value = 1, message = "名额至少 1 个") @Max(value = 100, message = "名额过多") Integer quota,
            @DecimalMin(value = "0", message = "最低分不能低于 0")
            @DecimalMax(value = "100", message = "最低分不能高于 100") BigDecimal minScore,
            Integer sortOrder) {
    }

    public record NoticeReq(
            @NotBlank(message = "请输入公告标题") @Size(max = 160, message = "标题过长") String title,
            @NotBlank(message = "请输入公告内容") String content,
            Boolean pinned) {
    }

    public record AssignReq(
            @NotNull(message = "请选择赛事") Long contestId,
            @NotNull(message = "请选择评委") Long judgeId,
            List<Long> workIds) {
    }

    public record LockReq(@NotNull(message = "缺少锁定状态") Boolean locked) {
    }

    public record UserStatusReq(@NotBlank(message = "缺少状态") String status) {
    }
}
