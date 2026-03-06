package com.automatedtest.platform.dto;

import com.automatedtest.platform.entity.TestPlan;
import lombok.Data;
import java.util.List;

@Data
public class DashboardStatsDTO {
    private long totalCases;
    private long passedCases;
    private long failedCases;
    private long totalExecutions;
    private double avgDuration; // in seconds
    private double passRate; // 0-100
    private double coverage; // 0-100
    private double stability; // 0-100
    private double healthScore; // 0-100
    
    private List<DailyTrendDTO> dailyTrend;
    private List<RecentActivityDTO> recentActivity;
    
    // New fields for workbench snapshot
    private List<TestPlan> myRunningTasks;
    private List<TestPlan> myFailedPlans;
}
