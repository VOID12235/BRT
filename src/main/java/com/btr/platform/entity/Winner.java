package com.btr.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contest_winner")
public class Winner {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contestId;
    private Long awardId;
    private Long workId;
    /** 线下赛事获奖对象为报名选手 */
    private Long signupId;
    private Integer rankNo;
    private Boolean published;
    private LocalDateTime createdAt;
}
