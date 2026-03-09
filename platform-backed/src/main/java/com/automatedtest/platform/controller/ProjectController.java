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

import com.automatedtest.platform.entity.PermissionRequest;
import com.automatedtest.platform.service.PermissionRequestService;
import com.automatedtest.platform.service.TeamService;
import com.automatedtest.platform.entity.Team;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TeamMemberService teamMemberService;
    
    @Autowired
    private PermissionRequestService permissionRequestService;
    
    @Autowired
    private TeamService teamService;
    
    @Autowired
    private com.automatedtest.platform.service.TestCaseService testCaseService;
    
    @Autowired
    private com.automatedtest.platform.service.TestPlanService testPlanService;
    
    @Autowired
    private com.automatedtest.platform.service.TestReportService testReportService;
    
    @Autowired
    private com.automatedtest.platform.service.ProjectVariableService projectVariableService;
    
    @Autowired
    private com.automatedtest.platform.service.EnvironmentService environmentService;
    
    @Autowired
    private com.automatedtest.platform.service.ProjectApiKeyService projectApiKeyService;

    @GetMapping
    public Result<List<Project>> list() {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            return Result.error(401, "未登录");
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

    @GetMapping("/search-available")
    public Result<List<Map<String, Object>>> searchAvailable(@RequestParam(required = false) String keyword) {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            return Result.error(401, "未登录");
        }
        Integer userId = user.getId().intValue();

        // 1. Get projects user is already a member of (via TeamMember)
        // Find teams the user belongs to
        List<TeamMember> memberships = teamMemberService.lambdaQuery()
                .eq(TeamMember::getUserId, userId)
                .select(TeamMember::getTeamId)
                .list();
        List<Integer> memberTeamIds = memberships.stream()
                .map(TeamMember::getTeamId)
                .collect(Collectors.toList());
        
        // 2. Get projects user has pending requests for
        List<PermissionRequest> pendingRequests = permissionRequestService.lambdaQuery()
                .eq(PermissionRequest::getUserId, userId)
                .eq(PermissionRequest::getStatus, "pending")
                .eq(PermissionRequest::getRequestType, "JOIN_PROJECT")
                .select(PermissionRequest::getProjectId)
                .list();
        List<Integer> pendingProjectIds = pendingRequests.stream()
                .map(PermissionRequest::getProjectId)
                .collect(Collectors.toList());

        // 3. Query Projects
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);
        // Only public projects
        queryWrapper.eq("is_public", true); 
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like("name", keyword.trim());
        }
        
        // Exclude projects where user is already in the team
        if (!memberTeamIds.isEmpty()) {
            queryWrapper.notIn("team_id", memberTeamIds);
        }
        
        // Exclude pending projects
        if (!pendingProjectIds.isEmpty()) {
            queryWrapper.notIn("id", pendingProjectIds);
        }
        
        queryWrapper.last("LIMIT 20"); // Limit results for performance
        
        List<Project> projects = projectService.list(queryWrapper);
        
        // 4. Enrich with Team Name
        if (projects.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        List<Integer> teamIds = projects.stream().map(Project::getTeamId).distinct().collect(Collectors.toList());
        Map<Integer, String> teamNames = teamService.lambdaQuery()
                .in(Team::getId, teamIds)
                .select(Team::getId, Team::getName)
                .list()
                .stream()
                .collect(Collectors.toMap(Team::getId, Team::getName));
                
        List<Map<String, Object>> result = projects.stream().map(p -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("description", p.getDescription());
            map.put("teamId", p.getTeamId());
            map.put("teamName", teamNames.getOrDefault(p.getTeamId(), "Unknown Team"));
            return map;
        }).collect(Collectors.toList());
        
        return Result.success(result);
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

    @PutMapping("/{id}/restore")
    @OperationAudit(module = "Project", operation = "Restore Project")
    public Result<Boolean> restore(@PathVariable Integer id) {
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        Project project = projectService.getById(id);
        if (project == null) return Result.error("项目不存在");
        
        // Only system admin or team admin can restore
        boolean isSystemAdmin = "admin".equalsIgnoreCase(user.getRole());
        boolean isTeamAdmin = false;
        if (!isSystemAdmin) {
            TeamMember member = teamMemberService.lambdaQuery()
                    .eq(TeamMember::getTeamId, project.getTeamId())
                    .eq(TeamMember::getUserId, user.getId())
                    .one();
            isTeamAdmin = member != null && "admin".equalsIgnoreCase(member.getRole());
        }
        
        if (!isSystemAdmin && !isTeamAdmin) {
            return Result.error("无权限");
        }
        
        project.setIsDeleted(false);
        return Result.success(projectService.updateById(project));
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
    @OperationAudit(module = "Project", operation = "Archive Project")
    public Result<Boolean> delete(@PathVariable Integer id) {
        Project existing = projectService.getById(id);
        if (existing == null) return Result.error("项目不存在");
        
        User user = UserContext.getCurrentUser();
        // Allow System Admin (global admin)
        boolean isSystemAdmin = user != null && "admin".equalsIgnoreCase(user.getRole());
        
        if (!isSystemAdmin) {
             // Check if user is Team Admin for the project's team
             if (user == null) return Result.error("未登录");
             
             TeamMember member = teamMemberService.lambdaQuery()
                    .eq(TeamMember::getTeamId, existing.getTeamId())
                    .eq(TeamMember::getUserId, user.getId())
                    .one();
             
             if (member == null || !"admin".equalsIgnoreCase(member.getRole())) {
                 return Result.error("只有系统管理员或团队管理员可以归档项目");
             }
        }
        
        existing.setIsDeleted(true);
        // Ensure status is updated if needed, though isDeleted is the main flag
        return Result.success(projectService.updateById(existing));
    }
    
    @DeleteMapping("/{id}/purge")
    @OperationAudit(module = "Project", operation = "Purge Project")
    public Result<Boolean> purge(@PathVariable Integer id) {
        Project existing = projectService.getById(id);
        if (existing == null) return Result.error("项目不存在");
        
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        boolean isSystemAdmin = "admin".equalsIgnoreCase(user.getRole());
        if (!isSystemAdmin) {
            return Result.error("只有系统管理员可以彻底删除项目");
        }
        
        // Physically delete associated resources
        testCaseService.remove(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.automatedtest.platform.entity.TestCase>().eq("project_id", id));
        testPlanService.remove(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.automatedtest.platform.entity.TestPlan>().eq("project_id", id));
        testReportService.remove(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.automatedtest.platform.entity.TestReport>().eq("project_id", id));
        projectVariableService.remove(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.automatedtest.platform.entity.ProjectVariable>().eq("project_id", id));
        environmentService.remove(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.automatedtest.platform.entity.Environment>().eq("project_id", id));
        projectApiKeyService.remove(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.automatedtest.platform.entity.ProjectApiKey>().eq("project_id", id));
        
        // Finally, delete project itself
        boolean ok = projectService.removeById(id);
        return Result.success(ok);
    }
}
