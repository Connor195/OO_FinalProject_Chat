package com.example.chat.common.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder // Lombok 注解：允许使用 .builder().build() 方式快速构建对象
public class Message {
    private String msgId;
    private String fromUser;
    private String toUser;     // 私聊是用户名，群聊是GroupId
    private boolean isGroup;   // 是否群聊
    private String content;
    private Long timestamp;

    // 引用回复
    private String quoteId;
    private String quoteContent;

    // 1. 被 @ 的用户列表 (存用户名)
    private List<String> atUsers;

    // 2. 已读列表 (存用户名，使用线程安全的 Set)
    // @Builder.Default 防止 Builder 模式覆盖掉初始化值
    @Builder.Default
    private Set<String> readBy = ConcurrentHashMap.newKeySet();
}