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
    /** 该奖项奖金（元） */
    private BigDecimal prizeAmount;
    private Integer sortOrder;
}
