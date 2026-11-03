package com.btr.platform.service;

import com.btr.platform.entity.EmailRecord;
import com.btr.platform.mapper.EmailRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailRecordMapper emailRecordMapper;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider, EmailRecordMapper emailRecordMapper) {
        this.mailSenderProvider = mailSenderProvider;
        this.emailRecordMapper = emailRecordMapper;
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

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
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

    public void signupReceipt(String email, String nickname, String contestTitle) {
        send(email, "SIGNUP_RECEIPT", "报名成功 · " + contestTitle,
                "<p>Hi " + nickname + "，</p><p>你已成功报名 <b>" + contestTitle + "</b>，请留意作品提交截止时间。</p>");
    }

    public void reviewReminder(String email, String nickname, String contestTitle) {
        send(email, "REVIEW_REMINDER", "评审提醒 · " + contestTitle,
                "<p>Hi " + nickname + "，</p><p><b>" + contestTitle + "</b> 的评审已开始，请尽快完成分配作品的打分。</p>");
    }

    public void winnerNotice(String email, String nickname, String contestTitle, String awardName) {
        send(email, "WINNER_NOTICE", "获奖通知 · " + contestTitle,
                "<p>Hi " + nickname + "，</p><p>恭喜你在 <b>" + contestTitle + "</b> 中获得 <b>" + awardName + "</b>！</p>");
    }
}
