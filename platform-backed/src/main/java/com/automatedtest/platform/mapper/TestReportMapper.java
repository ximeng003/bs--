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

    @Select("SELECT r.status, r.executed_by as executedBy, r.executed_at as executedAt, c.name as caseName FROM test_reports r LEFT JOIN test_cases c ON r.case_id = c.id ORDER BY r.executed_at DESC LIMIT 10")
    List<Map<String, Object>> getRecentActivity();
}
