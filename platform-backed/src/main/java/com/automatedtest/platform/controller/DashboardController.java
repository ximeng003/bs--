package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.common.UserContext;
import com.automatedtest.platform.dto.DashboardStatsDTO;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<DashboardStatsDTO> getStats(@RequestHeader(value = "X-Project-Id", required = false) String headerProjectId) {
        User user = UserContext.getCurrentUser();
        String username = user != null ? user.getUsername() : null;
        Integer projectId = UserContext.getCurrentProjectId();
        
        // Fallback to header if context is empty
        if (projectId == null && headerProjectId != null) {
            try {
                projectId = Integer.parseInt(headerProjectId);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        
        System.out.println("Dashboard Stats Request - User: " + username + ", ProjectId: " + projectId + " (Header: " + headerProjectId + ")");
        
        return Result.success(dashboardService.getDashboardStats(username, projectId));
    }
}
