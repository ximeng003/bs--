package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.automatedtest.platform.service.LoginLogService;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private LoginLogService loginLogService;

    @PostMapping("/login")
    public Result<User> login(@RequestBody User loginUser, HttpServletRequest request) {
        User user = userService.login(loginUser.getUsername(), loginUser.getPassword());
        String ip = request.getRemoteAddr();
        
        if (user != null) {
            // Record success
            loginLogService.recordLogin(user.getId(), user.getUsername(), ip, "success");
            
            // Update user last login info
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ip);
            userService.updateById(user);
            
            return Result.success(user);
        } else {
            // Record failure (if we knew the username, but here we might only have what was submitted)
            // Ideally we'd look up the user by username to get ID, but if login fails, maybe user doesn't exist.
            // For now, just log if username is provided.
            if (loginUser.getUsername() != null) {
                // Try to find user to get ID, or log with ID 0
                // For simplicity, just skip logging failed attempts without valid user for now or implement later
            }
        }
        return Result.error("用户名或密码错误");
    }

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        // Simple check if user exists could be added here
        return Result.success(userService.register(user));
    }
}
