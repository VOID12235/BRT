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
    private Integer rankNo;
    private Boolean published;
    private LocalDateTime createdAt;
}
