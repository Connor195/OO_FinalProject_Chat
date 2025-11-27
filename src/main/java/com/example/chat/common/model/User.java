package com.example.chat.common.model;

import lombok.Data;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data // Lombok 注解：自动生成 Getter/Setter/ToString
public class User {
    private String username;
    private String avatar;

    // 使用线程安全的 Set 存储好友和屏蔽列表
    private Set<String> friends = ConcurrentHashMap.newKeySet();
    private Set<String> blockList = ConcurrentHashMap.newKeySet();

    public User(String username) {
        this.username = username;
    }
}
