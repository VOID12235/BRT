package com.btr.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btr.platform.entity.User;
import com.btr.platform.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 生产环境首次启动时创建管理员账号。
 * 通过 ADMIN_PASSWORD 环境变量启用；账号已存在时不会覆盖。
 */
@Slf4j
@Component
@Order(0)
public class AdminBootstrap implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.account:admin}")
    private String account;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Value("${app.bootstrap-admin.nickname:平台管理员}")
    private String nickname;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    public AdminBootstrap(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (password == null || password.isBlank()) {
            return;
        }
        Long exists = userMapper.selectCount(new QueryWrapper<User>().eq("account", account));
        if (exists != null && exists > 0) {
            return;
        }
        User admin = new User();
        admin.setAccount(account);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setNickname(nickname);
        admin.setEmail(email == null || email.isBlank() ? null : email);
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        userMapper.insert(admin);
        log.info("已创建管理员账号：{}", account);
    }
}
