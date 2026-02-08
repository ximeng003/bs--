package com.automatedtest.platform.controller;

import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<User> login(@RequestBody User loginUser) {
        User user = userService.login(loginUser.getUsername(), loginUser.getPassword());
        if (user != null) {
            // For simplicity, we return the user object. In production, use JWT.
            return Result.success(user);
        }
        return Result.error("Invalid username or password");
    }

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        // Simple check if user exists could be added here
        return Result.success(userService.register(user));
    }
}
