package com.btr.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contest_judge_assign")
public class JudgeAssign {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contestId;
    private Long judgeId;
    private Long workId;
    /** 线下赛事分配对象为报名选手（signup_id），此时 work_id 为空 */
    private Long signupId;
    /** 分配类型：ALL=赛事整体（动态跟随，新作品自动进任务池） ITEM=指定作品/选手 */
    private String assignType;
    private LocalDateTime createdAt;
}
