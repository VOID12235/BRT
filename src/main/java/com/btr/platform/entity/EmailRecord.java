package com.btr.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_email_record")
public class EmailRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String recipient;
    private String template;
    private String subject;
    private String status;
    private String error;
    private LocalDateTime createdAt;
}
