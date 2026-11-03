package com.btr.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.btr.platform.mapper")
@EnableAsync
@EnableScheduling
public class BtrApplication {

    public static void main(String[] args) {
        SpringApplication.run(BtrApplication.class, args);
    }
}
