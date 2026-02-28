package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.dto.DailyTrendDTO;
import com.automatedtest.platform.dto.DashboardStatsDTO;
import com.automatedtest.platform.dto.RecentActivityDTO;
import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.entity.TestPlan;
import com.automatedtest.platform.entity.TestReport;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.mapper.TestCaseMapper;
import com.automatedtest.platform.mapper.TestPlanMapper;
import com.automatedtest.platform.mapper.TestReportMapper;
import com.automatedtest.platform.mapper.UserMapper;
import com.automatedtest.platform.service.DashboardService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private TestCaseMapper testCaseMapper;
    
    @Autowired
    private TestReportMapper testReportMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TestPlanMapper testPlanMapper;

    @Override
    public DashboardStatsDTO getDashboardStats(String username, Integer projectId) {
        String normalizedUsername = username != null ? username.trim() : null;
        User user = null;
        if (normalizedUsername != null && !normalizedUsername.isEmpty()) {
            user = userMapper.selectOne(new QueryWrapper<User>().eq("username", normalizedUsername));
        }
        boolean isAdmin = user != null && user.getRole() != null && "admin".equalsIgnoreCase(user.getRole());
        Long userId = user != null ? user.getId() : null;
        
        Integer finalProjectId = projectId;
        if (finalProjectId == null) {
            if (!isAdmin) {
                finalProjectId = 1; // Default to project 1 for non-admin if no project selected
            }
            // If admin and projectId is null, we assume "All Projects" view
        }

        DashboardStatsDTO stats = new DashboardStatsDTO();
        
        // 1. Top Cards (Test Case Status) - FILTER BY PROJECT
        QueryWrapper<TestCase> caseQuery = new QueryWrapper<>();
        if (finalProjectId != null) caseQuery.eq("project_id", finalProjectId);
        stats.setTotalCases(testCaseMapper.selectCount(caseQuery));
        
        QueryWrapper<TestCase> passedQuery = new QueryWrapper<>();
        if (finalProjectId != null) passedQuery.eq("project_id", finalProjectId);
        passedQuery.eq("last_result", "success");
        stats.setPassedCases(testCaseMapper.selectCount(passedQuery));
        
        QueryWrapper<TestCase> failedQuery = new QueryWrapper<>();
        if (finalProjectId != null) failedQuery.eq("project_id", finalProjectId);
        failedQuery.eq("last_result", "failed");
        stats.setFailedCases(testCaseMapper.selectCount(failedQuery));
        
        QueryWrapper<TestReport> executionQuery = new QueryWrapper<>();
        if (finalProjectId != null) executionQuery.eq("project_id", finalProjectId);
        stats.setTotalExecutions(testReportMapper.selectCount(executionQuery));

        // Avg Duration
        QueryWrapper<TestReport> avgQuery = new QueryWrapper<>();
        avgQuery.select("avg(execution_time) as avg_time");
        if (finalProjectId != null) avgQuery.eq("project_id", finalProjectId);
        List<Map<String, Object>> avgResult = testReportMapper.selectMaps(avgQuery);
        Double avg = 0.0;
        if (avgResult != null && !avgResult.isEmpty() && avgResult.get(0) != null) {
            Object val = avgResult.get(0).get("avg_time");
            if (val != null) {
                avg = Double.parseDouble(val.toString());
            }
        }
        stats.setAvgDuration(Math.round(avg * 100.0) / 100.0);
        
        // 2. Daily Trend (Last 7 days)
        LocalDateTime startDate = LocalDateTime.now().minusDays(6).withHour(0).withMinute(0).withSecond(0);
        
        // Fetch raw reports since startDate FILTERED BY PROJECT
        QueryWrapper<TestReport> reportQuery = new QueryWrapper<>();
        reportQuery.ge("executed_at", startDate);
        if (finalProjectId != null) reportQuery.eq("project_id", finalProjectId);

        List<TestReport> rawReports = testReportMapper.selectList(reportQuery);
        
        // Fetch related test cases for type info
        Map<Integer, String> caseTypeMap = new HashMap<>();
        if (rawReports != null && !rawReports.isEmpty()) {
            Set<Integer> caseIds = rawReports.stream()
                .map(TestReport::getCaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            if (!caseIds.isEmpty()) {
                List<TestCase> cases = testCaseMapper.selectBatchIds(caseIds);
                for (TestCase c : cases) {
                    caseTypeMap.put(c.getId(), c.getType());
                }
            }
        }
        
        // Debug Log
        System.out.println("Dashboard Stats: Fetching daily stats from " + startDate);
        System.out.println("Dashboard Stats: Found " + (rawReports != null ? rawReports.size() : 0) + " raw reports for aggregation");

        List<DailyTrendDTO> dailyTrend = new ArrayList<>();
        Map<String, DailyTrendDTO> dateMap = new HashMap<>();
        Map<String, long[]> perTypeCounters = new HashMap<>(); // key: date|type, val: [total, passed]
        
        // Init last 7 days
        for (int i = 0; i < 7; i++) {
            String dateStr = startDate.plusDays(i).format(DateTimeFormatter.ofPattern("MM-dd"));
            DailyTrendDTO dto = new DailyTrendDTO();
            dto.setDate(dateStr);
            dto.setPassed(0);
            dto.setFailed(0);
            dto.setApiCount(0);
            dto.setWebCount(0);
            dto.setAppCount(0);
            dto.setApiPassRate(0);
            dto.setWebPassRate(0);
            dto.setAppPassRate(0);
            dailyTrend.add(dto);
            dateMap.put(dateStr, dto);
        }
        
        if (rawReports != null) {
            for (TestReport report : rawReports) {
                if (report.getExecutedAt() == null) continue;
                
                String dateStr = report.getExecutedAt().format(DateTimeFormatter.ofPattern("MM-dd"));
                String status = report.getStatus();
                String type = caseTypeMap.get(report.getCaseId()); // Can be null if case deleted
                
                DailyTrendDTO dto = dateMap.get(dateStr);
                if (dto != null) {
                    if ("success".equalsIgnoreCase(status)) {
                        dto.setPassed(dto.getPassed() + 1);
                    } else {
                        dto.setFailed(dto.getFailed() + 1);
                    }
                }
    
                if (type != null && dto != null) {
                    String upperType = type.toUpperCase();
                    String key = dateStr + "|" + upperType;
                    long[] counters = perTypeCounters.computeIfAbsent(key, k -> new long[2]);
                    counters[0] += 1; // total
                    if ("success".equalsIgnoreCase(status)) {
                        counters[1] += 1; // passed
                    }
                }
            }
        }

        // Fill calculated rates back to dailyTrend DTOs
        for (DailyTrendDTO dto : dailyTrend) {
            String dateStr = dto.getDate();

            long[] apiCounters = perTypeCounters.get(dateStr + "|API");
            if (apiCounters != null) {
                dto.setApiCount(apiCounters[0]);
                if (apiCounters[0] > 0) {
                    dto.setApiPassRate(Math.round((apiCounters[1] * 10000.0) / apiCounters[0]) / 100.0);
                }
            }

            long[] webCounters = perTypeCounters.get(dateStr + "|WEB");
            if (webCounters != null) {
                dto.setWebCount(webCounters[0]);
                if (webCounters[0] > 0) {
                    dto.setWebPassRate(Math.round((webCounters[1] * 10000.0) / webCounters[0]) / 100.0);
                }
            }

            long[] appCounters = perTypeCounters.get(dateStr + "|APP");
            if (appCounters != null) {
                dto.setAppCount(appCounters[0]);
                if (appCounters[0] > 0) {
                    dto.setAppPassRate(Math.round((appCounters[1] * 10000.0) / appCounters[0]) / 100.0);
                }
            }
        }
        stats.setDailyTrend(dailyTrend);
        
        // 3. Recent Activity (Modified to use MP query instead of custom SQL for robustness)
        QueryWrapper<TestReport> recentWrapper = new QueryWrapper<>();
        recentWrapper.eq("project_id", finalProjectId);
        recentWrapper.orderByDesc("executed_at");
        recentWrapper.last("LIMIT 10");
        List<TestReport> recentReports = testReportMapper.selectList(recentWrapper);
        
        System.out.println("Dashboard Stats: Found " + (recentReports != null ? recentReports.size() : 0) + " recent reports");

        List<RecentActivityDTO> recentActivity = new ArrayList<>();
        if (recentReports != null && !recentReports.isEmpty()) {
            Set<Integer> caseIds = recentReports.stream()
                .map(TestReport::getCaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            Map<Integer, String> caseNameMap = new HashMap<>();
            if (!caseIds.isEmpty()) {
                List<TestCase> cases = testCaseMapper.selectBatchIds(caseIds);
                for (TestCase c : cases) {
                    caseNameMap.put(c.getId(), c.getName());
                }
            }
            
            for (TestReport r : recentReports) {
                RecentActivityDTO dto = new RecentActivityDTO();
                String name = caseNameMap.get(r.getCaseId());
                dto.setCaseName(name != null ? name : "Unknown Case (ID:" + r.getCaseId() + ")");
                dto.setStatus(r.getStatus());
                dto.setExecutedBy(r.getExecutedBy() != null ? r.getExecutedBy() : "System");
                
                if (r.getExecutedAt() != null) {
                    dto.setTimeAgo(formatTimeAgo(r.getExecutedAt()));
                } else {
                    dto.setTimeAgo("未知");
                }
                recentActivity.add(dto);
            }
        }
        
        stats.setRecentActivity(recentActivity);
        
        // 4. Workbench Snapshot (My Tasks & Exception Reminders)
        if (userId != null) {
            // My Running Tasks
            QueryWrapper<TestPlan> runningQuery = new QueryWrapper<>();
            runningQuery.eq("created_by", userId);
            runningQuery.eq("last_run_status", "Running");
            runningQuery.orderByDesc("last_run_time");
            runningQuery.last("LIMIT 5");
            stats.setMyRunningTasks(testPlanMapper.selectList(runningQuery));

            // Exception Reminders (Failed Plans)
            QueryWrapper<TestPlan> failedPlanQuery = new QueryWrapper<>();
            failedPlanQuery.eq("created_by", userId);
            failedPlanQuery.eq("last_run_status", "Failed");
            failedPlanQuery.orderByDesc("last_run_time");
            failedPlanQuery.last("LIMIT 5");
            stats.setMyFailedPlans(testPlanMapper.selectList(failedPlanQuery));
        }

        return stats;
    }
    
    private String formatTimeAgo(LocalDateTime dateTime) {
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) return "刚刚";
        if (seconds < 3600) return (seconds / 60) + "分钟前";
        if (seconds < 86400) return (seconds / 3600) + "小时前";
        return (seconds / 86400) + "天前";
    }
}
