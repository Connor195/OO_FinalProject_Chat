package com.example.chat.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore; // 务必引入这个！
import lombok.Data;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class User {
    private String userId;
    private String username;
    private String gender;// man woman unknown?
    private int age;



    //  加 @JsonIgnore，这样 User 对象发给前端时，password 字段会自动消失，防止泄露！
    @JsonIgnore
    private String password;

    private String avatar;

    // 新增角色 (ADMIN / USER)
    private String role = "USER";

    // 禁言结束时间戳 (0 表示没被禁言)
    private long muteEndTime = 0;

    // 原有的好友/屏蔽列表
    private Set<String> friends = ConcurrentHashMap.newKeySet();
    private Set<String> blockList = ConcurrentHashMap.newKeySet();

    // 记录用户名，表示谁向该用户申请加好友
    private Set<String> friendRequests = ConcurrentHashMap.newKeySet();

    public User(String userId) {
        this.userId = userId;
    }

    // 辅助方法：是否是管理员
    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }

    // 辅助方法：是否处于禁言状态
    public boolean isMuted() {
        return System.currentTimeMillis() < this.muteEndTime;
    }


}