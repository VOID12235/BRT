package com.btr.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("contest_score")
public class Score {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contestId;
    private Long workId;
    /** 线下赛事按报名选手打分，signup_id 指向报名记录 */
    private Long signupId;
    private Long judgeId;
    private BigDecimal score;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
