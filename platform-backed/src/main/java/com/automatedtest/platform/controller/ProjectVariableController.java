package com.automatedtest.platform.controller;

import com.automatedtest.platform.annotation.OperationAudit;
import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.common.UserContext;
import com.automatedtest.platform.entity.Project;
import com.automatedtest.platform.entity.ProjectVariable;
import com.automatedtest.platform.entity.TeamMember;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.ProjectService;
import com.automatedtest.platform.service.ProjectVariableService;
import com.automatedtest.platform.service.TeamMemberService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/project/variables")
public class ProjectVariableController {

    @Autowired
    private ProjectVariableService projectVariableService;
    
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
                .eq("user_id", userId.intValue()));
        return count > 0;
    }

    @GetMapping
    public Result<IPage<ProjectVariable>> list(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size) {
        Integer projectId = UserContext.getCurrentProjectId();
        if (projectId == null) return Result.error("Project ID is required");
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
            if (!hasProjectAccess(projectId, user.getId())) {
                return Result.error("您没有该项目的访问权限");
            }
        }
        
        Page<ProjectVariable> pageParam = new Page<>(page, size);
        QueryWrapper<ProjectVariable> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId);
        queryWrapper.orderByDesc("updated_at");
        return Result.success(projectVariableService.page(pageParam, queryWrapper));
    }

    @PostMapping
    @OperationAudit(module = "ProjectVariable", operation = "Create Project Variable")
    public Result<Boolean> create(@RequestBody ProjectVariable variable) {
        Integer projectId = UserContext.getCurrentProjectId();
        if (projectId == null) return Result.error("Project ID is required");
        
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        if (!"admin".equalsIgnoreCase(user.getRole())) {
            if (!hasProjectAccess(projectId, user.getId())) {
                return Result.error("您没有该项目的访问权限");
            }
        }
        
        variable.setProjectId(projectId);
        variable.setCreatedAt(LocalDateTime.now());
        variable.setUpdatedAt(LocalDateTime.now());
        
        // check if key exists
        long count = projectVariableService.count(new QueryWrapper<ProjectVariable>()
                .eq("project_id", projectId)
                .eq("key_name", variable.getKeyName()));
        if (count > 0) return Result.error("变量名已存在");
        
        return Result.success(projectVariableService.save(variable));
    }

    @PutMapping
    @OperationAudit(module = "ProjectVariable", operation = "Update Project Variable")
    public Result<Boolean> update(@RequestBody ProjectVariable variable) {
        if (variable == null || variable.getId() == null) return Result.error("参数错误");
        
        ProjectVariable existing = projectVariableService.getById(variable.getId());
        if (existing == null) return Result.error("变量不存在");
        
        Integer projectId = UserContext.getCurrentProjectId();
        if (projectId != null && !existing.getProjectId().equals(projectId)) {
             return Result.error("当前项目上下文不匹配");
        }
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
            if (!hasProjectAccess(existing.getProjectId(), user.getId())) {
                return Result.error("您没有该项目的访问权限");
            }
        }
        
        // Ensure project_id is not changed or is consistent
        variable.setProjectId(existing.getProjectId());
        variable.setUpdatedAt(LocalDateTime.now());
        // Only update allowed fields
        existing.setValue(variable.getValue());
        existing.setDescription(variable.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        
        return Result.success(projectVariableService.updateById(existing));
    }

    @DeleteMapping("/{id}")
    @OperationAudit(module = "ProjectVariable", operation = "Delete Project Variable")
    public Result<Boolean> delete(@PathVariable Integer id) {
        ProjectVariable existing = projectVariableService.getById(id);
        if (existing == null) return Result.success(true);
        
        Integer projectId = UserContext.getCurrentProjectId();
        if (projectId != null && !existing.getProjectId().equals(projectId)) {
             return Result.error("当前项目上下文不匹配");
        }
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
            if (!hasProjectAccess(existing.getProjectId(), user.getId())) {
                return Result.error("您没有该项目的访问权限");
            }
        }
        
        return Result.success(projectVariableService.removeById(id));
    }
}
