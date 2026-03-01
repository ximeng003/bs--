package com.automatedtest.platform.controller;

import com.automatedtest.platform.annotation.OperationAudit;
import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.common.UserContext;
import com.automatedtest.platform.entity.SystemVariable;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.SystemVariableService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/variables")
public class SystemVariableController {

    @Autowired
    private SystemVariableService systemVariableService;

    private boolean isAdmin(User user) {
        return user != null && "admin".equalsIgnoreCase(user.getRole());
    }

    @GetMapping
    public Result<IPage<SystemVariable>> list(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size) {
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        Page<SystemVariable> pageParam = new Page<>(page, size);
        QueryWrapper<SystemVariable> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("updated_at");
        return Result.success(systemVariableService.page(pageParam, queryWrapper));
    }

    @PostMapping
    @OperationAudit(module = "SystemVariable", operation = "Create System Variable")
    public Result<Boolean> create(@RequestBody SystemVariable variable) {
        User user = UserContext.getCurrentUser();
        if (!isAdmin(user)) return Result.error("无权操作");
        
        long count = systemVariableService.count(new QueryWrapper<SystemVariable>()
                .eq("key_name", variable.getKeyName()));
        if (count > 0) return Result.error("变量名已存在");
        
        return Result.success(systemVariableService.save(variable));
    }

    @PutMapping
    @OperationAudit(module = "SystemVariable", operation = "Update System Variable")
    public Result<Boolean> update(@RequestBody SystemVariable variable) {
        User user = UserContext.getCurrentUser();
        if (!isAdmin(user)) return Result.error("无权操作");
        
        return Result.success(systemVariableService.updateById(variable));
    }

    @DeleteMapping("/{id}")
    @OperationAudit(module = "SystemVariable", operation = "Delete System Variable")
    public Result<Boolean> delete(@PathVariable Long id) {
        User user = UserContext.getCurrentUser();
        if (!isAdmin(user)) return Result.error("无权操作");
        
        return Result.success(systemVariableService.removeById(id));
    }
}
