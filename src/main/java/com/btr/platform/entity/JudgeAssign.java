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
    private LocalDateTime createdAt;
}
