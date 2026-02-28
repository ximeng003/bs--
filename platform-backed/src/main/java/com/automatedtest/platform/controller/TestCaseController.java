package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.UserContext;
import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.dto.ApiTestRequestDTO;
import com.automatedtest.platform.dto.ApiTestResponseDTO;
import com.automatedtest.platform.dto.CaseExecuteResultDTO;
import com.automatedtest.platform.entity.Project;
import com.automatedtest.platform.entity.TeamMember;
import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.ProjectService;
import com.automatedtest.platform.service.TeamMemberService;
import com.automatedtest.platform.service.TestCaseService;
import com.automatedtest.platform.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/testcases")
public class TestCaseController {

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private com.automatedtest.platform.service.UserApiKeyService userApiKeyService;

    private boolean hasProjectAccess(Integer projectId, Long userId) {
        if (projectId == null || userId == null) return false;
        Project project = projectService.getById(projectId);
        if (project == null) return false;
        
        long count = teamMemberService.count(new QueryWrapper<TeamMember>()
                .eq("team_id", project.getTeamId())
                .eq("user_id", userId));
        return count > 0;
    }

    @PostMapping("/execute")
    @OperationAudit(module = "TestCase", operation = "Execute API Test")
    public Result<ApiTestResponseDTO> execute(@RequestBody ApiTestRequestDTO request) {
        User user = UserContext.getCurrentUser();
        Long userId = user != null ? user.getId() : null;
        Integer projectId = UserContext.getCurrentProjectId();

        return Result.success(testCaseService.executeApiTest(request, userId, projectId));
    }
    
    @PostMapping("/{id}/execute")
    @OperationAudit(module = "TestCase", operation = "Execute Test Case by ID")
    public Result<CaseExecuteResultDTO> executeById(@PathVariable Integer id) {
        TestCase testCase = testCaseService.getById(id);
        if (testCase == null) {
            return Result.error("测试用例不存在");
        }
        
        Integer contextProjectId = UserContext.getCurrentProjectId();
        if (contextProjectId != null && !testCase.getProjectId().equals(contextProjectId)) {
             return Result.error("当前项目上下文不匹配");
        }
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
             if (!hasProjectAccess(testCase.getProjectId(), user.getId())) {
                 return Result.error("您没有该项目的执行权限");
             }
        } else if (user == null && contextProjectId == null) {
             // If no user and no project context (API Key), we might want to block
             // But if API Key provided contextProjectId, it's checked above
             return Result.error("Unauthorized execution");
        }

        String executedBy = user != null ? user.getUsername() : "Project API Key";
        return Result.success(testCaseService.executeCaseById(id, executedBy));
    }

    @GetMapping
    public Result<IPage<TestCase>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(required = false, defaultValue = "updated") String sort) {
        Integer projectId = UserContext.getCurrentProjectId();
        
        if (projectId == null) {
            return Result.error("Project ID is required (X-Project-Id header)");
        }

        // Access is already verified by Interceptor if projectId is set via header
        
        Page<TestCase> pageParam = new Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TestCase> qw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        
        qw.eq("project_id", projectId);

        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.like("name", keyword.trim());
        }
        if (type != null && !type.trim().isEmpty()) {
            qw.eq("type", type.trim());
        }
        if ("created".equalsIgnoreCase(sort)) {
            qw.orderByDesc("created_at");
        } else {
            qw.orderByDesc("updated_at");
        }
        return Result.success(testCaseService.page(pageParam, qw));
    }

    @GetMapping("/{id}")
    public Result<TestCase> getById(@PathVariable Integer id) {
        TestCase testCase = testCaseService.getById(id);
        if (testCase == null) {
            return Result.error("测试用例不存在");
        }
        
        Integer contextProjectId = UserContext.getCurrentProjectId();
        if (contextProjectId != null && !testCase.getProjectId().equals(contextProjectId)) {
             return Result.error("当前项目上下文不匹配");
        }
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
             if (!hasProjectAccess(testCase.getProjectId(), user.getId())) {
                 return Result.error("您没有该项目的访问权限");
             }
        }
        return Result.success(testCase);
    }

    @PostMapping
    @OperationAudit(module = "TestCase", operation = "Create Test Case")
    public Result<Integer> save(@RequestBody TestCase testCase) {
        Integer projectId = UserContext.getCurrentProjectId();
        
        if (projectId == null) {
             return Result.error("Project ID is required (X-Project-Id header)");
        }
        testCase.setProjectId(projectId);

        User user = UserContext.getCurrentUser();
        if (user != null) {
            // Access check handled by Interceptor for the projectId in context
            // But we double check just in case logic changes
            if (!"admin".equalsIgnoreCase(user.getRole())) {
                 // Interceptor checks if user is in team. 
            }
            if (testCase.getCreatedBy() == null) {
                testCase.setCreatedBy(user.getId().intValue());
            }
        }
        
        boolean success = testCaseService.save(testCase);
        if (success) {
            return Result.success(testCase.getId());
        }
        return Result.error("Save failed");
    }

    @PutMapping
    @OperationAudit(module = "TestCase", operation = "Update Test Case")
    public Result<Boolean> update(@RequestBody TestCase testCase) {
        if (testCase == null || testCase.getId() == null) {
            return Result.error("参数错误");
        }
        TestCase existing = testCaseService.getById(testCase.getId());
        if (existing == null) {
            return Result.error("测试用例不存在");
        }
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
             if (!hasProjectAccess(existing.getProjectId(), user.getId())) {
                 return Result.error("您没有该项目的访问权限");
             }
        }
        
        // Ensure project_id is not changed
        testCase.setProjectId(existing.getProjectId()); 
        testCase.setCreatedBy(existing.getCreatedBy());
        
        return Result.success(testCaseService.updateById(testCase));
    }

    @DeleteMapping("/{id}")
    @OperationAudit(module = "TestCase", operation = "Delete Test Case")
    public Result<Boolean> delete(@PathVariable Integer id) {
        TestCase existing = testCaseService.getById(id);
        if (existing == null) {
            return Result.error("测试用例不存在");
        }
        
        Integer contextProjectId = UserContext.getCurrentProjectId();
        if (contextProjectId != null && !existing.getProjectId().equals(contextProjectId)) {
             return Result.error("当前项目上下文不匹配");
        }
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
             if (!hasProjectAccess(existing.getProjectId(), user.getId())) {
                 return Result.error("您没有该项目的访问权限");
             }
        }
        return Result.success(testCaseService.removeById(id));
    }

    @DeleteMapping
    @OperationAudit(module = "TestCase", operation = "Delete All Test Cases")
    public Result<Boolean> deleteAll() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TestCase> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        
        User user = UserContext.getCurrentUser();
        if (user == null || user.getId() == null) {
             return Result.error("未登录");
        }
        
        Integer contextProjectId = UserContext.getCurrentProjectId();
        if (contextProjectId == null) {
             return Result.error("Project ID is required");
        }
        
        qw.eq("created_by", user.getId());
        qw.eq("project_id", contextProjectId);
        
        return Result.success(testCaseService.remove(qw));
    }
        qw.eq("created_by", user.getId());
        return Result.success(testCaseService.remove(qw));
    }
}
