package com.automatedtest.platform.mapper;

import com.automatedtest.platform.entity.TestReport;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface TestReportMapper extends BaseMapper<TestReport> {
    @Select("SELECT DATE(r.executed_at) as date, c.type as type, r.status as status, COUNT(*) as count " +
            "FROM test_reports r " +
            "LEFT JOIN test_cases c ON r.case_id = c.id " +
            "WHERE r.executed_at >= #{startDate} " +
            "GROUP BY DATE(r.executed_at), c.type, r.status")
    List<Map<String, Object>> getDailyStats(@Param("startDate") LocalDateTime startDate);

    @Select("SELECT AVG(execution_time) FROM test_reports")
    Double getAvgDuration();

    @Select("SELECT AVG(execution_time) FROM test_reports WHERE executed_by = #{executedBy}")
    Double getAvgDurationByExecutedBy(@Param("executedBy") String executedBy);

    @Select("SELECT r.status, r.executed_by as executedBy, r.executed_at as executedAt, c.name as caseName FROM test_reports r LEFT JOIN test_cases c ON r.case_id = c.id ORDER BY r.executed_at DESC LIMIT 10")
    List<Map<String, Object>> getRecentActivity();

    @Select("<script>" +
            "SELECT * FROM (" +
            "  SELECT r.id, r.project_id as projectId, r.plan_id as planId, r.plan_run_no as planRunNo, r.case_id as caseId, r.status, r.execution_time as executionTime, r.executed_at as executedAt, r.executed_by as executedBy, r.trigger_type as triggerType, r.asserts_total as assertsTotal, r.asserts_passed as assertsPassed, r.asserts_failed as assertsFailed, " +
            "         1 as totalCases, CASE WHEN r.status = 'success' THEN 1 ELSE 0 END as passedCases, CASE WHEN r.status = 'failed' THEN 1 ELSE 0 END as failedCases " +
            "  FROM test_reports r " +
            "  LEFT JOIN test_cases c ON r.case_id = c.id " +
            "  WHERE r.plan_id IS NULL AND r.is_deleted = 0 " +
            "  <if test='status != null and status != \"all\"'> AND r.status = #{status} </if> " +
            "  <if test='keyword != null and keyword != \"\"'> AND (r.logs LIKE CONCAT('%',#{keyword},'%') OR c.name LIKE CONCAT('%',#{keyword},'%')) </if> " +
            "  <if test='date != null and date != \"\"'> AND DATE(r.executed_at) = #{date} </if> " +
            "  UNION ALL " +
            "  SELECT MIN(r.id) as id, r.project_id as projectId, r.plan_id as planId, r.plan_run_no as planRunNo, NULL as caseId, " +
            "         CASE WHEN SUM(CASE WHEN r.status = 'running' THEN 1 ELSE 0 END) > 0 THEN 'running' " +
            "              WHEN SUM(CASE WHEN r.status = 'failed' THEN 1 ELSE 0 END) > 0 THEN 'failed' " +
            "              ELSE 'success' END as status, " +
            "         SUM(r.execution_time) as executionTime, MAX(r.executed_at) as executedAt, MAX(r.executed_by) as executedBy, r.trigger_type as triggerType, " +
            "         SUM(r.asserts_total) as assertsTotal, SUM(r.asserts_passed) as assertsPassed, SUM(r.asserts_failed) as assertsFailed, " +
            "         COUNT(*) as totalCases, SUM(CASE WHEN r.status = 'success' THEN 1 ELSE 0 END) as passedCases, SUM(CASE WHEN r.status = 'failed' THEN 1 ELSE 0 END) as failedCases " +
            "  FROM test_reports r " +
            "  LEFT JOIN test_plans p ON r.plan_id = p.id " +
            "  WHERE r.plan_id IS NOT NULL AND r.is_deleted = 0 " +
            "  <if test='keyword != null and keyword != \"\"'> AND (r.logs LIKE CONCAT('%',#{keyword},'%') OR p.name LIKE CONCAT('%',#{keyword},'%')) </if> " +
            "  <if test='date != null and date != \"\"'> AND DATE(r.executed_at) = #{date} </if> " +
            "  GROUP BY r.project_id, r.plan_id, r.plan_run_no, r.trigger_type " +
            "  <if test='status != null and status != \"all\"'> HAVING status = #{status} </if> " +
            ") AS unified " +
            "WHERE projectId = #{projectId} " +
            "ORDER BY executedAt DESC" +
            "</script>")
    IPage<TestReport> selectUnifiedReports(Page<TestReport> page, @Param("projectId") Integer projectId, @Param("status") String status, @Param("keyword") String keyword, @Param("date") String date);
}
