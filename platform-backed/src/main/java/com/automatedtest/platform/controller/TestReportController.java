package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.dto.ReportDetailDTO;
import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.entity.TestPlan;
import com.automatedtest.platform.entity.TestReport;
import com.automatedtest.platform.service.TestReportService;
import com.automatedtest.platform.service.TestCaseService;
import com.automatedtest.platform.service.TestPlanService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class TestReportController {

    @Autowired
    private TestReportService testReportService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private TestPlanService testPlanService;

    @GetMapping
    public Result<IPage<TestReport>> list(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String date,
                                          @RequestParam(required = false) Integer planId,
                                          @RequestParam(required = false, name = "planRunNo") Integer planRunNo) {
        Page<TestReport> pageParam = new Page<>(page, size);
        QueryWrapper<TestReport> queryWrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            queryWrapper.eq("status", status);
        }

        if (planId != null && planId > 0) {
            queryWrapper.eq("plan_id", planId);
        }

        if (planRunNo != null && planRunNo > 0) {
            queryWrapper.eq("plan_run_no", planRunNo);
        }
        
        // Keyword search might need joins with Plan/Case names, but for now we just search ID or logs
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("logs", keyword).or().eq("id", keyword);
        }

        if (StringUtils.hasText(date)) {
            try {
                LocalDate localDate = LocalDate.parse(date);
                LocalDateTime start = localDate.atStartOfDay();
                LocalDateTime end = localDate.plusDays(1).atStartOfDay();
                queryWrapper.ge("executed_at", start);
                queryWrapper.lt("executed_at", end);
            } catch (Exception ignored) {
            }
        }
        
        queryWrapper.orderByDesc("executed_at");
        queryWrapper.orderByDesc("id");

        IPage<TestReport> pageResult = testReportService.page(pageParam, queryWrapper);

        if (pageResult.getRecords() != null && !pageResult.getRecords().isEmpty()) {
            java.util.List<TestReport> records = pageResult.getRecords();
            java.util.Set<Integer> caseIds = new java.util.HashSet<>();
            java.util.Set<Integer> planIds = new java.util.HashSet<>();
            for (TestReport r : records) {
                if (r.getCaseId() != null) {
                    caseIds.add(r.getCaseId());
                }
                if (r.getPlanId() != null) {
                    planIds.add(r.getPlanId());
                }
            }
            java.util.Map<Integer, TestCase> caseMap = new java.util.HashMap<>();
            if (!caseIds.isEmpty()) {
                java.util.List<TestCase> cases = testCaseService.listByIds(caseIds);
                for (TestCase c : cases) {
                    if (c.getId() != null) {
                        caseMap.put(c.getId(), c);
                    }
                }
            }
            java.util.Map<Integer, TestPlan> planMap = new java.util.HashMap<>();
            if (!planIds.isEmpty()) {
                java.util.List<TestPlan> plans = testPlanService.listByIds(planIds);
                for (TestPlan p : plans) {
                    if (p.getId() != null) {
                        planMap.put(p.getId(), p);
                    }
                }
            }
            for (TestReport r : records) {
                if (r.getCaseId() != null) {
                    TestCase c = caseMap.get(r.getCaseId());
                    if (c != null) {
                        r.setCaseName(c.getName());
                        r.setEnvironment(c.getEnvironment());
                        r.setCaseType(c.getType());
                        if (r.getExecutedAt() == null) {
                            r.setExecutedAt(c.getLastRun());
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
        }

        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<ReportDetailDTO> getById(@PathVariable Integer id) {
        TestReport report = testReportService.getById(id);
        if (report == null) {
            return Result.error("测试报告不存在");
        }
        ReportDetailDTO dto = new ReportDetailDTO();
        dto.setId(report.getId());
        dto.setPlanId(report.getPlanId());
        dto.setPlanRunNo(report.getPlanRunNo());
        dto.setCaseId(report.getCaseId());
        dto.setStatus(report.getStatus());
        dto.setExecutionTime(report.getExecutionTime());
        dto.setLogs(report.getLogs());
        dto.setExecutedAt(report.getExecutedAt());
        dto.setExecutedBy(report.getExecutedBy());

        if (report.getCaseId() != null) {
            TestCase testCase = testCaseService.getById(report.getCaseId());
            if (testCase != null) {
                dto.setCaseName(testCase.getName());
                dto.setCaseType(testCase.getType());
                dto.setEnvironment(testCase.getEnvironment());
            }
        }

        return Result.success(dto);
    }
    
    // Create report is usually done internally by execution engine, but expose it for now
    @PostMapping
    public Result<Boolean> save(@RequestBody TestReport testReport) {
        return Result.success(testReportService.save(testReport));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return Result.success(testReportService.removeById(id));
    }

    @DeleteMapping
    public Result<Boolean> deleteBatch(@RequestBody(required = false) List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            QueryWrapper<TestReport> queryWrapper = new QueryWrapper<>();
            return Result.success(testReportService.remove(queryWrapper));
        }
        return Result.success(testReportService.removeBatchByIds(ids));
    }
}
