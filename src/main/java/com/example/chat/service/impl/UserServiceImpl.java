package com.example.chat.service.impl;

import com.example.chat.common.model.Group;
import com.example.chat.common.model.User;
import com.example.chat.repository.DataCenter;
import com.example.chat.service.UserService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class UserServiceImpl implements UserService {
    @Override
    public User login(String userId, String password) {
        User user = DataCenter.USERS.get(userId);

        if (user == null) {
            // 新用户注册
            user = new User(userId);
            user.setPassword(password);

            // --- 核心修改: 钦定管理员 ---
            if ("admin".equals(userId)) { // 这里可以改你自己喜欢的名字
                user.setRole("ADMIN");
            }
            // ------------------------

            DataCenter.USERS.put(userId, user);
            return user;
        } else {
            // 老用户验密... (略)
            if (!user.getPassword().equals(password)) {
                throw new IllegalArgumentException("密码错误");
            }
            return user;
        }
    }
    @Override
    public void logout(String userId) {
        if (userId != null) {
            DataCenter.ONLINE_USERS.remove(userId);
        }
    }
    @Override
    public Group createGroup(String groupName, String owner, List<String> initialMembers) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("群名称不能为空");
        }
        if (owner == null || owner.isEmpty()) {
            throw new IllegalArgumentException("群主不能为空");
        }

        // 生成唯一群ID（UUID）
        String groupId = java.util.UUID.randomUUID().toString();

        Group group = new Group();
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setOwner(owner);

        // 初始化成员列表
        List<String> members = new CopyOnWriteArrayList<>();
        members.add(owner); // 群主必须加入成员列表
        if (initialMembers != null) {
            for (String member : initialMembers) {
                // 过滤空值、重复的用户名（简单判断）
                if (member != null && !member.isEmpty() && !members.contains(member)) {
                    members.add(member);
                }
            }
        }
        group.setMembers(members);

        // 保存到内存群组集合
        DataCenter.GROUPS.put(groupId, group);

        return group;
    }
    @Override
    public void updateAvatar(String userId, String newAvatar) {
        User user = DataCenter.USERS.get(userId);
        if (user != null) {
            user.setAvatar(newAvatar);
        } else {
            throw new IllegalArgumentException("用户不存在");
        }
    }
    @Override
    public void updateUsername(String userId, String newUsername) {
        User user = DataCenter.USERS.get(userId);
        if (user != null) {
            user.setUsername(newUsername);
        } else {
            throw new IllegalArgumentException("用户不存在");
        }
    }
    @Override
    public boolean sendFriendRequest(String fromUser, String toUser) {
        if (fromUser == null || toUser == null || fromUser.equals(toUser)) return false;

        User from = DataCenter.USERS.get(fromUser);
        User to = DataCenter.USERS.get(toUser);
        if (from == null || to == null) return false;

        if (from.getFriends().contains(toUser)) {
            return false; // 已经是好友了
        }

        // 如果已经发过申请，不重复发
        if (to.getFriendRequests().contains(fromUser)) {
            return false;
        }

        to.getFriendRequests().add(fromUser);
        return true;
    }
    @Override
    public boolean acceptFriendRequest(String toUser, String fromUser) {
        if (toUser == null || fromUser == null) return false;

        User to = DataCenter.USERS.get(toUser);
        User from = DataCenter.USERS.get(fromUser);
        if (to == null || from == null) return false;

        // 确认请求存在
        if (!to.getFriendRequests().contains(fromUser)) {
            return false;
        }

        // 双向添加好友
        to.getFriends().add(fromUser);
        from.getFriends().add(toUser);

        // 移除申请
        to.getFriendRequests().remove(fromUser);
        return true;
    }
    @Override
    public boolean rejectFriendRequest(String toUser, String fromUser) {
        if (toUser == null || fromUser == null) return false;

        User to = DataCenter.USERS.get(toUser);
        if (to == null) return false;

        return to.getFriendRequests().remove(fromUser);
    }
}