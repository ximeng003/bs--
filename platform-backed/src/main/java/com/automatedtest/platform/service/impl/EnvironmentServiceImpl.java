package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.Environment;
import com.automatedtest.platform.mapper.EnvironmentMapper;
import com.automatedtest.platform.service.EnvironmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentServiceImpl extends ServiceImpl<EnvironmentMapper, Environment> implements EnvironmentService {
}
