package com.automatedtest.platform.service;

import com.automatedtest.platform.entity.LoginLog;
import com.baomidou.mybatisplus.extension.service.IService;

public interface LoginLogService extends IService<LoginLog> {
    void recordLogin(Long userId, String username, String ip, String status);
}
