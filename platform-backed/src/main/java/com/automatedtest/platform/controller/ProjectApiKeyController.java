package com.automatedtest.platform.controller;

import com.automatedtest.platform.annotation.OperationAudit;
import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.common.UserContext;
import com.automatedtest.platform.entity.Project;
import com.automatedtest.platform.entity.ProjectApiKey;
import com.automatedtest.platform.entity.TeamMember;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.ProjectApiKeyService;
import com.automatedtest.platform.service.ProjectService;
import com.automatedtest.platform.service.TeamMemberService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/project/keys")
public class ProjectApiKeyController {

    @Autowired
    private ProjectApiKeyService projectApiKeyService;

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
    public Result<IPage<ProjectApiKey>> list(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size) {
        Integer projectId = UserContext.getCurrentProjectId();
        if (projectId == null) return Result.error("Project ID is required");
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
             if (!hasProjectAccess(projectId, user.getId())) {
                 return Result.error("您没有该项目的访问权限");
             }
        }
        
        Page<ProjectApiKey> pageParam = new Page<>(page, size);
        QueryWrapper<ProjectApiKey> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId);
        queryWrapper.orderByDesc("created_at");
        return Result.success(projectApiKeyService.page(pageParam, queryWrapper));
    }

    @PostMapping
    @OperationAudit(module = "ProjectApiKey", operation = "Create Project API Key")
    public Result<ProjectApiKey> create(@RequestBody ProjectApiKey key) {
        Integer projectId = UserContext.getCurrentProjectId();
        if (projectId == null) return Result.error("Project ID is required");

        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        if (!"admin".equalsIgnoreCase(user.getRole())) {
             if (!hasProjectAccess(projectId, user.getId())) {
                 return Result.error("您没有该项目的访问权限");
             }
        }
        
        key.setProjectId(projectId);
        key.setUserId(user.getId().intValue());
        key.setApiKey(projectApiKeyService.generateKey());
        key.setCreatedAt(LocalDateTime.now());
        
        projectApiKeyService.save(key);
        return Result.success(key);
    }

    @DeleteMapping("/{id}")
    @OperationAudit(module = "ProjectApiKey", operation = "Delete Project API Key")
    public Result<Boolean> delete(@PathVariable Integer id) {
        Integer projectId = UserContext.getCurrentProjectId();
        if (projectId == null) return Result.error("Project ID is required");
        
        ProjectApiKey existing = projectApiKeyService.getById(id);
        if (existing == null) return Result.success(true);
        
        if (!existing.getProjectId().equals(projectId)) return Result.error("无权删除其他项目的Key");
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
             if (!hasProjectAccess(projectId, user.getId())) {
                 return Result.error("您没有该项目的访问权限");
             }
        }
        
        return Result.success(projectApiKeyService.removeById(id));
    }
}
