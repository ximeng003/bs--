package com.automatedtest.platform.controller;

import com.automatedtest.platform.annotation.OperationAudit;
import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.dto.CaseExecuteResultDTO;
import com.automatedtest.platform.entity.TestPlan;
import com.automatedtest.platform.entity.TestReport;
import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.entity.Project;
import com.automatedtest.platform.entity.TeamMember;
import com.automatedtest.platform.service.ProjectService;
import com.automatedtest.platform.service.TeamMemberService;
import com.automatedtest.platform.service.TestCaseService;
import com.automatedtest.platform.service.TestPlanService;
import com.automatedtest.platform.service.TestReportService;
import com.automatedtest.platform.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.automatedtest.platform.util.WebhookClient;

@RestController
@RequestMapping("/api/plans")
public class TestPlanController {

    @Autowired
    private TestPlanService testPlanService;

    @Autowired
    private TestReportService testReportService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private com.automatedtest.platform.service.ProjectApiKeyService projectApiKeyService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private WebhookClient webhookClient;
    
    @Autowired
    private com.automatedtest.platform.service.ProjectVariableService projectVariableService;
    
    private boolean hasProjectAccess(Integer projectId, Long userId) {
        if (projectId == null || userId == null) return false;
        Project project = projectService.getById(projectId);
        if (project == null) return false;
        
        long count = teamMemberService.count(new QueryWrapper<TeamMember>()
                .eq("team_id", project.getTeamId())
                .eq("user_id", userId.intValue()));
        return count > 0;
    }

