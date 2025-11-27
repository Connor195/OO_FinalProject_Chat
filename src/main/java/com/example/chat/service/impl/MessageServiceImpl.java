package com.example.chat.service.impl;

import com.example.chat.common.model.Message;
import com.example.chat.service.MessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {
    @Override
    public Message processAndSaveMsg(String fromUser, String toUser, String content, boolean isGroup, List<String> atUsers) {
        // 生成唯一ID
        String msgId = java.util.UUID.randomUUID().toString();

        Message msg = Message.builder()
                .msgId(msgId)
                .fromUser(fromUser)
                .toUser(toUser)
                .content(content)
                .isGroup(isGroup)
                .timestamp(System.currentTimeMillis())
                .atUsers(atUsers) // --- 新增：保存 @ 列表 ---
                .build();

        // 存入历史记录 (DataCenter)
        com.example.chat.repository.DataCenter.MSG_HISTORY.put(msgId, msg);

        return msg;
    }

    @Override
    public boolean recallMessage(String msgId, String operator) {
        return true; // 假装撤回成功
    }
}