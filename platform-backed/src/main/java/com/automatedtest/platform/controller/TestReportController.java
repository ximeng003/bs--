package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.entity.TestReport;
import com.automatedtest.platform.service.TestReportService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class TestReportController {

    @Autowired
    private TestReportService testReportService;

    @GetMapping
    public Result<IPage<TestReport>> list(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String date) { // Simplified keyword search
        Page<TestReport> pageParam = new Page<>(page, size);
        QueryWrapper<TestReport> queryWrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            queryWrapper.eq("status", status);
        }
        
        // Keyword search might need joins with Plan/Case names, but for now we just search ID or logs
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("logs", keyword).or().eq("id", keyword);
        }

        if (StringUtils.hasText(date)) {
            try {
                LocalDate localDate = LocalDate.parse(date);
                LocalDateTime start = localDate.atStartOfDay();
                LocalDateTime end = localDate.plusDays(1).atStartOfDay();
                queryWrapper.ge("executed_at", start);
                queryWrapper.lt("executed_at", end);
            } catch (Exception ignored) {
            }
        }
        
        queryWrapper.orderByDesc("executed_at");
        
        return Result.success(testReportService.page(pageParam, queryWrapper));
    }

    @GetMapping("/{id}")
    public Result<TestReport> getById(@PathVariable Integer id) {
        return Result.success(testReportService.getById(id));
    }
    
    // Create report is usually done internally by execution engine, but expose it for now
    @PostMapping
    public Result<Boolean> save(@RequestBody TestReport testReport) {
        return Result.success(testReportService.save(testReport));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return Result.success(testReportService.removeById(id));
    }

    @DeleteMapping
    public Result<Boolean> deleteBatch(@RequestBody(required = false) List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            QueryWrapper<TestReport> queryWrapper = new QueryWrapper<>();
            return Result.success(testReportService.remove(queryWrapper));
        }
        return Result.success(testReportService.removeBatchByIds(ids));
    }
}
