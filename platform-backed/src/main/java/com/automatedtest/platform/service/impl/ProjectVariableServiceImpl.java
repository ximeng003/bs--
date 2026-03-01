package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.ProjectVariable;
import com.automatedtest.platform.mapper.ProjectVariableMapper;
import com.automatedtest.platform.service.ProjectVariableService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProjectVariableServiceImpl extends ServiceImpl<ProjectVariableMapper, ProjectVariable> implements ProjectVariableService {
}
