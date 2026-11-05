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
    /** 比赛类型：ONLINE 线上 / OFFLINE 线下 */
    private String mode;
    /** 线下赛事录像视频 URL（管理员上传），可为空 */
    private String videoUrl;
    /** 线下赛事录像视频名称 */
    private String videoName;
    private LocalDateTime signupStart;
    private LocalDateTime signupEnd;
    private LocalDateTime reviewStart;
    private LocalDateTime reviewEnd;
    private String status;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
