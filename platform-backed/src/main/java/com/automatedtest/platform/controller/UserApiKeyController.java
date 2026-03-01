package com.automatedtest.platform.controller;

import com.automatedtest.platform.annotation.OperationAudit;
import com.automatedtest.platform.common.Result;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.entity.UserApiKey;
import com.automatedtest.platform.service.UserApiKeyService;
import com.automatedtest.platform.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/user/api-keys")
@CrossOrigin(origins = "*")
public class UserApiKeyController {

    @Autowired
    private UserApiKeyService userApiKeyService;

    @Autowired
    private UserService userService;

    private User getCurrentUser(HttpServletRequest request) {
        String username = request.getHeader("X-User-Name");
        if (username != null && !username.trim().isEmpty()) {
            return userService.lambdaQuery().eq(User::getUsername, username.trim()).one();
        }
        return null;
    }

    @GetMapping
    public Result<List<UserApiKey>> listKeys(HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null) {
            return Result.error("Unauthorized");
        }
        // status=1 means active
        QueryWrapper<UserApiKey> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user.getId());
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("created_at");
        return Result.success(userApiKeyService.list(queryWrapper));
    }

    @PostMapping
    @OperationAudit(module = "UserApiKey", operation = "Generate API Key")
    public Result<UserApiKey> generateKey(@RequestBody UserApiKey keyRequest, HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null) {
            return Result.error("Unauthorized");
        }
        String name = keyRequest.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "Personal Key";
        }
        UserApiKey newKey = userApiKeyService.generateKey(user.getId(), name);
        return Result.success(newKey);
    }

    @DeleteMapping("/{id}")
    @OperationAudit(module = "UserApiKey", operation = "Revoke API Key")
    public Result<String> revokeKey(@PathVariable Long id, HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null) {
            return Result.error("Unauthorized");
        }
        UserApiKey key = userApiKeyService.getById(id);
        if (key == null || !key.getUserId().equals(user.getId())) {
            return Result.error("Key not found or access denied");
        }
        key.setStatus(0); // Revoke
        userApiKeyService.updateById(key);
        return Result.success("Revoked successfully");
    }
}
