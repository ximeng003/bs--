package com.automatedtest.platform.controller;

import com.automatedtest.platform.annotation.OperationAudit;
import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.dto.ChangePasswordDTO;
import com.automatedtest.platform.dto.UpdateProfileDTO;
import com.automatedtest.platform.entity.LoginLog;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.LoginLogService;
import com.automatedtest.platform.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private LoginLogService loginLogService;

    private User getCurrentUser(HttpServletRequest request) {
        String username = request.getHeader("X-User-Name");
        if (username != null && !username.trim().isEmpty()) {
            return userService.getOne(new QueryWrapper<User>().eq("username", username.trim()));
        }
        return null;
    }

    @GetMapping("/profile")
    public Result<User> getProfile(HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null) {
            return Result.error("未登录或用户不存在");
        }
        user.setPassword(null); // Hide password
        return Result.success(user);
    }

    @PutMapping("/profile")
    @OperationAudit(module = "User", operation = "Update Profile")
    public Result<User> updateProfile(@RequestBody UpdateProfileDTO dto, HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null) {
            return Result.error("未登录或用户不存在");
        }
        
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        if (dto.getNotificationWebhook() != null) user.setNotificationWebhook(dto.getNotificationWebhook());
        if (dto.getEnableNotification() != null) user.setEnableNotification(dto.getEnableNotification());

        userService.updateById(user);
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/password")
    @OperationAudit(module = "User", operation = "Change Password")
    public Result<Boolean> changePassword(@RequestBody ChangePasswordDTO dto, HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null) {
            return Result.error("未登录或用户不存在");
        }
        if (!user.getPassword().equals(dto.getOldPassword())) {
            return Result.error("旧密码错误");
        }
        user.setPassword(dto.getNewPassword());
        return Result.success(userService.updateById(user));
    }

    @GetMapping("/logs")
    public Result<IPage<LoginLog>> getLoginLogs(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null) {
            return Result.error("未登录或用户不存在");
        }
        Page<LoginLog> pageParam = new Page<>(page, size);
        QueryWrapper<LoginLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user.getId());
        queryWrapper.orderByDesc("login_time");
        return Result.success(loginLogService.page(pageParam, queryWrapper));
    }
}
