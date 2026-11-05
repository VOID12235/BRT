package com.btr.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contest_work")
public class Work {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long signupId;
    private Long contestId;
    private Long userId;
    private String title;
    private String description;
    private String fileUrl;
    private String fileName;
    /** 作品封面图 URL（上传接口 purpose=cover） */
    private String cover;
    private String status;
    private Boolean locked;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}
