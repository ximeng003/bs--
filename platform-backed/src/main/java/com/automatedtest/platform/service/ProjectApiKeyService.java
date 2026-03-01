package com.automatedtest.platform.service;

import com.automatedtest.platform.entity.ProjectApiKey;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ProjectApiKeyService extends IService<ProjectApiKey> {
    String generateKey();
    ProjectApiKey verifyKey(String apiKey);
}
