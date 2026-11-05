package com.btr.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String account;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String role;
    private String status;
    private String avatar;
    /** 令牌版本：改密、禁用、角色变更时自增，使已签发的 Token 立即失效。 */
    private Integer tokenVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
