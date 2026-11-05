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
            @NotBlank(message = "请输入邮箱") @Email(message = "邮箱格式不正确") String email,
            @NotBlank(message = "请输入验证码") String code,
            @NotBlank(message = "请输入密码")
            @Size(min = 6, max = 32, message = "密码长度为 6-32 位")
            String password,
            @NotBlank(message = "请输入昵称")
            @Size(max = 32, message = "昵称最长 32 个字符")
            String nickname,
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
            @DecimalMin(value = "0.00", message = "奖金不能为负") @DecimalMax(value = "999999999.99", message = "奖金数额过大")
            BigDecimal prizeAmount,
            String mode,
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
            String fileName,
            String cover) {
    }

    public record ScoreReq(
            Long workId,
            /** 线下赛事打分对象为报名选手 */
            Long signupId,
            @NotNull(message = "请输入分数")
            @DecimalMin(value = "0", message = "分数不能低于 0")
            @DecimalMax(value = "100", message = "分数不能高于 100")
            BigDecimal score,
            @NotBlank(message = "请填写评语") @Size(max = 1000, message = "评语最长 1000 字") String comment) {
    }

    public record AwardReq(
            @NotBlank(message = "请输入奖项名称") @Size(max = 64, message = "奖项名称过长") String name,
            @NotNull(message = "请填写名额") @Min(value = 1, message = "名额至少 1 个") @Max(value = 100, message = "名额过多") Integer quota,
            @DecimalMin(value = "0.00", message = "奖金不能为负")
            @DecimalMax(value = "999999999.99", message = "奖金数额过大") BigDecimal prizeAmount,
            @Size(max = 255, message = "奖品描述过长") String gift,
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

    /** 管理员手动调整榜单：每条指定获奖对象（workId 或 signupId 二选一）、奖项与名次 */
    public record WinnerEditReq(
            @NotNull(message = "缺少获奖对象") Long subjectId,
            @NotNull(message = "请选择奖项") Long awardId,
            @NotNull(message = "请填写名次") Integer rankNo) {
    }

    public record LockReq(@NotNull(message = "缺少锁定状态") Boolean locked) {
    }

    public record UserStatusReq(@NotBlank(message = "缺少状态") String status) {
    }

    /* ------------------------- 系统配置请求 ------------------------- */

    public record MailConfigReq(
            String host,
            Integer port,
            String username,
            /** 为空或 "******" 时表示保留原密码 */
            String password,
            String from,
            Boolean ssl,
            Boolean enabled) {
    }

    public record MailTestReq(
            @NotBlank(message = "请输入收件邮箱") @Email(message = "邮箱格式不正确") String to) {
    }

    public record DbConfigReq(
            String host,
            @Min(value = 1, message = "端口不合法") @Max(value = 65535, message = "端口不合法") Integer port,
            String database,
            String username,
            /** 为空或 "******" 时表示保留原密码 */
            String password) {
    }

    public record RedisConfigReq(
            String host,
            @Min(value = 1, message = "端口不合法") @Max(value = 65535, message = "端口不合法") Integer port,
            /** 为空或 "******" 时表示保留原密码 */
            String password,
            @Min(value = 0, message = "数据库索引不能为负") @Max(value = 15, message = "数据库索引最大 15") Integer database) {
    }

    public record SiteConfigReq(String name, String logoUrl, String footer,
                                String aboutIntro, String aboutRules, String aboutContact) {
    }

    public record SwitchesConfigReq(Boolean registerEnabled, Boolean captchaEnabled) {
    }

    public record TemplateConfigReq(@NotBlank(message = "模板内容不能为空") String content) {
    }

    public record UploadConfigReq(Integer maxSizeMb, String allowedTypes) {
    }

    public record SecurityConfigReq(Integer loginFailLimit, Integer loginLockMinutes) {
    }

    /* ------------------------- 账号管理请求 ------------------------- */

    public record AdminCreateUserReq(
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
            @NotBlank(message = "请选择角色") String role) {
    }

    public record ResetPasswordReq(
            @NotBlank(message = "请输入新密码")
            @Size(min = 6, max = 32, message = "密码长度为 6-32 位")
            String newPassword) {
    }

    public record ChangeRoleReq(@NotBlank(message = "请选择角色") String role) {
    }

    /* ------------------------- 忘记密码请求 ------------------------- */

    public record ForgotPasswordReq(
            @NotBlank(message = "请输入邮箱") @Email(message = "邮箱格式不正确") String email) {
    }

    public record ResetPasswordByCodeReq(
            @NotBlank(message = "请输入邮箱") @Email(message = "邮箱格式不正确") String email,
            @NotBlank(message = "请输入验证码") String code,
            @NotBlank(message = "请输入新密码")
            @Size(min = 6, max = 32, message = "密码长度为 6-32 位")
            String newPassword) {
    }

    /* ------------------------- 个人资料请求 ------------------------- */

    public record UpdateProfileReq(
            @NotBlank(message = "昵称不能为空")
            @Size(max = 32, message = "昵称最长 32 个字符")
            String nickname,
            @Size(max = 20, message = "手机号最长 20 位") String phone) {
    }
}
