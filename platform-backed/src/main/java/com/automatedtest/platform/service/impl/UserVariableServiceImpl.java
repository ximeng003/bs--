package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.UserVariable;
import com.automatedtest.platform.mapper.UserVariableMapper;
import com.automatedtest.platform.service.UserVariableService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserVariableServiceImpl extends ServiceImpl<UserVariableMapper, UserVariable> implements UserVariableService {
}
