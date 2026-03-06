package com.automatedtest.platform.interceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.automatedtest.platform.common.UserContext;
import com.automatedtest.platform.entity.Project;
import com.automatedtest.platform.entity.TeamMember;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.ProjectService;
import com.automatedtest.platform.service.TeamMemberService;
import com.automatedtest.platform.service.UserService;
import com.automatedtest.platform.service.UserApiKeyService;
import com.automatedtest.platform.service.ProjectApiKeyService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.automatedtest.platform.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private UserApiKeyService userApiKeyService;

    @Autowired
    private ProjectApiKeyService projectApiKeyService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TeamMemberService teamMemberService;
    @Autowired
    private JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserContext.clear();

        // 1. Resolve User
        User user = null;
        Integer projectKeyProjectId = null;

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            try {
                String username = jwtService.parseUsername(token);
                if (username != null && !username.isEmpty()) {
                    user = userService.getOne(new QueryWrapper<User>().eq("username", username));
                }
            } catch (Exception ignored) {
            }
        }

        String apiKey = request.getHeader("X-API-KEY");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            // Check User Key first
            Long userId = userApiKeyService.verifyKey(apiKey.trim());
            if (userId != null) {
                user = userService.getById(userId);
            } else {
                // Check Project Key
                try {
                    com.automatedtest.platform.entity.ProjectApiKey pKey = projectApiKeyService.verifyKey(apiKey.trim());
                    if (pKey != null) {
                        projectKeyProjectId = pKey.getProjectId();
                        // For Project Key, set user to the key creator
                        if (user == null && pKey.getUserId() != null) {
                            user = userService.getById(Long.valueOf(pKey.getUserId()));
                        }
                    }
                } catch (Exception e) {
                    // Ignore, invalid key
                }
            }
        }

        // X-User-Name path removed to avoid header spoofing

        if (user != null) {
            UserContext.setCurrentUser(user);
        } else {
             System.out.println("Authentication failed. X-User-Name: " + request.getHeader("X-User-Name"));
            String uri = request.getRequestURI();
            boolean isAuthOrPublic = uri.contains("/api/auth/") || uri.contains("/api/public/");
            if (!isAuthOrPublic) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                return false;
            }
        }

        // 2. Resolve Project Context
        Integer projectId = null;
        String projectIdStr = request.getHeader("X-Project-Id");
        
        if (projectKeyProjectId != null) {
            // If API Key dictates project, enforce it
            projectId = projectKeyProjectId;
            if (projectIdStr != null && !projectIdStr.trim().isEmpty()) {
                try {
                    int requestedPid = Integer.parseInt(projectIdStr.trim());
                    if (requestedPid != projectId) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "API Key does not match requested Project ID");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    // Ignore malformed header if key is valid
                }
            }
        } else if (projectIdStr != null && !projectIdStr.trim().isEmpty()) {
            try {
                projectId = Integer.parseInt(projectIdStr.trim());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // 3. Validate Access (if User is present and Project is requested)
        if (projectId != null) {
            String uri = request.getRequestURI();
            boolean isGlobalEndpoint =
                    uri.contains("/api/projects") ||
                    uri.contains("/api/teams") ||
                    uri.contains("/api/user") ||
                    uri.contains("/api/system");

            if (isGlobalEndpoint) {
                return true;
            }

            Project project = projectService.getById(projectId);
            if (project == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Project not found");
                return false;
            }

            if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
                long count = teamMemberService.count(new QueryWrapper<TeamMember>()
                        .eq("team_id", project.getTeamId())
                        .eq("user_id", user.getId().intValue()));
                if (count == 0) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have access to this project");
                    return false;
                }
            }
            UserContext.setCurrentProjectId(projectId);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
