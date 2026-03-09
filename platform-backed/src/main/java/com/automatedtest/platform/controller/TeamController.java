package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.common.UserContext;
import com.automatedtest.platform.entity.Team;
import com.automatedtest.platform.entity.TeamMember;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.TeamMemberService;
import com.automatedtest.platform.service.TeamService;
import com.automatedtest.platform.service.UserService;
import com.automatedtest.platform.service.ProjectService;
import com.automatedtest.platform.entity.Project;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public Result<List<Team>> list() {
        User user = UserContext.getCurrentUser();
        if (user == null) {
            return Result.error("未登录");
        }

        boolean isSystemAdmin = "admin".equalsIgnoreCase(user.getRole());
        List<Team> teams;
        if (isSystemAdmin) {
            teams = teamService.lambdaQuery().eq(Team::getIsDeleted, false).list();
            // For system admin, show membership role if exists; otherwise leave empty
            List<TeamMember> myMemberships = teamMemberService.list(new QueryWrapper<TeamMember>().eq("user_id", user.getId().intValue()));
            if (!myMemberships.isEmpty()) {
                java.util.Map<Integer, String> roleMap = myMemberships.stream()
                        .collect(java.util.stream.Collectors.toMap(TeamMember::getTeamId, TeamMember::getRole, (a, b) -> a));
                for (Team t : teams) {
                    String r = roleMap.get(t.getId());
                    if (r != null) t.setRole(r);
                }
            }
            return Result.success(teams);
        } else {
            List<TeamMember> memberships = teamMemberService.list(new QueryWrapper<TeamMember>().eq("user_id", user.getId().intValue()));
            if (memberships.isEmpty()) {
                return Result.success(Collections.emptyList());
            }
            List<Integer> teamIds = memberships.stream().map(TeamMember::getTeamId).collect(Collectors.toList());
            teams = teamService.lambdaQuery().in(Team::getId, teamIds).eq(Team::getIsDeleted, false).list();
            if (!teams.isEmpty()) {
                java.util.Map<Integer, String> roleMap = memberships.stream()
                        .collect(java.util.stream.Collectors.toMap(TeamMember::getTeamId, TeamMember::getRole, (a, b) -> a));
                for (Team t : teams) {
                    String r = roleMap.get(t.getId());
                    if (r != null) t.setRole(r);
                }
            }
            return Result.success(teams);
        }
    }

    @PostMapping
    public Result<Team> create(@RequestBody Team team) {
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");

        // Quota Check
        if (!"admin".equalsIgnoreCase(user.getRole())) {
            long teamCount = teamService.count(new QueryWrapper<Team>().eq("created_by", user.getId()));
            if (teamCount >= user.getMaxTeams()) {
                return Result.error("已达到团队创建数量上限(" + user.getMaxTeams() + ")");
            }
        }
        
        team.setCreatedBy(user.getId().intValue());
        teamService.save(team);
        
        // Add creator as admin member
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(user.getId().intValue());
        member.setRole("admin");
        teamMemberService.save(member);
        
        return Result.success(team);
    }
    
    @PostMapping("/{id}/members")
    public Result<Boolean> addMember(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        TeamMember currentMember = teamMemberService.getOne(new QueryWrapper<TeamMember>()
            .eq("team_id", id)
            .eq("user_id", user.getId().intValue()));
            
        boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());
        if (!isAdmin && (currentMember == null || !"admin".equalsIgnoreCase(currentMember.getRole()))) {
            return Result.error("无权限添加成员");
        }
        
        String targetUsername = body.get("username");
        String role = body.getOrDefault("role", "member");
        
        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        
        User targetUser = userService.lambdaQuery().eq(User::getUsername, targetUsername.trim()).one();
        if (targetUser == null) {
            return Result.error("用户不存在: " + targetUsername);
        }
        
        // Check if already member
        Long count = teamMemberService.count(new QueryWrapper<TeamMember>()
            .eq("team_id", id)
            .eq("user_id", targetUser.getId()));
            
        if (count > 0) {
            return Result.error("用户已经是团队成员");
        }
        
        TeamMember member = new TeamMember();
        member.setTeamId(id);
        member.setUserId(targetUser.getId().intValue());
        member.setRole(role);
        return Result.success(teamMemberService.save(member));
    }

    @GetMapping("/{id}/members")
    public Result<List<TeamMember>> listMembers(@PathVariable Integer id) {
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        TeamMember currentMember = teamMemberService.getOne(new QueryWrapper<TeamMember>()
            .eq("team_id", id)
            .eq("user_id", user.getId().intValue()));
            
        boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());
        if (!isAdmin && currentMember == null) {
            return Result.error("无权限查看成员");
        }
        
        List<TeamMember> members = teamMemberService.list(new QueryWrapper<TeamMember>().eq("team_id", id));
        
        if (!members.isEmpty()) {
            List<Long> userIds = members.stream().map(m -> m.getUserId().longValue()).collect(Collectors.toList());
            List<User> users = userService.listByIds(userIds);
            Map<Integer, User> userMap = users.stream().collect(Collectors.toMap(u -> u.getId().intValue(), u -> u));
            
            for (TeamMember m : members) {
                User u = userMap.get(m.getUserId());
                if (u != null) {
                    m.setUsername(u.getUsername());
                    m.setNickname(u.getNickname());
                    m.setAvatar(u.getAvatar());
                }
            }
        }
        
        return Result.success(members);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public Result<Boolean> removeMember(@PathVariable Integer id, @PathVariable Integer memberId) {
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        TeamMember currentMember = teamMemberService.getOne(new QueryWrapper<TeamMember>()
            .eq("team_id", id)
            .eq("user_id", user.getId().intValue()));
            
        boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());
        if (!isAdmin && (currentMember == null || !"admin".equalsIgnoreCase(currentMember.getRole()))) {
            return Result.error("无权限移除成员");
        }
        
        TeamMember targetMember = teamMemberService.getById(memberId);
        if (targetMember == null) {
             return Result.error("成员不存在");
        }
        if (!targetMember.getTeamId().equals(id)) {
             return Result.error("成员不属于该团队");
        }
        
        if (targetMember.getUserId().equals(user.getId().intValue())) {
            return Result.error("不能移除自己");
        }

        return Result.success(teamMemberService.removeById(memberId));
    }

    @DeleteMapping("/{id}")
    @com.automatedtest.platform.annotation.OperationAudit(module = "Team", operation = "Delete Team")
    public Result<Boolean> delete(@PathVariable Integer id) {
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");

        Team team = teamService.getById(id);
        if (team == null) return Result.error("团队不存在");

        // Permission: Only System Admin can delete a team
        boolean isSystemAdmin = "admin".equalsIgnoreCase(user.getRole());
        if (!isSystemAdmin) {
            return Result.error("只有系统管理员可以删除团队");
        }

        team.setIsDeleted(true);
        teamService.updateById(team);
        projectService.lambdaUpdate().eq(com.automatedtest.platform.entity.Project::getTeamId, id)
                .set(com.automatedtest.platform.entity.Project::getStatus, "archived")
                .set(com.automatedtest.platform.entity.Project::getIsDeleted, true)
                .update();
        teamMemberService.remove(new QueryWrapper<TeamMember>().eq("team_id", id));
        return Result.success(true);
    }

    @PutMapping("/{id}/members/{memberId}")
    public Result<Boolean> updateMemberRole(@PathVariable Integer id, @PathVariable Integer memberId, @RequestBody TeamMember member) {
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        TeamMember currentMember = teamMemberService.getOne(new QueryWrapper<TeamMember>()
            .eq("team_id", id)
            .eq("user_id", user.getId()));
            
        boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());
        if (!isAdmin && (currentMember == null || !"admin".equalsIgnoreCase(currentMember.getRole()))) {
            return Result.error("无权限修改成员角色");
        }
        
        TeamMember targetMember = teamMemberService.getById(memberId);
        if (targetMember == null) {
             return Result.error("成员不存在");
        }
        if (!targetMember.getTeamId().equals(id)) {
             return Result.error("成员不属于该团队");
        }
        
        targetMember.setRole(member.getRole());
        return Result.success(teamMemberService.updateById(targetMember));
    }
}
