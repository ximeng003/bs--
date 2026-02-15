package com.automatedtest.platform.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardStatsDTO {
    private long totalCases;
    private long passedCases;
    private long failedCases;
    private long totalExecutions;
    private double avgDuration; // in seconds
    
    private List<DailyTrendDTO> dailyTrend;
    private List<RecentActivityDTO> recentActivity;
}
