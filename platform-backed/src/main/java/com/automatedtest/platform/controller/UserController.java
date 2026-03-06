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
import java.util.List;
import java.util.Map;

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
        if (dto.getNotifyRule() != null) user.setNotificationRule(dto.getNotifyRule());
        if (dto.getNotifyThreshold() != null) user.setNotificationThreshold(dto.getNotifyThreshold());

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

    @GetMapping("/pending")
    public Result<IPage<User>> listPendingUsers(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                HttpServletRequest request) {
        User current = getCurrentUser(request);
        if (current == null || current.getRole() == null || !"admin".equalsIgnoreCase(current.getRole())) {
            return Result.error("无权限");
        }
        Page<User> pageParam = new Page<>(page, size);
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("role", "pending");
        query.orderByDesc("created_at");
        IPage<User> result = userService.page(pageParam, query);
        // Hide password for safety
        if (result.getRecords() != null) {
            result.getRecords().forEach(u -> u.setPassword(null));
        }
        return Result.success(result);
    }

    @PutMapping("/{id}/approve")
    @OperationAudit(module = "User", operation = "Approve User")
    public Result<Boolean> approveUser(@PathVariable Long id, HttpServletRequest request) {
        User current = getCurrentUser(request);
        if (current == null || current.getRole() == null || !"admin".equalsIgnoreCase(current.getRole())) {
            return Result.error("无权限");
        }
        User target = userService.getById(id);
        if (target == null) return Result.error("用户不存在");
        target.setRole("user");
        return Result.success(userService.updateById(target));
    }

    @PutMapping("/{id}/reject")
    @OperationAudit(module = "User", operation = "Reject User")
    public Result<Boolean> rejectUser(@PathVariable Long id, HttpServletRequest request) {
        User current = getCurrentUser(request);
        if (current == null || current.getRole() == null || !"admin".equalsIgnoreCase(current.getRole())) {
            return Result.error("无权限");
        }
        User target = userService.getById(id);
        if (target == null) return Result.error("用户不存在");
        target.setRole("disabled");
        return Result.success(userService.updateById(target));
    }

    @PutMapping("/approve-batch")
    @OperationAudit(module = "User", operation = "Approve Users Batch")
    public Result<Boolean> approveUsersBatch(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User current = getCurrentUser(request);
        if (current == null || current.getRole() == null || !"admin".equalsIgnoreCase(current.getRole())) {
            return Result.error("无权限");
        }
        Object idsObj = body != null ? body.get("ids") : null;
        if (!(idsObj instanceof List)) {
            return Result.error("参数错误");
        }
        List<?> ids = (List<?>) idsObj;
        if (ids.isEmpty()) return Result.success(true);

        for (Object idObj : ids) {
            if (idObj == null) continue;
            Long id;
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else {
                try {
                    id = Long.parseLong(idObj.toString());
                } catch (Exception ex) {
                    continue;
                }
            }
            User target = userService.getById(id);
            if (target != null) {
                target.setRole("user");
                userService.updateById(target);
            }
        }
        return Result.success(true);
    }

    @PutMapping("/reject-batch")
    @OperationAudit(module = "User", operation = "Reject Users Batch")
    public Result<Boolean> rejectUsersBatch(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        User current = getCurrentUser(request);
        if (current == null || current.getRole() == null || !"admin".equalsIgnoreCase(current.getRole())) {
            return Result.error("无权限");
        }
        Object idsObj = body != null ? body.get("ids") : null;
        if (!(idsObj instanceof List)) {
            return Result.error("参数错误");
        }
        List<?> ids = (List<?>) idsObj;
        if (ids.isEmpty()) return Result.success(true);

        for (Object idObj : ids) {
            if (idObj == null) continue;
            Long id;
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else {
                try {
                    id = Long.parseLong(idObj.toString());
                } catch (Exception ex) {
                    continue;
                }
            }
            User target = userService.getById(id);
            if (target != null) {
                target.setRole("disabled");
                userService.updateById(target);
            }
        }
        return Result.success(true);
    }
}
