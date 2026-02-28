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

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<DashboardStatsDTO> getStats() {
        User user = UserContext.getCurrentUser();
        String username = user != null ? user.getUsername() : null;
        Integer projectId = UserContext.getCurrentProjectId();
        
        return Result.success(dashboardService.getDashboardStats(username, projectId));
    }
}
