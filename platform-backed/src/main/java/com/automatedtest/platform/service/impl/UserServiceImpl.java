package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.mapper.UserMapper;
import com.automatedtest.platform.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(String username, String password) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        queryWrapper.eq("password", password);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public User register(User user) {
        user.setCreatedAt(LocalDateTime.now());
        if (user.getRole() == null) {
            user.setRole("user");
        }
        baseMapper.insert(user);
        return user;
    }
}
