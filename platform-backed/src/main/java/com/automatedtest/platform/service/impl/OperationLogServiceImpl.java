package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.OperationLog;
import com.automatedtest.platform.mapper.OperationLogMapper;
import com.automatedtest.platform.service.OperationLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Async
    @Override
    public void log(Long userId, String username, String module, String operation, String target, String details, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setModule(module);
        log.setOperation(operation);
        log.setTarget(target);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        this.save(log);
    }
}
