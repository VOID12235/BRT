package com.btr.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contest_signup")
public class Signup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contestId;
    private Long userId;
    private String realName;
    private String school;
    private String contact;
    private String intro;
    private String status;
    private LocalDateTime createdAt;
}
