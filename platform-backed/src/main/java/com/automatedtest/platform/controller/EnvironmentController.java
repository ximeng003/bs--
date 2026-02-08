package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.entity.Environment;
import com.automatedtest.platform.service.EnvironmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/environments")
public class EnvironmentController {

    @Autowired
    private EnvironmentService environmentService;

    @GetMapping
    public Result<List<Environment>> list() {
        return Result.success(environmentService.list());
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Environment environment) {
        return Result.success(environmentService.save(environment));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Environment environment) {
        return Result.success(environmentService.updateById(environment));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return Result.success(environmentService.removeById(id));
    }
}
