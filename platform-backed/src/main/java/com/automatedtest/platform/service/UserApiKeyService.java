package com.automatedtest.platform.service;

import com.automatedtest.platform.entity.UserApiKey;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserApiKeyService extends IService<UserApiKey> {
    /**
     * Generate a new API Key for user
     */
    UserApiKey generateKey(Long userId, String name);

    /**
     * Verify API Key and return user ID
     */
    Long verifyKey(String accessKey);
}
