package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.entity.OperationLog;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.OperationLogService;
import com.automatedtest.platform.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/audit-logs")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<IPage<OperationLog>> list(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String username,
                                            @RequestParam(required = false) String module,
                                            @RequestParam(required = false) String operation,
                                            HttpServletRequest request) {
        String currentUsername = request.getHeader("X-User-Name");
        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            return Result.error("Unauthorized");
        }
        
        User currentUser = userService.lambdaQuery().eq(User::getUsername, currentUsername.trim()).one();
        if (currentUser == null) {
            return Result.error("User not found");
        }

        // Only admin can view all logs. Regular users can only view their own (or none?).
        // Requirement says "Operation Audit... crucial for traceability". Usually for admins.
        // If regular user requests, enforce username=self.
        
        QueryWrapper<OperationLog> queryWrapper = new QueryWrapper<>();
        
        if (!"admin".equalsIgnoreCase(currentUser.getRole())) {
            // Non-admin can only see their own logs
            queryWrapper.eq("user_id", currentUser.getId());
        } else {
            // Admin can filter by username
            if (username != null && !username.trim().isEmpty()) {
                queryWrapper.like("username", username.trim());
            }
        }

        if (module != null && !module.trim().isEmpty()) {
            queryWrapper.eq("module", module.trim());
        }
        if (operation != null && !operation.trim().isEmpty()) {
            queryWrapper.like("operation", operation.trim());
        }
        
        queryWrapper.orderByDesc("created_at");
        
        Page<OperationLog> pageParam = new Page<>(page, size);
        return Result.success(operationLogService.page(pageParam, queryWrapper));
    }
}
