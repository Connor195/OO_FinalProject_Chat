package com.example.chat.service;

import com.example.chat.common.model.User;
import com.example.chat.common.model.Group;
import java.util.List;

public interface UserService {
    User login(String username, String avatar);
    Group createGroup(String groupName, String owner, List<String> initialMembers);
}