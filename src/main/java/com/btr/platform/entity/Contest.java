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
    /** 赛题附件 URL（题目文档/文件，进行中阶段对选手展示），可为空 */
    private String topicFileUrl;
    /** 赛题附件名称 */
    private String topicFileName;
    private LocalDateTime signupStart;
    private LocalDateTime signupEnd;
    private LocalDateTime reviewStart;
    private LocalDateTime reviewEnd;
    private String status;
    private Boolean isPublic;
    /** 每个项目评委数（多评委互评） */
    private Integer judgePerWork;
    /** 聚合模式：AVG=均分 TRIM=去极值 */
    private String aggregateMode;
    /** 评审截止时间 */
    private LocalDateTime reviewDeadline;
    /** 成绩固化锁定 */
    private Boolean resultsLocked;
    /** 固化时间 */
    private LocalDateTime finalizedAt;
    /** 公示时间 */
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
