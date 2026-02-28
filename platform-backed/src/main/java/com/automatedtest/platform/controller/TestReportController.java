package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.common.UserContext;
import com.automatedtest.platform.dto.ReportDetailDTO;
import com.automatedtest.platform.entity.Project;
import com.automatedtest.platform.entity.TeamMember;
import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.entity.TestPlan;
import com.automatedtest.platform.entity.TestReport;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.ProjectService;
import com.automatedtest.platform.service.TeamMemberService;
import com.automatedtest.platform.service.TestCaseService;
import com.automatedtest.platform.service.TestPlanService;
import com.automatedtest.platform.service.TestReportService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private TeamMemberService teamMemberService;
    
    private boolean hasProjectAccess(Integer projectId, Long userId) {
        if (projectId == null || userId == null) return false;
        // Optimization: If UserContext already has the project ID and it matches, we assume access is valid 
        // because the interceptor would have checked it (if we trust the interceptor flow).
        // However, for cross-project access checks, we still need this.
        
        Project project = projectService.getById(projectId);
        if (project == null) return false;
        
        long count = teamMemberService.count(new QueryWrapper<TeamMember>()
                .eq("team_id", project.getTeamId())
                .eq("user_id", userId));
        return count > 0;
    }

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
        
        User user = UserContext.getCurrentUser();
        Integer projectId = UserContext.getCurrentProjectId();

        boolean isAdmin = user != null && "admin".equalsIgnoreCase(user.getRole());

        if (projectId != null) {
            queryWrapper.eq("project_id", projectId);
        } else {
            // No project ID provided. Only admin can see all.
            if (!isAdmin) {
                 return Result.error("Project ID is required");
            }
        }

        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            queryWrapper.eq("status", status);
        }

        if (planId != null && planId > 0) {
            queryWrapper.eq("plan_id", planId);
        }

        if (planRunNo != null && planRunNo > 0) {
            queryWrapper.eq("plan_run_no", planRunNo);
        }
        
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
        
        User user = UserContext.getCurrentUser();
        Integer contextProjectId = UserContext.getCurrentProjectId();
        
        if (contextProjectId != null) {
            if (report.getProjectId() != null && !report.getProjectId().equals(contextProjectId)) {
                 return Result.error("当前项目上下文不匹配");
            }
        }
        
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
             if (report.getProjectId() != null) {
                 if (!hasProjectAccess(report.getProjectId(), user.getId())) {
                      return Result.error("您没有该项目的访问权限");
                 }
             } else {
                 // Fallback for old reports without projectId? Or allow if executedBy matches?
                 String executedBy = report.getExecutedBy() != null ? report.getExecutedBy().trim() : null;
                 if (executedBy == null || !executedBy.equals(user.getUsername())) {
                     return Result.error("无权限查看该测试报告");
                 }
             }
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
    
    @PostMapping
    public Result<Boolean> save(@RequestBody TestReport testReport) {
        User user = UserContext.getCurrentUser();
        Integer projectId = UserContext.getCurrentProjectId();
        
        if (projectId != null) {
            testReport.setProjectId(projectId);
        }
        
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
            testReport.setExecutedBy(user.getUsername());
        }
        return Result.success(testReportService.save(testReport));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        TestReport report = testReportService.getById(id);
        if (report == null) {
            return Result.error("测试报告不存在");
        }
        
        User user = UserContext.getCurrentUser();
        Integer contextProjectId = UserContext.getCurrentProjectId();

        if (contextProjectId != null) {
            if (report.getProjectId() != null && !report.getProjectId().equals(contextProjectId)) {
                 return Result.error("当前项目上下文不匹配");
            }
        }

        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
            String executedBy = report.getExecutedBy() != null ? report.getExecutedBy().trim() : null;
            if (executedBy == null || !executedBy.equals(user.getUsername())) {
                return Result.error("无权限删除该测试报告");
            }
        }
        return Result.success(testReportService.removeById(id));
    }

    @DeleteMapping
    public Result<Boolean> deleteBatch(@RequestBody(required = false) List<Integer> ids) {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            return Result.error("未登录");
        }
        
        Integer contextProjectId = UserContext.getCurrentProjectId();
        boolean restrictToUser = !"admin".equalsIgnoreCase(user.getRole());

        QueryWrapper<TestReport> queryWrapper = new QueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            queryWrapper.in("id", ids);
        }
        
        if (contextProjectId != null) {
            queryWrapper.eq("project_id", contextProjectId);
        }
        
        if (restrictToUser) {
             queryWrapper.eq("executed_by", user.getUsername());
        }
        
        if (ids == null || ids.isEmpty()) {
             // Delete all matching criteria
             if (contextProjectId == null && !restrictToUser) {
                 // Prevent accidental "delete all" by admin without filters
                 return Result.error("批量删除需指定条件"); 
             }
             return Result.success(testReportService.remove(queryWrapper));
        }

        // Verify count matches
        long count = testReportService.count(queryWrapper);
        if (count != ids.size()) {
             return Result.error("部分报告不存在或无权限删除");
        }
        
        return Result.success(testReportService.removeBatchByIds(ids));
    }
}
