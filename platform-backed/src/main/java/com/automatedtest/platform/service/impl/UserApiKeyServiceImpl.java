package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.UserApiKey;
import com.automatedtest.platform.mapper.UserApiKeyMapper;
import com.automatedtest.platform.service.UserApiKeyService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserApiKeyServiceImpl extends ServiceImpl<UserApiKeyMapper, UserApiKey> implements UserApiKeyService {

    @Override
    public UserApiKey generateKey(Long userId, String name) {
        String key = "sk_live_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        
        UserApiKey apiKey = new UserApiKey();
        apiKey.setUserId(userId);
        apiKey.setName(name);
        apiKey.setAccessKey(key);
        apiKey.setStatus(1); // Active
        
        this.save(apiKey);
        return apiKey;
    }

    @Override
    public Long verifyKey(String accessKey) {
        UserApiKey key = this.getOne(new QueryWrapper<UserApiKey>().eq("access_key", accessKey));
        if (key != null && key.getStatus() == 1) {
            // Update last used time asynchronously or here
            key.setLastUsedAt(LocalDateTime.now());
            this.updateById(key);
            return key.getUserId();
        }
        return null;
    }
}
