package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.Project;
import com.automatedtest.platform.mapper.ProjectMapper;
import com.automatedtest.platform.service.ProjectService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {
    @Async
    @Override
    public void updateHealthScoreAsync(Integer projectId, double healthScore) {
        if (projectId == null) return;
        Project p = new Project();
        p.setId(projectId);
        p.setHealthScore(healthScore);
        updateById(p);
    }
}
