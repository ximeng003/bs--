package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.ProjectApiKey;
import com.automatedtest.platform.mapper.ProjectApiKeyMapper;
import com.automatedtest.platform.service.ProjectApiKeyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ProjectApiKeyServiceImpl extends ServiceImpl<ProjectApiKeyMapper, ProjectApiKey> implements ProjectApiKeyService {

    @Override
    public String generateKey() {
        return "pk_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public ProjectApiKey verifyKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }
        ProjectApiKey key = getOne(new QueryWrapper<ProjectApiKey>().eq("api_key", apiKey));
        if (key != null) {
            if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
                return null;
            }
            return key;
        }
        return null;
    }
}
