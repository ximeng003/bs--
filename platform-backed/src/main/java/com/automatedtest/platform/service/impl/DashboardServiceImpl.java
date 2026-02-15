package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.dto.DailyTrendDTO;
import com.automatedtest.platform.dto.DashboardStatsDTO;
import com.automatedtest.platform.dto.RecentActivityDTO;
import com.automatedtest.platform.entity.TestCase;
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
        List<Map<String, Object>> dailyRaw = testReportMapper.getDailyStats(startDate);

        List<DailyTrendDTO> dailyTrend = new ArrayList<>();
        Map<String, DailyTrendDTO> dateMap = new HashMap<>();
        Map<String, long[]> perTypeCounters = new HashMap<>();
        
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
        
        for (Map<String, Object> record : dailyRaw) {
            Object dateObj = record.get("date");
            String dateStr;
            if (dateObj == null) continue;
            
            if (dateObj instanceof java.sql.Date) {
                 dateStr = ((java.sql.Date) dateObj).toLocalDate().format(DateTimeFormatter.ofPattern("MM-dd"));
            } else if (dateObj instanceof java.sql.Timestamp) {
                 dateStr = ((java.sql.Timestamp) dateObj).toLocalDateTime().format(DateTimeFormatter.ofPattern("MM-dd"));
            } else if (dateObj instanceof LocalDateTime) {
                 dateStr = ((LocalDateTime) dateObj).format(DateTimeFormatter.ofPattern("MM-dd"));
            } else {
                 dateStr = dateObj.toString();
                 if (dateStr.length() >= 10) {
                     // Check format like yyyy-MM-dd
                     try {
                         // Simple heuristic: assume yyyy-MM-dd at start
                         dateStr = dateStr.substring(5, 10);
                     } catch (Exception e) {
                         // ignore
                     }
                 }
            }
            
            String status = (String) record.get("status");
            String type = (String) record.get("type");
            Number countNum = (Number) record.get("count");
            long count = countNum != null ? countNum.longValue() : 0;
            
            DailyTrendDTO dto = dateMap.get(dateStr);
            if (dto != null) {
                if ("success".equalsIgnoreCase(status)) {
                    dto.setPassed(dto.getPassed() + count);
                } else {
                    dto.setFailed(dto.getFailed() + count);
                }
            }

            if (type != null) {
                String upperType = type.toUpperCase();
                String key = dateStr + "|" + upperType;
                long[] counters = perTypeCounters.computeIfAbsent(key, k -> new long[2]);
                counters[0] += count;
                if ("success".equalsIgnoreCase(status)) {
                    counters[1] += count;
                }
            }
        }

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
        
        // 3. Recent Activity
        List<Map<String, Object>> recentRaw = testReportMapper.getRecentActivity();
        List<RecentActivityDTO> recentActivity = recentRaw.stream().map(r -> {
            RecentActivityDTO dto = new RecentActivityDTO();
            dto.setCaseName((String) r.get("caseName"));
            dto.setStatus((String) r.get("status"));
            dto.setExecutedBy((String) r.get("executedBy"));
            
            Object executedAtObj = r.get("executedAt");
            LocalDateTime executedAt = null;
            if (executedAtObj instanceof java.sql.Timestamp) {
                executedAt = ((java.sql.Timestamp) executedAtObj).toLocalDateTime();
            } else if (executedAtObj instanceof LocalDateTime) {
                executedAt = (LocalDateTime) executedAtObj;
            }
            
            if (executedAt != null) {
                dto.setTimeAgo(formatTimeAgo(executedAt));
            } else {
                dto.setTimeAgo("未知");
            }
            return dto;
        }).collect(Collectors.toList());
        
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
