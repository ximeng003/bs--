package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.mapper.TestCaseMapper;
import com.automatedtest.platform.service.TestCaseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class TestCaseServiceImpl extends ServiceImpl<TestCaseMapper, TestCase> implements TestCaseService {
}