    @GetMapping
    public Result<IPage<TestPlan>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size) {
        Integer projectId = com.automatedtest.platform.common.UserContext.getCurrentProjectId();
        
        if (projectId == null) {
            return Result.error("Project ID is required");
        }
        
        Page<TestPlan> pageParam = new Page<>(page, size);
        QueryWrapper<TestPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId);
        
        queryWrapper.orderByDesc("created_at").orderByDesc("id");
        return Result.success(testPlanService.page(pageParam, queryWrapper));
    }

    @GetMapping("/{id}")
    public Result<TestPlan> getById(@PathVariable Integer id) {
        TestPlan plan = testPlanService.getById(id);
        if (plan == null) {
            return Result.error("测试计划不存在");
        }
        
        Integer contextProjectId = com.automatedtest.platform.common.UserContext.getCurrentProjectId();
        if (contextProjectId != null && !plan.getProjectId().equals(contextProjectId)) {
             return Result.error("当前项目上下文不匹配");
        }
        
        User user = com.automatedtest.platform.common.UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
             if (!hasProjectAccess(plan.getProjectId(), user.getId())) {
                 return Result.error("您没有该项目的访问权限");
             }
        }
        return Result.success(plan);
    }

    @PostMapping
    @OperationAudit(module = "TestPlan", operation = "Create Test Plan")
    public Result<Boolean> save(@RequestBody TestPlan testPlan) {
        Integer projectId = com.automatedtest.platform.common.UserContext.getCurrentProjectId();
        
        if (projectId == null) {
             return Result.error("Project ID is required");
        }
        testPlan.setProjectId(projectId);

        User user = com.automatedtest.platform.common.UserContext.getCurrentUser();
        if (user != null) {
            if (!"admin".equalsIgnoreCase(user.getRole())) {
                if (!hasProjectAccess(projectId, user.getId())) {
                    return Result.error("您没有该项目的访问权限");
                }
            }
            if (testPlan.getCreatedBy() == null) {
                testPlan.setCreatedBy(user.getId().intValue());
            }
        }
        return Result.success(testPlanService.save(testPlan));
    }

    @PutMapping
    @OperationAudit(module = "TestPlan", operation = "Update Test Plan")
    public Result<Boolean> update(@RequestBody TestPlan testPlan) {
        if (testPlan == null || testPlan.getId() == null) {
            return Result.error("参数错误");
        }
        TestPlan existing = testPlanService.getById(testPlan.getId());
        if (existing == null) {
            return Result.error("测试计划不存在");
        }
        
        User user = com.automatedtest.platform.common.UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
            if (!hasProjectAccess(existing.getProjectId(), user.getId())) {
                 return Result.error("您没有该项目的访问权限");
            }
            testPlan.setCreatedBy(existing.getCreatedBy());
        }
        testPlan.setProjectId(existing.getProjectId());
        return Result.success(testPlanService.updateById(testPlan));
    }

    @PostMapping("/{id}/execute")
    @OperationAudit(module = "TestPlan", operation = "Execute Test Plan")
    public Result<Map<String, Object>> execute(@PathVariable Integer id) {
        TestPlan plan = testPlanService.getById(id);
        if (plan == null) {
            return Result.error("测试计划不存在");
        }
        
        Integer contextProjectId = com.automatedtest.platform.common.UserContext.getCurrentProjectId();
        if (contextProjectId != null && !plan.getProjectId().equals(contextProjectId)) {
             return Result.error("当前项目上下文不匹配");
        }

        User user = com.automatedtest.platform.common.UserContext.getCurrentUser();
        String executedBy = "System";
        boolean isAdmin = false;
        Long userId = null;
        
        if (user != null) {
            executedBy = user.getUsername();
            userId = user.getId();
            isAdmin = "admin".equalsIgnoreCase(user.getRole());
            
            if (!isAdmin && !hasProjectAccess(plan.getProjectId(), userId)) {
                 return Result.error("您没有该项目的执行权限");
            }
        } else {
            // If no user, must be API Key with Project Context
            if (contextProjectId == null) {
                 return Result.error("Unauthorized execution");
            }
            executedBy = "Project API Key";
        }

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
                    if (!isAdmin && userId != null) {
                        if (c.getCreatedBy() == null || c.getCreatedBy().longValue() != userId.longValue()) {
                            return Result.error("测试计划包含非本人测试用例");
                        }
                    }
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
        
        // Update status to Running
        plan.setLastRunStatus("Running");
        plan.setLastRunTime(LocalDateTime.now());
        testPlanService.updateById(plan);

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

        double passRate = total > 0 ? (successCount * 100.0) / total : 0.0;
        double coverage = 0.0;
        long totalProjectCases = testCaseService.count(new QueryWrapper<TestCase>().eq("project_id", plan.getProjectId()));
        if (totalProjectCases > 0) {
            java.util.Set<Integer> uniq = new java.util.HashSet<>(caseIds);
            coverage = (uniq.size() * 100.0) / totalProjectCases;
        }
        double stability = 0.0;
        java.util.List<TestReport> recent = testReportService.list(new QueryWrapper<TestReport>()
                .eq("project_id", plan.getProjectId())
                .orderByDesc("executed_at")
                .last("LIMIT 50"));
        if (recent != null && !recent.isEmpty()) {
            long ok = recent.stream().filter(r -> "success".equalsIgnoreCase(String.valueOf(r.getStatus()))).count();
            stability = (ok * 100.0) / recent.size();
        }
        double healthScore = passRate * 0.6 + coverage * 0.2 + stability * 0.2;
        summary.put("passRate", passRate);
        summary.put("coverage", coverage);
        summary.put("stability", stability);
        summary.put("healthScore", healthScore);
        projectService.updateHealthScoreAsync(plan.getProjectId(), healthScore);
        com.automatedtest.platform.entity.ProjectVariable hs = projectVariableService.getOne(new QueryWrapper<com.automatedtest.platform.entity.ProjectVariable>()
                .eq("project_id", plan.getProjectId())
                .eq("key_name", "health_score"));
        if (hs == null) {
            hs = new com.automatedtest.platform.entity.ProjectVariable();
            hs.setProjectId(plan.getProjectId());
            hs.setKeyName("health_score");
            hs.setValue(String.format(java.util.Locale.ROOT, "%.2f", healthScore));
            projectVariableService.save(hs);
        } else {
            hs.setValue(String.format(java.util.Locale.ROOT, "%.2f", healthScore));
            projectVariableService.updateById(hs);
        }

        Long ownerId = null;
        if (plan.getCreatedBy() != null) {
            ownerId = Long.valueOf(plan.getCreatedBy());
        } else if (userId != null) {
            ownerId = userId;
        }

        if (ownerId != null) {
            User owner = userService.getById(ownerId);
            if (owner != null && Boolean.TRUE.equals(owner.getEnableNotification()) && owner.getNotificationWebhook() != null) {
                String rule = owner.getNotificationRule() != null ? owner.getNotificationRule().trim() : "on_fail";
                Integer threshold = owner.getNotificationThreshold() != null ? owner.getNotificationThreshold() : 80;
                boolean shouldNotify = false;
                if ("all".equalsIgnoreCase(rule)) {
                    shouldNotify = true;
                } else if ("low_pass_rate".equalsIgnoreCase(rule)) {
                    shouldNotify = passRate < threshold;
                } else {
                    shouldNotify = failedCount > 0;
                }

                if (shouldNotify) {
                    String title = "测试计划通知: " + plan.getName();
                    String content = String.format(
                        "**执行人**: %s\n\n**环境**: %s\n\n**总用例数**: %d\n\n**通过**: %d\n\n**失败**: %d\n\n**通过率**: %.2f%%\n\n**耗时**: %dms",
                        executedBy != null ? executedBy : "System",
                        plan.getEnvironment(),
                        total,
                        successCount,
                        failedCount,
                        passRate,
                        totalDuration
                    );
                    webhookClient.sendNotification(owner.getNotificationWebhook(), title, content);
                }
            }
        }

        // Update status to Success/Failed
        plan.setLastRunStatus(failedCount > 0 ? "Failed" : "Success");
        plan.setLastRunTime(LocalDateTime.now());
        testPlanService.updateById(plan);

        return Result.success(summary);
    }

    @DeleteMapping("/{id}")
    @OperationAudit(module = "TestPlan", operation = "Delete Test Plan")
    public Result<Boolean> delete(@PathVariable Integer id) {
        TestPlan plan = testPlanService.getById(id);
        if (plan == null) {
            return Result.error("测试计划不存在");
        }
        
        Integer contextProjectId = com.automatedtest.platform.common.UserContext.getCurrentProjectId();
        if (contextProjectId != null && !plan.getProjectId().equals(contextProjectId)) {
             return Result.error("当前项目上下文不匹配");
        }
        
        User user = com.automatedtest.platform.common.UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
            if (plan.getCreatedBy() == null || user.getId() == null || plan.getCreatedBy().longValue() != user.getId().longValue()) {
                return Result.error("无权限删除该测试计划");
            }
        }
        QueryWrapper<TestReport> reportWrapper = new QueryWrapper<>();
        reportWrapper.eq("plan_id", id);
        testReportService.remove(reportWrapper);
        return Result.success(testPlanService.removeById(id));
    }

    @DeleteMapping
    @OperationAudit(module = "TestPlan", operation = "Delete All Test Plans")
    public Result<Boolean> deleteAll() {
        User user = com.automatedtest.platform.common.UserContext.getCurrentUser();
        if (user == null || user.getId() == null) {
            return Result.error("未登录");
        }
        
        Integer contextProjectId = com.automatedtest.platform.common.UserContext.getCurrentProjectId();
        if (contextProjectId == null) {
             return Result.error("Project ID is required");
        }
        
        List<TestPlan> plans = testPlanService.list(new QueryWrapper<TestPlan>()
                .eq("created_by", user.getId())
                .eq("project_id", contextProjectId));
                
        if (plans != null && !plans.isEmpty()) {
            List<Integer> planIds = new ArrayList<>();
            for (TestPlan p : plans) {
                if (p != null && p.getId() != null) {
                    planIds.add(p.getId());
                }
            }
            if (!planIds.isEmpty()) {
                testReportService.remove(new QueryWrapper<TestReport>().in("plan_id", planIds));
            }
        }
        return Result.success(testPlanService.remove(new QueryWrapper<TestPlan>()
                .eq("created_by", user.getId())
                .eq("project_id", contextProjectId)));
    }
}
