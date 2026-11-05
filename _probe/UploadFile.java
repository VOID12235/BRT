package com.btr.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("upload_file")
public class UploadFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String url;
    private String md5;
    private Long size;
    private String originalName;
    private Long uploaderId;
    private String purpose;
    private LocalDateTime createdAt;
}
