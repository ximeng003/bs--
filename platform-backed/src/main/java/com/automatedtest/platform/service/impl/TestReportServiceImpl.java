package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.entity.TestPlan;
import com.automatedtest.platform.entity.TestReport;
import com.automatedtest.platform.mapper.TestReportMapper;
import com.automatedtest.platform.service.TestCaseService;
import com.automatedtest.platform.service.TestPlanService;
import com.automatedtest.platform.service.TestReportService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestReportServiceImpl extends ServiceImpl<TestReportMapper, TestReport> implements TestReportService {

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private TestPlanService testPlanService;

    @Override
    public IPage<TestReport> listUnified(Page<TestReport> page, Integer projectId, String status, String keyword, String date) {
        IPage<TestReport> pageResult = baseMapper.selectUnifiedReports(page, projectId, status, keyword, date);
        List<TestReport> records = pageResult.getRecords();
        if (records == null || records.isEmpty()) {
            return pageResult;
        }

        Set<Integer> caseIds = records.stream()
                .map(TestReport::getCaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Set<Integer> planIds = records.stream()
                .map(TestReport::getPlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, TestCase> caseMap = new HashMap<>();
        if (!caseIds.isEmpty()) {
            testCaseService.listByIds(caseIds).forEach(c -> caseMap.put(c.getId(), c));
        }

        Map<Integer, TestPlan> planMap = new HashMap<>();
        if (!planIds.isEmpty()) {
            testPlanService.listByIds(planIds).forEach(p -> planMap.put(p.getId(), p));
        }

        for (TestReport r : records) {
            if (r.getCaseId() != null) {
                TestCase c = caseMap.get(r.getCaseId());
                if (c != null) {
                    r.setCaseName(c.getName());
                    r.setCaseType(c.getType());
                    if (r.getEnvironment() == null || r.getEnvironment().isEmpty()) {
                        r.setEnvironment(c.getEnvironment());
                    }
                }
            }
            if (r.getPlanId() != null) {
                TestPlan p = planMap.get(r.getPlanId());
                if (p != null) {
                    r.setPlanName(p.getName());
                    if (r.getEnvironment() == null || r.getEnvironment().isEmpty()) {
                        r.setEnvironment(p.getEnvironment());
                    }
                }
            }
        }

        return pageResult;
    }
}
