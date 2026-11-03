package com.btr.platform.service;

import com.btr.platform.entity.EmailRecord;
import com.btr.platform.mapper.EmailRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

/**
 * 邮件服务：优先使用管理后台配置的 SMTP（动态构建 sender，改完即时生效）；
 * 未配置时回退 application.yml 的 spring.mail.*（若有），否则标记 SKIPPED 不阻塞业务。
 */
@Slf4j
@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailRecordMapper emailRecordMapper;
    private final ConfigService configService;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        EmailRecordMapper emailRecordMapper,
                        ConfigService configService) {
        this.mailSenderProvider = mailSenderProvider;
        this.emailRecordMapper = emailRecordMapper;
        this.configService = configService;
    }

    @Async
    public void send(String recipient, String template, String subject, String html) {
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        EmailRecord record = new EmailRecord();
        record.setRecipient(recipient);
        record.setTemplate(template);
        record.setSubject(subject);
        record.setStatus("PENDING");
        record.setCreatedAt(LocalDateTime.now());
        emailRecordMapper.insert(record);

        JavaMailSender sender = resolveSender();
        if (sender == null) {
            record.setStatus("SKIPPED");
            record.setError("未配置 SMTP，开发环境跳过发送");
            emailRecordMapper.updateById(record);
            log.info("[mail:{}] to={} subject={}", template, recipient, subject);
            return;
        }
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            record.setStatus("SENT");
        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setError(e.getMessage() == null ? "发送失败" : e.getMessage().substring(0, Math.min(480, e.getMessage().length())));
            log.warn("邮件发送失败 to={}: {}", recipient, e.getMessage());
        }
        emailRecordMapper.updateById(record);
    }

    /**
     * 解析邮件发送器：管理后台配置优先，其次 yml 兜底。
     *
     * @return 可用 sender，无法发送时返回 null
     */
    private JavaMailSender resolveSender() {
        ConfigService.MailRuntime runtime = configService.mailRuntime();
        if (runtime.enabled && runtime.host != null && !runtime.host.isBlank()) {
            try {
                JavaMailSenderImpl sender = new JavaMailSenderImpl();
                sender.setHost(runtime.host);
                sender.setPort(runtime.port);
                if (runtime.username != null && !runtime.username.isBlank()) {
                    sender.setUsername(runtime.username);
                    sender.setPassword(runtime.password);
                }
                Properties props = sender.getJavaMailProperties();
                props.put("mail.smtp.auth", "true");
                if (runtime.ssl) {
                    props.put("mail.smtp.ssl.enable", "true");
                } else {
                    props.put("mail.smtp.starttls.enable", "true");
                }
                props.put("mail.smtp.timeout", "10000");
                props.put("mail.smtp.connectiontimeout", "10000");
                return sender;
            } catch (Exception e) {
                log.warn("动态构建 SMTP sender 失败：{}", e.getMessage());
            }
        }
        JavaMailSender yml = mailSenderProvider.getIfAvailable();
        return yml;
    }

    /** 发送测试邮件（同步，返回是否成功） */
    public String sendTest(String to) {
        EmailRecord record = new EmailRecord();
        record.setRecipient(to);
        record.setTemplate("MAIL_TEST");
        record.setSubject("BTR 平台 · 邮件配置测试");
        record.setStatus("PENDING");
        record.setCreatedAt(LocalDateTime.now());
        emailRecordMapper.insert(record);

        JavaMailSender sender = resolveSender();
        if (sender == null) {
            record.setStatus("SKIPPED");
            record.setError("未配置 SMTP 或邮件开关未开启");
            emailRecordMapper.updateById(record);
            return "未配置 SMTP 或邮件开关未开启，请先在基础配置中填写 SMTP 并开启邮件";
        }
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject("BTR 平台 · 邮件配置测试");
            helper.setText("<p>这是一封测试邮件，说明 BTR 平台的邮件服务已配置成功。</p>", true);
            sender.send(message);
            record.setStatus("SENT");
            emailRecordMapper.updateById(record);
            return null;
        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setError(e.getMessage() == null ? "发送失败" : e.getMessage().substring(0, Math.min(480, e.getMessage().length())));
            emailRecordMapper.updateById(record);
            log.warn("测试邮件发送失败 to={}: {}", to, e.getMessage());
            return "发送失败：" + e.getMessage();
        }
    }

    /* ------------------------- 业务邮件 ------------------------- */

    public void signupReceipt(String email, String nickname, String contestTitle) {
        String tpl = configService.getTemplate(ConfigService.KEY_TPL_SIGNUP,
                "<p>Hi {nickname}，</p><p>你已成功报名 <b>{contestTitle}</b>，请留意作品提交截止时间。</p>");
        send(email, "SIGNUP_RECEIPT", "报名成功 · " + contestTitle,
                ConfigService.render(tpl, Map.of("nickname", nickname, "contestTitle", contestTitle)));
    }

    public void reviewReminder(String email, String nickname, String contestTitle) {
        String tpl = configService.getTemplate(ConfigService.KEY_TPL_REVIEW,
                "<p>Hi {nickname}，</p><p><b>{contestTitle}</b> 的评审已开始，请尽快完成分配作品的打分。</p>");
        send(email, "REVIEW_REMINDER", "评审提醒 · " + contestTitle,
                ConfigService.render(tpl, Map.of("nickname", nickname, "contestTitle", contestTitle)));
    }

    public void winnerNotice(String email, String nickname, String contestTitle, String awardName) {
        String tpl = configService.getTemplate(ConfigService.KEY_TPL_WINNER,
                "<p>Hi {nickname}，</p><p>恭喜你在 <b>{contestTitle}</b> 中获得 <b>{awardName}</b>！</p>");
        send(email, "WINNER_NOTICE", "获奖通知 · " + contestTitle,
                ConfigService.render(tpl, Map.of("nickname", nickname, "contestTitle", contestTitle, "awardName", awardName)));
    }

    /** 发送邮箱验证码（忘记密码等场景） */
    public void sendVerifyCode(String email, String nickname, String code) {
        String tpl = configService.getTemplate(ConfigService.KEY_TPL_VERIFY,
                "<p>Hi {nickname}，</p><p>你的验证码是：<b style=\"font-size:18px\">{code}</b></p>"
                        + "<p>验证码 10 分钟内有效，请勿泄露给他人。</p>");
        send(email, "VERIFY_CODE", "验证码 · BTR 赛事平台",
                ConfigService.render(tpl, Map.of("nickname", nickname == null ? "" : nickname, "code", code)));
    }
}
