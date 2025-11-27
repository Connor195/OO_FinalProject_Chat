package com.example.chat.service.impl;

import com.example.chat.common.model.Group;
import com.example.chat.common.model.User;
import com.example.chat.service.UserService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Override
    public User login(String username, String avatar) {
        // 先写个假的，返回一个空用户，防止报错
        return new User(username);
    }

    @Override
    public Group createGroup(String groupName, String owner, List<String> initialMembers) {
        return null; // 暂时返回 null
    }
}