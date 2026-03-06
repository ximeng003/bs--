package com.automatedtest.platform.controller;

import com.automatedtest.platform.annotation.OperationAudit;
import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.common.UserContext;
import com.automatedtest.platform.entity.Project;
import com.automatedtest.platform.entity.TeamMember;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.ProjectService;
import com.automatedtest.platform.service.TeamMemberService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TeamMemberService teamMemberService;

    @GetMapping
    public Result<List<Project>> list() {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            return Result.error("未登录");
        }

        if ("admin".equalsIgnoreCase(user.getRole())) {
            List<Project> projects = projectService.lambdaQuery().eq(Project::getIsDeleted, false).list();
            projects.forEach(p -> p.setRole("admin"));
            return Result.success(projects);
        }

        // Find teams the user belongs to
        List<TeamMember> memberships = teamMemberService.lambdaQuery().eq(TeamMember::getUserId, user.getId()).list();
        if (memberships.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<Integer> teamIds = memberships.stream().map(TeamMember::getTeamId).collect(Collectors.toList());
        
        // Find projects belonging to these teams
        List<Project> projects = projectService.lambdaQuery().in(Project::getTeamId, teamIds).eq(Project::getIsDeleted, false).list();
        
        // Populate role from team membership
        for (Project project : projects) {
            memberships.stream()
                .filter(m -> m.getTeamId().equals(project.getTeamId()))
                .findFirst()
                .ifPresent(m -> project.setRole(m.getRole()));
        }
        
        return Result.success(projects);
    }

    @GetMapping("/archived")
    public Result<List<Project>> listArchived() {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            return Result.error("未登录");
        }
        if ("admin".equalsIgnoreCase(user.getRole())) {
            List<Project> projects = projectService.lambdaQuery().eq(Project::getIsDeleted, true).list();
            projects.forEach(p -> p.setRole("admin"));
            return Result.success(projects);
        }
        List<TeamMember> memberships = teamMemberService.lambdaQuery().eq(TeamMember::getUserId, user.getId()).list();
        if (memberships.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<Integer> teamIds = memberships.stream().map(TeamMember::getTeamId).collect(Collectors.toList());
        List<Project> projects = projectService.lambdaQuery()
                .in(Project::getTeamId, teamIds)
                .eq(Project::getIsDeleted, true)
                .list();
        for (Project project : projects) {
            memberships.stream()
                    .filter(m -> m.getTeamId().equals(project.getTeamId()))
                    .findFirst()
                    .ifPresent(m -> project.setRole(m.getRole()));
        }
        return Result.success(projects);
    }

    @PostMapping
    @OperationAudit(module = "Project", operation = "Create Project")
    public Result<Boolean> create(@RequestBody Project project) {
        if (project.getTeamId() == null) {
            return Result.error("项目必须归属于一个团队");
        }

        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        // Quota Check
        if (!"admin".equalsIgnoreCase(user.getRole())) {
            long projectCount = projectService.count(new QueryWrapper<Project>().eq("created_by", user.getId()));
            if (projectCount >= user.getMaxProjects()) {
                return Result.error("已达到项目创建数量上限(" + user.getMaxProjects() + ")");
            }
        }

        // Check if user is a member of the team
        TeamMember member = teamMemberService.lambdaQuery()
                .eq(TeamMember::getTeamId, project.getTeamId())
                .eq(TeamMember::getUserId, user.getId())
                .one();
        
        boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());
        
        if (!isAdmin && member == null) {
            return Result.error("您不是该团队成员，无法创建项目");
        }
        
        if (!isAdmin && !"admin".equalsIgnoreCase(member.getRole())) {
             return Result.error("只有团队管理员或系统管理员可以创建项目");
        }

        project.setCreatedBy(user.getId().intValue());
        return Result.success(projectService.save(project));
    }
    
    @GetMapping("/{id}")
    public Result<Project> getById(@PathVariable Integer id) {
        Project project = projectService.getById(id);
        if (project == null) return Result.error("项目不存在");
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
             long count = teamMemberService.count(new QueryWrapper<TeamMember>()
                    .eq("team_id", project.getTeamId())
                    .eq("user_id", user.getId().intValue()));
             if (count == 0) return Result.error("无权访问该项目");
        }
        return Result.success(project);
    }

    @PutMapping
    @OperationAudit(module = "Project", operation = "Update Project")
    public Result<Boolean> update(@RequestBody Project project) {
        if (project == null || project.getId() == null) return Result.error("参数错误");
        
        Project existing = projectService.getById(project.getId());
        if (existing == null) return Result.error("项目不存在");
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
             TeamMember member = teamMemberService.lambdaQuery()
                    .eq(TeamMember::getTeamId, existing.getTeamId())
                    .eq(TeamMember::getUserId, user.getId())
                    .one();
             if (member == null || !"admin".equalsIgnoreCase(member.getRole())) {
                 return Result.error("只有团队管理员可以修改项目");
             }
        }
        // Don't allow changing team_id easily as it breaks security boundaries?
        // For now, assume it's allowed but require permission in new team too?
        // Let's restrict team_id change for simplicity or keep existing team_id
        project.setTeamId(existing.getTeamId());
        
        return Result.success(projectService.updateById(project));
    }

    @DeleteMapping("/{id}")
    @OperationAudit(module = "Project", operation = "Delete Project")
    public Result<Boolean> delete(@PathVariable Integer id) {
        Project existing = projectService.getById(id);
        if (existing == null) return Result.error("项目不存在");
        
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
             TeamMember member = teamMemberService.lambdaQuery()
                    .eq(TeamMember::getTeamId, existing.getTeamId())
                    .eq(TeamMember::getUserId, user.getId())
                    .one();
             if (member == null || !"admin".equalsIgnoreCase(member.getRole())) {
                 return Result.error("只有团队管理员可以删除项目");
             }
        }
        existing.setStatus("archived");
        existing.setIsDeleted(true);
        return Result.success(projectService.updateById(existing));
    }

    @PutMapping("/{id}/restore")
    @OperationAudit(module = "Project", operation = "Restore Project")
    public Result<Boolean> restore(@PathVariable Integer id) {
        Project existing = projectService.getById(id);
        if (existing == null) return Result.error("项目不存在");
        User user = UserContext.getCurrentUser();
        if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
            TeamMember member = teamMemberService.lambdaQuery()
                    .eq(TeamMember::getTeamId, existing.getTeamId())
                    .eq(TeamMember::getUserId, user.getId())
                    .one();
            if (member == null || !"admin".equalsIgnoreCase(member.getRole())) {
                return Result.error("只有团队管理员可以恢复项目");
            }
        }
        existing.setStatus("active");
        existing.setIsDeleted(false);
        return Result.success(projectService.updateById(existing));
    }
}
