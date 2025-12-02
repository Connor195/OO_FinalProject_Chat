package com.example.chat.service;

import com.example.chat.common.model.User;
import com.example.chat.common.model.Group;
import java.util.List;

public interface UserService {
    User login(String userId, String avatar);
    void logout(String userId);
    Group createGroup(String groupName, String owner, List<String> initialMembers);
    void updateAvatar(String userId, String newAvatar);
    void updateUsername(String userId, String newUsername);

    // 发送好友申请
    boolean sendFriendRequest(String fromUser, String toUser);

    // 接受好友申请
    boolean acceptFriendRequest(String fromUser, String toUser);

    boolean rejectFriendRequest(String fromUser, String toUser);
}