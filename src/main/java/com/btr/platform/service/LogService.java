package com.btr.platform.service;

import com.btr.platform.entity.SysLog;
import com.btr.platform.mapper.SysLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogService {

    private final SysLogMapper sysLogMapper;

    public LogService(SysLogMapper sysLogMapper) {
        this.sysLogMapper = sysLogMapper;
    }

    public void record(Long userId, String action, String target, String ip) {
        SysLog log = new SysLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTarget(target);
        log.setIp(ip);
        log.setCreatedAt(LocalDateTime.now());
        sysLogMapper.insert(log);
    }
}
