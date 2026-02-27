package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.dto.DailyTrendDTO;
import com.automatedtest.platform.dto.DashboardStatsDTO;
import com.automatedtest.platform.dto.RecentActivityDTO;
import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.entity.TestReport;
import com.automatedtest.platform.mapper.TestCaseMapper;
import com.automatedtest.platform.mapper.TestReportMapper;
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

    @Override
    public DashboardStatsDTO getDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        
        // 1. Top Cards (Test Case Status)
        stats.setTotalCases(testCaseMapper.selectCount(null));
        stats.setPassedCases(testCaseMapper.selectCount(new QueryWrapper<TestCase>().eq("last_result", "success")));
        stats.setFailedCases(testCaseMapper.selectCount(new QueryWrapper<TestCase>().eq("last_result", "failed")));
        stats.setTotalExecutions(testReportMapper.selectCount(null));
        
        Double avg = testReportMapper.getAvgDuration();
        stats.setAvgDuration(avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0);
        
        // 2. Daily Trend (Last 7 days)
        LocalDateTime startDate = LocalDateTime.now().minusDays(6).withHour(0).withMinute(0).withSecond(0);
        
        // Fetch raw reports since startDate
        QueryWrapper<TestReport> reportQuery = new QueryWrapper<>();
        reportQuery.ge("executed_at", startDate);
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
