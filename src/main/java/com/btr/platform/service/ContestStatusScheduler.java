package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.entity.Contest;
import com.btr.platform.entity.Work;
import com.btr.platform.mapper.ContestMapper;
import com.btr.platform.mapper.WorkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 赛事状态自动推进：
 *  报名中(SIGNUP) → 报名截止(signupEnd) → 进行中(REVIEW)
 *  进行中(REVIEW) → 评审开始(reviewStart) → 打分中(JUDGING)
 *  打分中(JUDGING) → 评审截止(reviewEnd) → 已结束(FINISHED，公布全部作品与结果)
 * 未配置对应时间字段的赛事不自动推进，由管理员手动变更。
 */
@Component
public class ContestStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(ContestStatusScheduler.class);

    private final ContestMapper contestMapper;
    private final WorkMapper workMapper;

    public ContestStatusScheduler(ContestMapper contestMapper, WorkMapper workMapper) {
        this.contestMapper = contestMapper;
        this.workMapper = workMapper;
    }

    @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
    public void autoAdvance() {
        LocalDateTime now = LocalDateTime.now();
        advance("SIGNUP", "REVIEW", "signup_end", now, null);
        advance("REVIEW", "JUDGING", "review_start", now, true);
        advance("JUDGING", "FINISHED", "review_end", now, true);
    }

    private void advance(String from, String to, String timeField, LocalDateTime now, Boolean lockWorks) {
        List<Contest> list = contestMapper.selectList(new QueryWrapper<Contest>()
                .eq("status", from).isNotNull(timeField).le(timeField, now));
        if (list.isEmpty()) {
            return;
        }
        for (Contest c : list) {
            try {
                c.setStatus(to);
                c.setUpdatedAt(now);
                contestMapper.updateById(c);
                if (Boolean.TRUE.equals(lockWorks) && "ONLINE".equals(c.getMode())) {
                    Work lock = new Work();
                    lock.setLocked(true);
                    workMapper.update(lock, new QueryWrapper<Work>().eq("contest_id", c.getId()));
                }
                log.info("赛事状态自动推进：{} (id={}) {} → {}", c.getTitle(), c.getId(), from, to);
            } catch (Exception e) {
                log.error("赛事状态自动推进失败：id={} {} → {}：{}", c.getId(), from, to, e.getMessage());
            }
        }
    }
}
