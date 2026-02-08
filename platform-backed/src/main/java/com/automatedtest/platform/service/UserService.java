package com.automatedtest.platform.service;

import com.automatedtest.platform.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    User login(String username, String password);
    User register(User user);
}
