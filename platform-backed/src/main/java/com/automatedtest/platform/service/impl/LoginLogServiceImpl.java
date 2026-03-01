package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.LoginLog;
import com.automatedtest.platform.mapper.LoginLogMapper;
import com.automatedtest.platform.service.LoginLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {

    @Override
    public void recordLogin(Long userId, String username, String ip, String status) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setIpAddress(ip);
        log.setStatus(status);
        log.setLoginTime(LocalDateTime.now());
        // For simplicity, device and location are left empty or can be parsed from User-Agent if available in future
        log.setDevice("Web Browser"); 
        log.setLocation("Unknown");
        save(log);
    }
}
