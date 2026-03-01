package com.automatedtest.platform.service;

import com.automatedtest.platform.entity.OperationLog;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OperationLogService extends IService<OperationLog> {
    void log(Long userId, String username, String module, String operation, String target, String details, String ipAddress);
}
