package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.entity.TestPlan;
import com.automatedtest.platform.service.TestPlanService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/plans")
public class TestPlanController {

    @Autowired
    private TestPlanService testPlanService;

    @GetMapping
    public Result<IPage<TestPlan>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size) {
        Page<TestPlan> pageParam = new Page<>(page, size);
        return Result.success(testPlanService.page(pageParam));
    }

    @GetMapping("/{id}")
    public Result<TestPlan> getById(@PathVariable Integer id) {
        return Result.success(testPlanService.getById(id));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody TestPlan testPlan) {
        return Result.success(testPlanService.save(testPlan));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody TestPlan testPlan) {
        return Result.success(testPlanService.updateById(testPlan));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return Result.success(testPlanService.removeById(id));
    }
}
