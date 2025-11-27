package com.example.chat.common.model;

import lombok.Builder;
import lombok.Data;

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
}