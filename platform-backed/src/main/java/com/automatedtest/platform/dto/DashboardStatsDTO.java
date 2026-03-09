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
    private List<RunningTaskDTO> myRunningTasks;
    private List<FailedItemDTO> myFailedPlans;
    private List<FailedItemDTO> topFailedCases;
    
    public void setTotalCases(long totalCases) { this.totalCases = totalCases; }
    public void setTotalExecutions(long totalExecutions) { this.totalExecutions = totalExecutions; }
    public void setPassedCases(long passedCases) { this.passedCases = passedCases; }
    public void setFailedCases(long failedCases) { this.failedCases = failedCases; }
    public long getTotalCases() { return totalCases; }
    public long getPassedCases() { return passedCases; }
    public void setPassRate(double passRate) { this.passRate = passRate; }
    public void setMyRunningTasks(List<RunningTaskDTO> myRunningTasks) { this.myRunningTasks = myRunningTasks; }
    public void setMyFailedPlans(List<FailedItemDTO> myFailedPlans) { this.myFailedPlans = myFailedPlans; }
    public void setAvgDuration(double avgDuration) { this.avgDuration = avgDuration; }
    public void setDailyTrend(List<DailyTrendDTO> dailyTrend) { this.dailyTrend = dailyTrend; }
    public void setCoverage(double coverage) { this.coverage = coverage; }
    public void setStability(double stability) { this.stability = stability; }
    public void setHealthScore(double healthScore) { this.healthScore = healthScore; }
    public void setRecentActivity(List<RecentActivityDTO> recentActivity) { this.recentActivity = recentActivity; }
}
