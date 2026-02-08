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

@RestController
@RequestMapping("/api/reports")
public class TestReportController {

    @Autowired
    private TestReportService testReportService;

    @GetMapping
    public Result<IPage<TestReport>> list(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String keyword) { // Simplified keyword search
        Page<TestReport> pageParam = new Page<>(page, size);
        QueryWrapper<TestReport> queryWrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            queryWrapper.eq("status", status);
        }
        
        // Keyword search might need joins with Plan/Case names, but for now we just search ID or logs
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("logs", keyword).or().eq("id", keyword);
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
}
