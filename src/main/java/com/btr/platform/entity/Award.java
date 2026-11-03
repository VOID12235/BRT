package com.btr.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("contest_award")
public class Award {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contestId;
    private String name;
    private Integer quota;
    /** 该奖项奖金（元），可空（奖品制奖项不设奖金） */
    private BigDecimal prizeAmount;
    /** 该奖项奖品描述，可空（奖金制奖项不设奖品） */
    private String gift;
    private Integer sortOrder;
}
