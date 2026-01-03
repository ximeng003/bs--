package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.TestPlan;
import com.automatedtest.platform.mapper.TestPlanMapper;
import com.automatedtest.platform.service.TestPlanService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class TestPlanServiceImpl extends ServiceImpl<TestPlanMapper, TestPlan> implements TestPlanService {
}
