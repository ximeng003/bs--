package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.TestReport;
import com.automatedtest.platform.mapper.TestReportMapper;
import com.automatedtest.platform.service.TestReportService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class TestReportServiceImpl extends ServiceImpl<TestReportMapper, TestReport> implements TestReportService {
}
