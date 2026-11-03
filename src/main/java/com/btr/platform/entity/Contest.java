package com.btr.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("contest")
public class Contest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String cover;
    private String summary;
    private String description;
    private String rules;
    /** 赛事奖金总额（元） */
    private BigDecimal prizeAmount;
    private LocalDateTime signupStart;
    private LocalDateTime signupEnd;
    private LocalDateTime reviewStart;
    private LocalDateTime reviewEnd;
    private String status;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
