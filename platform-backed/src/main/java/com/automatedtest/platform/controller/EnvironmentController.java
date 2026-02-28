package com.automatedtest.platform.controller;

import com.automatedtest.platform.annotation.OperationAudit;
import com.automatedtest.platform.common.UserContext;
import com.automatedtest.platform.entity.Environment;
import com.automatedtest.platform.entity.Project;
import com.automatedtest.platform.entity.TeamMember;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.EnvironmentService;
import com.automatedtest.platform.service.ProjectService;
import com.automatedtest.platform.service.TeamMemberService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/environments")
public class EnvironmentController {

    @Autowired
    private EnvironmentService environmentService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private TeamMemberService teamMemberService;
    
    private boolean hasProjectAccess(Integer projectId, Long userId) {
        if (projectId == null || userId == null) return false;
        Project project = projectService.getById(projectId);
        if (project == null) return false;
        
        long count = teamMemberService.count(new QueryWrapper<TeamMember>()
                .eq("team_id", project.getTeamId())
                .eq("user_id", userId));
        return count > 0;
    }

    @GetMapping
    public Result<List<Environment>> list() {
        Integer projectId = UserContext.getCurrentProjectId();
        
        if (projectId == null) {
             return Result.error("Project ID is required");
        }
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
            // Access check handled by interceptor if projectId is in context
            // But double check for safety
            if (!hasProjectAccess(projectId, user.getId())) {
                return Result.error("您没有该项目的访问权限");
            }
        }
        
        return Result.success(environmentService.lambdaQuery().eq(Environment::getProjectId, projectId).list());
    }

    @PostMapping
    @OperationAudit(module = "Environment", operation = "Create Environment")
    public Result<Boolean> save(@RequestBody Environment environment) {
        Integer projectId = UserContext.getCurrentProjectId();
        
        if (projectId == null) {
             return Result.error("Project ID is required");
        }
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
            if (!hasProjectAccess(projectId, user.getId())) {
                return Result.error("您没有该项目的访问权限");
            }
        }
        
        environment.setProjectId(projectId);
        return Result.success(environmentService.save(environment));
    }

    @PutMapping
    @OperationAudit(module = "Environment", operation = "Update Environment")
    public Result<Boolean> update(@RequestBody Environment environment) {
        if (environment == null || environment.getId() == null) {
             return Result.error("参数错误");
        }
        Environment existing = environmentService.getById(environment.getId());
        if (existing == null) {
             return Result.error("环境配置不存在");
        }
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
            if (!hasProjectAccess(existing.getProjectId(), user.getId())) {
                return Result.error("您没有该项目的访问权限");
            }
        }
        environment.setProjectId(existing.getProjectId());
        return Result.success(environmentService.updateById(environment));
    }

    @DeleteMapping("/{id}")
    @OperationAudit(module = "Environment", operation = "Delete Environment")
    public Result<Boolean> delete(@PathVariable Integer id) {
        Environment existing = environmentService.getById(id);
        if (existing == null) {
             return Result.error("环境配置不存在");
        }
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
            if (!hasProjectAccess(existing.getProjectId(), user.getId())) {
                return Result.error("您没有该项目的访问权限");
            }
        }
        return Result.success(environmentService.removeById(id));
    }
}
