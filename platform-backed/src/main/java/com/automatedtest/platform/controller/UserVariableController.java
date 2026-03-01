package com.automatedtest.platform.controller;

import com.automatedtest.platform.annotation.OperationAudit;
import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.common.UserContext;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.entity.UserVariable;
import com.automatedtest.platform.service.UserService;
import com.automatedtest.platform.service.UserVariableService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/user/variables")
public class UserVariableController {

    @Autowired
    private UserVariableService userVariableService;

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<IPage<UserVariable>> list(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size) {
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        Page<UserVariable> pageParam = new Page<>(page, size);
        QueryWrapper<UserVariable> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user.getId());
        queryWrapper.orderByDesc("updated_at");
        return Result.success(userVariableService.page(pageParam, queryWrapper));
    }

    @PostMapping
    @OperationAudit(module = "UserVariable", operation = "Create User Variable")
    public Result<Boolean> create(@RequestBody UserVariable variable) {
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        variable.setUserId(user.getId());
        variable.setCreatedAt(LocalDateTime.now());
        variable.setUpdatedAt(LocalDateTime.now());
        
        // check if key exists
        long count = userVariableService.count(new QueryWrapper<UserVariable>()
                .eq("user_id", user.getId())
                .eq("key_name", variable.getKeyName()));
        if (count > 0) return Result.error("变量名已存在");
        
        return Result.success(userVariableService.save(variable));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody UserVariable variable) {
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        UserVariable existing = userVariableService.getById(variable.getId());
        if (existing == null) return Result.error("变量不存在");
        if (!existing.getUserId().equals(user.getId())) return Result.error("无权修改");
        
        existing.setValue(variable.getValue());
        existing.setDescription(variable.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        return Result.success(userVariableService.updateById(existing));
    }

    @DeleteMapping("/{id}")
    @OperationAudit(module = "UserVariable", operation = "Delete User Variable")
    public Result<Boolean> delete(@PathVariable Long id) {
        User user = UserContext.getCurrentUser();
        if (user == null) return Result.error("未登录");
        
        UserVariable existing = userVariableService.getById(id);
        if (existing == null) return Result.success(true);
        if (!existing.getUserId().equals(user.getId())) return Result.error("无权删除");
        
        return Result.success(userVariableService.removeById(id));
    }
}
