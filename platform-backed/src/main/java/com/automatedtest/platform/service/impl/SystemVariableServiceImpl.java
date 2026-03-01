package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.SystemVariable;
import com.automatedtest.platform.mapper.SystemVariableMapper;
import com.automatedtest.platform.service.SystemVariableService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SystemVariableServiceImpl extends ServiceImpl<SystemVariableMapper, SystemVariable> implements SystemVariableService {
}
