package com.btr.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_read")
public class NoticeRead {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long noticeId;
    private Long userId;
    private LocalDateTime readAt;
}
