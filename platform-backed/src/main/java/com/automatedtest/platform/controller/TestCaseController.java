package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.dto.ApiTestRequestDTO;
import com.automatedtest.platform.dto.ApiTestResponseDTO;
import com.automatedtest.platform.dto.CaseExecuteResultDTO;
import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.service.TestCaseService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/testcases")
public class TestCaseController {

    @Autowired
    private TestCaseService testCaseService;

    @PostMapping("/execute")
    public Result<ApiTestResponseDTO> execute(@RequestBody ApiTestRequestDTO request) {
        return Result.success(testCaseService.executeApiTest(request));
    }
    
    @PostMapping("/{id}/execute")
    public Result<CaseExecuteResultDTO> executeById(@PathVariable Integer id) {
        return Result.success(testCaseService.executeCaseById(id));
    }

    @GetMapping
    public Result<IPage<TestCase>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(required = false, defaultValue = "updated") String sort) {
        Page<TestCase> pageParam = new Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TestCase> qw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.like("name", keyword.trim());
        }
        if (type != null && !type.trim().isEmpty()) {
            qw.eq("type", type.trim());
        }
        if ("created".equalsIgnoreCase(sort)) {
            qw.orderByDesc("created_at");
        } else {
            qw.orderByDesc("updated_at");
        }
        return Result.success(testCaseService.page(pageParam, qw));
    }

    @GetMapping("/{id}")
    public Result<TestCase> getById(@PathVariable Integer id) {
        return Result.success(testCaseService.getById(id));
    }

    @PostMapping
    public Result<Integer> save(@RequestBody TestCase testCase) {
        boolean success = testCaseService.save(testCase);
        if (success) {
            return Result.success(testCase.getId());
        }
        return Result.error("Save failed");
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody TestCase testCase) {
        return Result.success(testCaseService.updateById(testCase));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return Result.success(testCaseService.removeById(id));
    }

    @DeleteMapping
    public Result<Boolean> deleteAll() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TestCase> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        return Result.success(testCaseService.remove(qw));
    }
}
