package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.dto.ApiTestRequestDTO;
import com.automatedtest.platform.dto.ApiTestResponseDTO;
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

    @GetMapping
    public Result<IPage<TestCase>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size) {
        Page<TestCase> pageParam = new Page<>(page, size);
        return Result.success(testCaseService.page(pageParam));
    }

    @GetMapping("/{id}")
    public Result<TestCase> getById(@PathVariable Integer id) {
        return Result.success(testCaseService.getById(id));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody TestCase testCase) {
        return Result.success(testCaseService.save(testCase));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody TestCase testCase) {
        return Result.success(testCaseService.updateById(testCase));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return Result.success(testCaseService.removeById(id));
    }
}
