package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.dto.DailyTrendDTO;
import com.automatedtest.platform.dto.DashboardStatsDTO;
import com.automatedtest.platform.dto.RecentActivityDTO;
import com.automatedtest.platform.dto.RunningTaskDTO;
import com.automatedtest.platform.dto.FailedItemDTO;
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
        try {
            String normalizedUsername = username != null ? username.trim() : null;
            User user = null;
            if (normalizedUsername != null && !normalizedUsername.isEmpty()) {
                user = userMapper.selectOne(new QueryWrapper<User>().eq("username", normalizedUsername));
            }
            
            // 修正：从 UserContext 获取当前项目ID，如果参数未传递
            if (projectId == null) {
                projectId = com.automatedtest.platform.common.UserContext.getCurrentProjectId();
            }
            
            Integer finalProjectId = projectId;
            boolean isAdmin = user != null && user.getRole() != null && "admin".equalsIgnoreCase(user.getRole());
            Long userId = user != null ? user.getId() : null;
            
            // 如果不是管理员且未指定项目，尝试查找用户所属的第一个项目
            if (finalProjectId == null && !isAdmin && user != null) {
                // 这里可以添加逻辑查找用户的默认项目，暂时保持为 null
            }
            
            System.out.println("Generating Dashboard Stats. User: " + normalizedUsername + ", ProjectId: " + finalProjectId);

            DashboardStatsDTO stats = new DashboardStatsDTO();
        // 1. Top Cards (Test Case Status) - FILTER BY PROJECT
        QueryWrapper<TestCase> caseTotalQuery = new QueryWrapper<>();
        if (finalProjectId != null) {
            caseTotalQuery.eq("project_id", finalProjectId);
        }
        Long totalCases = testCaseMapper.selectCount(caseTotalQuery);
        System.out.println("Dashboard Debug: Total Cases = " + totalCases + " (Project: " + finalProjectId + ")");
        stats.setTotalCases(totalCases);
        
        QueryWrapper<TestReport> totalExecQuery = new QueryWrapper<>();
        if (finalProjectId != null) {
            totalExecQuery.eq("project_id", finalProjectId);
        }
        Long totalExecutions = testReportMapper.selectCount(totalExecQuery);
        System.out.println("Dashboard Debug: Total Executions = " + totalExecutions + " (Project: " + finalProjectId + ")");
        stats.setTotalExecutions(totalExecutions);

        // Passed Cases
        QueryWrapper<TestCase> passedCaseQuery = new QueryWrapper<>();
        if (finalProjectId != null) passedCaseQuery.eq("project_id", finalProjectId);
        passedCaseQuery.eq("last_result", "success");
        Long passedCases = testCaseMapper.selectCount(passedCaseQuery);
        System.out.println("Dashboard Debug: Passed Cases = " + passedCases);
        stats.setPassedCases(passedCases);
        
        // Failed Cases
        QueryWrapper<TestCase> failedCaseQuery = new QueryWrapper<>();
        if (finalProjectId != null) failedCaseQuery.eq("project_id", finalProjectId);
        failedCaseQuery.eq("last_result", "failed");
        Long failedCases = testCaseMapper.selectCount(failedCaseQuery);
        System.out.println("Dashboard Debug: Failed Cases = " + failedCases);
        stats.setFailedCases(failedCases);

        // Avg Duration
        QueryWrapper<TestReport> avgQuery = new QueryWrapper<>();
        if (finalProjectId != null) avgQuery.eq("project_id", finalProjectId);
        avgQuery.select("avg(execution_time) as avg_time");
        List<Map<String, Object>> avgResult = testReportMapper.selectMaps(avgQuery);
        Double avg = 0.0;
        if (avgResult != null && !avgResult.isEmpty() && avgResult.get(0) != null) {
            Object val = avgResult.get(0).get("avg_time");
            if (val != null) {
                try {
                    avg = Double.parseDouble(val.toString());
                } catch (NumberFormatException e) {
                    avg = 0.0;
                }
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
        
        // Fallback: If no reports found in last 7 days, derive trend from test_cases.last_run / last_result
        if ((rawReports == null || rawReports.isEmpty())) {
            QueryWrapper<TestCase> caseTrendQuery = new QueryWrapper<>();
            caseTrendQuery.ge("last_run", startDate);
            if (finalProjectId != null) caseTrendQuery.eq("project_id", finalProjectId);
            List<TestCase> recentCases = testCaseMapper.selectList(caseTrendQuery);
            if (recentCases != null) {
                for (TestCase c : recentCases) {
                    if (c.getLastRun() == null) continue;
                    String dateStr = c.getLastRun().format(DateTimeFormatter.ofPattern("MM-dd"));
                    String status = c.getLastResult();
                    String type = c.getType();
                    DailyTrendDTO dto = dateMap.get(dateStr);
                    if (dto != null) {
                        if ("success".equalsIgnoreCase(String.valueOf(status))) {
                            dto.setPassed(dto.getPassed() + 1);
                        } else if ("failed".equalsIgnoreCase(String.valueOf(status))) {
                            dto.setFailed(dto.getFailed() + 1);
                        }
                    }
                    if (type != null && dto != null) {
                        String upperType = type.toUpperCase();
                        String key = dateStr + "|" + upperType;
                        long[] counters = perTypeCounters.computeIfAbsent(key, k -> new long[2]);
                        counters[0] += 1;
                        if ("success".equalsIgnoreCase(String.valueOf(status))) {
                            counters[1] += 1;
                        }
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
        
        // Compute passRate
        long totalForRate = stats.getPassedCases() + stats.getFailedCases();
        double passRate = totalForRate > 0 ? Math.round((stats.getPassedCases() * 10000.0) / totalForRate) / 100.0 : 0.0;
        stats.setPassRate(passRate);
        
        // Coverage: unique executed cases in last 7 days / total cases
        QueryWrapper<TestReport> coverageWrapper = new QueryWrapper<>();
        coverageWrapper.ge("executed_at", startDate);
        if (finalProjectId != null) coverageWrapper.eq("project_id", finalProjectId);
        List<TestReport> covReports = testReportMapper.selectList(coverageWrapper);
        Set<Integer> executedCaseIds = new HashSet<>();
        if (covReports != null) {
            for (TestReport r : covReports) {
                if (r.getCaseId() != null) executedCaseIds.add(r.getCaseId());
            }
        }
        double coverage = stats.getTotalCases() > 0 ? Math.round((executedCaseIds.size() * 10000.0) / stats.getTotalCases()) / 100.0 : 0.0;
        stats.setCoverage(coverage);
        
        // Stability: 1 - stddev/mean of execution time (last 7 days), clamp to [0,1], then scale 0-100
        List<Integer> durations = new ArrayList<>();
        if (covReports != null) {
            for (TestReport r : covReports) {
                if (r.getExecutionTime() != null && r.getExecutionTime() > 0) durations.add(r.getExecutionTime());
            }
        }
        double stability = 0.0;
        if (!durations.isEmpty()) {
            double mean = durations.stream().mapToDouble(d -> d).average().orElse(0.0);
            double variance = durations.stream().mapToDouble(d -> (d - mean) * (d - mean)).sum() / durations.size();
            double stddev = Math.sqrt(variance);
            double ratio = mean > 0 ? (stddev / mean) : 1.0;
            double s = 1.0 - Math.min(Math.max(ratio, 0.0), 1.0);
            stability = Math.round(s * 10000.0) / 100.0;
        }
        stats.setStability(stability);
        
        // Health Score: 0.6*passRate + 0.2*coverage + 0.2*stability
        double healthScore = Math.round((passRate * 0.6 + coverage * 0.2 + stability * 0.2) * 100.0) / 100.0;
        stats.setHealthScore(healthScore);
        
        // 3. Recent Activity
        QueryWrapper<TestReport> recentWrapper = new QueryWrapper<>();
        if (finalProjectId != null) {
            recentWrapper.eq("project_id", finalProjectId);
        }
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
                dto.setTimestamp(r.getExecutedAt());
                
                if (r.getExecutedAt() != null) {
                    dto.setTimeAgo(formatTimeAgo(r.getExecutedAt()));
                } else {
                    dto.setTimeAgo("未知");
                }
                recentActivity.add(dto);
            }
        }
        // Fallback: if no recent reports, use recent test cases by last_run
        if (recentActivity.isEmpty()) {
            QueryWrapper<TestCase> lastRunQuery = new QueryWrapper<>();
            if (finalProjectId != null) lastRunQuery.eq("project_id", finalProjectId);
            lastRunQuery.orderByDesc("last_run");
            lastRunQuery.last("LIMIT 10");
            List<TestCase> recentRunCases = testCaseMapper.selectList(lastRunQuery);
            if (recentRunCases != null) {
                for (TestCase c : recentRunCases) {
                    if (c.getLastRun() == null) continue;
                    RecentActivityDTO dto = new RecentActivityDTO();
                    dto.setCaseName(c.getName());
                    dto.setStatus(c.getLastResult());
                    dto.setExecutedBy("System");
                    dto.setTimestamp(c.getLastRun());
                    dto.setTimeAgo(formatTimeAgo(c.getLastRun()));
                    recentActivity.add(dto);
                }
            }
        }
        
        stats.setRecentActivity(recentActivity);
        
        // 4. Workbench Snapshot (My Tasks & Exception Reminders)
        List<RunningTaskDTO> myRunningTasks = new ArrayList<>();
        
        // My Running Test Plans
        QueryWrapper<TestPlan> runningPlanQuery = new QueryWrapper<>();
        if (finalProjectId != null) runningPlanQuery.eq("project_id", finalProjectId);
        if (userId != null && !isAdmin) runningPlanQuery.eq("created_by", userId);
        runningPlanQuery.eq("last_run_status", "Running");
        runningPlanQuery.orderByDesc("last_run_time");
        runningPlanQuery.last("LIMIT 5");
        List<TestPlan> runningPlans = testPlanMapper.selectList(runningPlanQuery);
        if (runningPlans != null) {
            for (TestPlan p : runningPlans) {
                RunningTaskDTO rtd = new RunningTaskDTO();
                rtd.setId("plan-" + p.getId());
                rtd.setName("[计划] " + p.getName());
                rtd.setLastRunTime(p.getLastRunTime());
                rtd.setType("Plan");
                
                // Find latest report for this plan
                QueryWrapper<TestReport> planReportQuery = new QueryWrapper<>();
                planReportQuery.eq("plan_id", p.getId());
                planReportQuery.orderByDesc("id");
                planReportQuery.last("LIMIT 1");
                TestReport latest = testReportMapper.selectOne(planReportQuery);
                if (latest != null) {
                    rtd.setReportId(latest.getId());
                }
                
                myRunningTasks.add(rtd);
            }
        }

        // My Running Test Cases (Manual execution)
        QueryWrapper<TestCase> runningCaseQuery = new QueryWrapper<>();
        if (finalProjectId != null) runningCaseQuery.eq("project_id", finalProjectId);
        if (userId != null && !isAdmin) runningCaseQuery.eq("created_by", userId);
        runningCaseQuery.eq("last_result", "running");
        runningCaseQuery.orderByDesc("last_run");
        runningCaseQuery.last("LIMIT 5");
        List<TestCase> runningCases = testCaseMapper.selectList(runningCaseQuery);
        if (runningCases != null) {
            for (TestCase c : runningCases) {
                RunningTaskDTO rtd = new RunningTaskDTO();
                rtd.setId("case-" + c.getId());
                rtd.setName("[单例] " + c.getName());
                rtd.setLastRunTime(c.getLastRun());
                rtd.setType("Case");
                
                // Find latest report for this case
                QueryWrapper<TestReport> caseReportQuery = new QueryWrapper<>();
                caseReportQuery.eq("case_id", c.getId());
                caseReportQuery.isNull("plan_id"); // only single case reports
                caseReportQuery.orderByDesc("id");
                caseReportQuery.last("LIMIT 1");
                TestReport latest = testReportMapper.selectOne(caseReportQuery);
                if (latest != null) {
                    rtd.setReportId(latest.getId());
                }
                
                myRunningTasks.add(rtd);
            }
        }
        
        // Sort by time descending and limit to 5
        myRunningTasks.sort((a, b) -> {
            if (a.getLastRunTime() == null) return 1;
            if (b.getLastRunTime() == null) return -1;
            return b.getLastRunTime().compareTo(a.getLastRunTime());
        });
        if (myRunningTasks.size() > 5) {
            myRunningTasks = myRunningTasks.subList(0, 5);
        }
        stats.setMyRunningTasks(myRunningTasks);

        // Exception Reminders (Failed Plans & Failed Cases)
        List<FailedItemDTO> myFailedItems = new ArrayList<>();
        
        // Failed Test Plans
        QueryWrapper<TestPlan> failedPlanQuery = new QueryWrapper<>();
        if (finalProjectId != null) failedPlanQuery.eq("project_id", finalProjectId);
        if (userId != null && !isAdmin) failedPlanQuery.eq("created_by", userId);
        failedPlanQuery.eq("last_run_status", "Failed");
        failedPlanQuery.orderByDesc("last_run_time");
        failedPlanQuery.last("LIMIT 5");
        List<TestPlan> failedPlans = testPlanMapper.selectList(failedPlanQuery);
        if (failedPlans != null) {
            for (TestPlan p : failedPlans) {
                FailedItemDTO fid = new FailedItemDTO();
                fid.setId("plan-" + p.getId());
                fid.setName("[计划] " + p.getName());
                fid.setLastRunTime(p.getLastRunTime());
                fid.setType("Plan");
                
                // Find latest report for this plan
                QueryWrapper<TestReport> failedPlanReportQuery = new QueryWrapper<>();
                failedPlanReportQuery.eq("plan_id", p.getId());
                failedPlanReportQuery.orderByDesc("id");
                failedPlanReportQuery.last("LIMIT 1");
                TestReport latest = testReportMapper.selectOne(failedPlanReportQuery);
                if (latest != null) {
                    fid.setReportId(latest.getId());
                }
                
                myFailedItems.add(fid);
            }
        }

        // Failed Test Cases (Manual execution)
        QueryWrapper<TestCase> failedCaseManualQuery = new QueryWrapper<>();
        if (finalProjectId != null) failedCaseManualQuery.eq("project_id", finalProjectId);
        if (userId != null && !isAdmin) failedCaseManualQuery.eq("created_by", userId);
        failedCaseManualQuery.eq("last_result", "failed");
        failedCaseManualQuery.orderByDesc("last_run");
        failedCaseManualQuery.last("LIMIT 5");
        List<TestCase> failedManualCases = testCaseMapper.selectList(failedCaseManualQuery);
        if (failedManualCases != null) {
            for (TestCase c : failedManualCases) {
                FailedItemDTO fid = new FailedItemDTO();
                fid.setId("case-" + c.getId());
                fid.setName("[单例] " + c.getName());
                fid.setLastRunTime(c.getLastRun());
                fid.setType("Case");
                
                // Find latest report for this case
                QueryWrapper<TestReport> failedCaseReportQuery = new QueryWrapper<>();
                failedCaseReportQuery.eq("case_id", c.getId());
                failedCaseReportQuery.isNull("plan_id"); // only single case reports
                failedCaseReportQuery.orderByDesc("id");
                failedCaseReportQuery.last("LIMIT 1");
                TestReport latest = testReportMapper.selectOne(failedCaseReportQuery);
                if (latest != null) {
                    fid.setReportId(latest.getId());
                }
                
                myFailedItems.add(fid);
            }
        }

         // Sort by time descending and limit to 5
         myFailedItems.sort((a, b) -> {
             if (a.getLastRunTime() == null) return 1;
             if (b.getLastRunTime() == null) return -1;
             return b.getLastRunTime().compareTo(a.getLastRunTime());
         });
         if (myFailedItems.size() > 5) {
             myFailedItems = myFailedItems.subList(0, 5);
         }
            stats.setMyFailedPlans(myFailedItems);
            
            // Top Failed Cases (last 30 days)
            List<FailedItemDTO> topFailedCases = new ArrayList<>();
            {
                QueryWrapper<TestReport> failQuery = new QueryWrapper<>();
                if (finalProjectId != null) failQuery.eq("project_id", finalProjectId);
                failQuery.eq("status", "failed");
                failQuery.orderByDesc("executed_at");
                List<TestReport> recentFails = testReportMapper.selectList(failQuery);
                Map<Integer, Integer> countMap = new HashMap<>();
                Map<Integer, LocalDateTime> lastTimeMap = new HashMap<>();
                for (TestReport r : recentFails) {
                    if (r.getCaseId() == null) continue;
                    int cid = r.getCaseId();
                    countMap.put(cid, 1 + (countMap.getOrDefault(cid, 0)));
                    if (r.getExecutedAt() != null) {
                        LocalDateTime t = lastTimeMap.get(cid);
                        if (t == null || r.getExecutedAt().isAfter(t)) {
                            lastTimeMap.put(cid, r.getExecutedAt());
                        }
                    }
                }
                if (!countMap.isEmpty()) {
                    List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(countMap.entrySet());
                    entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
                    int limit = Math.min(5, entries.size());
                    Set<Integer> caseIds = entries.stream().limit(limit).map(Map.Entry::getKey).collect(Collectors.toSet());
                    List<TestCase> cases = caseIds.isEmpty() ? Collections.emptyList() : testCaseMapper.selectBatchIds(caseIds);
                    Map<Integer, String> nameMap = new HashMap<>();
                    for (TestCase c : cases) {
                        if (c.getId() != null) nameMap.put(c.getId(), c.getName());
                    }
                    for (int i = 0; i < limit; i++) {
                        Integer cid = entries.get(i).getKey();
                        Integer cnt = entries.get(i).getValue();
                        FailedItemDTO dto = new FailedItemDTO();
                        dto.setId("case-" + cid);
                        dto.setName(nameMap.getOrDefault(cid, "用例#" + cid));
                        dto.setType("Case");
                        dto.setCount(cnt);
                        dto.setLastRunTime(lastTimeMap.get(cid));
                        topFailedCases.add(dto);
                    }
                }
                // Fallback from test_cases if no failed reports
                if (topFailedCases.isEmpty()) {
                    QueryWrapper<TestCase> failedCaseListQuery = new QueryWrapper<>();
                    if (finalProjectId != null) failedCaseListQuery.eq("project_id", finalProjectId);
                    failedCaseListQuery.eq("last_result", "failed");
                    failedCaseListQuery.orderByDesc("last_run");
                    failedCaseListQuery.last("LIMIT 50");
                    List<TestCase> failedCaseList = testCaseMapper.selectList(failedCaseListQuery);
                    Map<Integer, Integer> cntMap = new HashMap<>();
                    Map<Integer, LocalDateTime> lastMap = new HashMap<>();
                    for (TestCase c : failedCaseList) {
                        if (c.getId() == null) continue;
                        int cid = c.getId();
                        cntMap.put(cid, 1 + (cntMap.getOrDefault(cid, 0)));
                        LocalDateTime t = lastMap.get(cid);
                        if (t == null || (c.getLastRun() != null && c.getLastRun().isAfter(t))) {
                            lastMap.put(cid, c.getLastRun());
                        }
                    }
                    if (!cntMap.isEmpty()) {
                        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(cntMap.entrySet());
                        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
                        int limit2 = Math.min(5, entries.size());
                        Set<Integer> caseIds2 = entries.stream().limit(limit2).map(Map.Entry::getKey).collect(Collectors.toSet());
                        List<TestCase> cases2 = caseIds2.isEmpty() ? Collections.emptyList() : testCaseMapper.selectBatchIds(caseIds2);
                        Map<Integer, String> nameMap2 = new HashMap<>();
                        for (TestCase c : cases2) {
                            if (c.getId() != null) nameMap2.put(c.getId(), c.getName());
                        }
                        for (int i = 0; i < limit2; i++) {
                            Integer cid = entries.get(i).getKey();
                            Integer cnt = entries.get(i).getValue();
                            FailedItemDTO dto = new FailedItemDTO();
                            dto.setId("case-" + cid);
                            dto.setName(nameMap2.getOrDefault(cid, "用例#" + cid));
                            dto.setType("Case");
                            dto.setCount(cnt);
                            dto.setLastRunTime(lastMap.get(cid));
                            topFailedCases.add(dto);
                        }
                    }
                }
            }
            stats.setTopFailedCases(topFailedCases);


            return stats;
        } catch (Exception e) {
            System.err.println("Dashboard Stats error: " + e.getMessage());
            try {
                DashboardStatsDTO stats = new DashboardStatsDTO();
                Integer finalProjectId = projectId != null ? projectId : com.automatedtest.platform.common.UserContext.getCurrentProjectId();
                QueryWrapper<TestCase> cQ = new QueryWrapper<>();
                if (finalProjectId != null) cQ.eq("project_id", finalProjectId);
                stats.setTotalCases(testCaseMapper.selectCount(cQ));
                QueryWrapper<TestCase> cSucc = new QueryWrapper<>();
                if (finalProjectId != null) cSucc.eq("project_id", finalProjectId);
                cSucc.eq("last_result", "success");
                stats.setPassedCases(testCaseMapper.selectCount(cSucc));
                QueryWrapper<TestCase> cFail = new QueryWrapper<>();
                if (finalProjectId != null) cFail.eq("project_id", finalProjectId);
                cFail.eq("last_result", "failed");
                stats.setFailedCases(testCaseMapper.selectCount(cFail));
                QueryWrapper<TestReport> rQ = new QueryWrapper<>();
                if (finalProjectId != null) rQ.eq("project_id", finalProjectId);
                stats.setTotalExecutions(testReportMapper.selectCount(rQ));
                stats.setPassRate(stats.getPassedCases() + stats.getFailedCases() > 0
                        ? Math.round((stats.getPassedCases() * 10000.0) / (stats.getPassedCases() + stats.getFailedCases())) / 100.0
                        : 0.0);
                return stats;
            } catch (Exception ignored) {}
            return new DashboardStatsDTO();
        }
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
