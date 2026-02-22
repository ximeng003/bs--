package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.dto.CaseExecuteResultDTO;
import com.automatedtest.platform.entity.TestPlan;
import com.automatedtest.platform.entity.TestReport;
import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.service.TestCaseService;
import com.automatedtest.platform.service.TestPlanService;
import com.automatedtest.platform.service.TestReportService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
public class TestPlanController {

    @Autowired
    private TestPlanService testPlanService;

    @Autowired
    private TestReportService testReportService;

    @Autowired
    private TestCaseService testCaseService;

    @GetMapping
    public Result<IPage<TestPlan>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size) {
        Page<TestPlan> pageParam = new Page<>(page, size);
        QueryWrapper<TestPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("created_at").orderByDesc("id");
        return Result.success(testPlanService.page(pageParam, queryWrapper));
    }

    @GetMapping("/{id}")
    public Result<TestPlan> getById(@PathVariable Integer id) {
        return Result.success(testPlanService.getById(id));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody TestPlan testPlan) {
        return Result.success(testPlanService.save(testPlan));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody TestPlan testPlan) {
        return Result.success(testPlanService.updateById(testPlan));
    }

    @PostMapping("/{id}/execute")
    public Result<Map<String, Object>> execute(@PathVariable Integer id, HttpServletRequest request) {
        TestPlan plan = testPlanService.getById(id);
        if (plan == null) {
            return Result.error("测试计划不存在");
        }
        String executedBy = request.getHeader("X-User-Name");
        String caseIdsStr = plan.getTestCaseIds();
        if (caseIdsStr == null || caseIdsStr.trim().isEmpty()) {
            return Result.error("测试计划未配置测试用例");
        }
        String[] parts = caseIdsStr.split(",");
        List<Integer> caseIds = new ArrayList<>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                int caseId = Integer.parseInt(trimmed);
                if (caseId > 0) {
                    caseIds.add(caseId);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (caseIds.isEmpty()) {
            return Result.error("测试计划未配置有效的测试用例");
        }

        Map<Integer, TestCase> caseMap = new HashMap<>();
        List<TestCase> caseList = testCaseService.listByIds(caseIds);
        if (caseList != null) {
            for (TestCase c : caseList) {
                if (c != null && c.getId() != null) {
                    caseMap.put(c.getId(), c);
                }
            }
        }

        Integer maxRunNo = testReportService.lambdaQuery()
                .eq(TestReport::getPlanId, id)
                .select(TestReport::getPlanRunNo)
                .list()
                .stream()
                .map(TestReport::getPlanRunNo)
                .filter(n -> n != null && n > 0)
                .max(Integer::compareTo)
                .orElse(0);
        int currentRunNo = maxRunNo + 1;

        int total = 0;
        int successCount = 0;
        int failedCount = 0;
        long totalDuration = 0L;
        List<Map<String, Object>> items = new ArrayList<>();
        Integer planSummaryReportId = null;
        for (Integer caseId : caseIds) {
            if (caseId == null || caseId <= 0) {
                continue;
            }
            total++;
            CaseExecuteResultDTO result = testCaseService.executeCaseById(caseId, executedBy, id, currentRunNo);
            boolean success = result != null && "success".equalsIgnoreCase(result.getStatus());
            if (success) {
                successCount++;
            } else {
                failedCount++;
            }
            if (result != null && result.getDurationMs() != null) {
                totalDuration += result.getDurationMs();
            }
            Map<String, Object> item = new HashMap<>();
            item.put("caseId", caseId);
            item.put("status", result != null ? result.getStatus() : "failed");
            item.put("durationMs", result != null ? result.getDurationMs() : null);
            item.put("reportId", result != null ? result.getReportId() : null);
            if (planSummaryReportId == null && result != null && result.getReportId() != null) {
                planSummaryReportId = result.getReportId();
            }
            TestCase tc = caseMap.get(caseId);
            if (tc != null) {
                item.put("caseName", tc.getName());
                item.put("caseType", tc.getType());
            }
            items.add(item);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("planId", plan.getId());
        summary.put("planName", plan.getName());
        summary.put("environment", plan.getEnvironment());
        if (executedBy != null && !executedBy.trim().isEmpty()) {
            summary.put("executedBy", executedBy.trim());
        } else {
            summary.put("executedBy", "System");
        }
        summary.put("total", total);
        summary.put("success", successCount);
        summary.put("failed", failedCount);
        summary.put("durationMs", totalDuration);
        summary.put("avgDurationMs", total > 0 ? totalDuration / total : 0L);
        summary.put("items", items);
        if (planSummaryReportId != null) {
            summary.put("planSummaryReportId", planSummaryReportId);
        }
        return Result.success(summary);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        QueryWrapper<TestReport> reportWrapper = new QueryWrapper<>();
        reportWrapper.eq("plan_id", id);
        testReportService.remove(reportWrapper);
        return Result.success(testPlanService.removeById(id));
    }

    @DeleteMapping
    public Result<Boolean> deleteAll() {
        QueryWrapper<TestReport> reportWrapper = new QueryWrapper<>();
        reportWrapper.isNotNull("plan_id");
        testReportService.remove(reportWrapper);

        QueryWrapper<TestPlan> planWrapper = new QueryWrapper<>();
        return Result.success(testPlanService.remove(planWrapper));
    }
}
