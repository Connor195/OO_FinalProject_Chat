package com.example.chat.service.impl;

import com.example.chat.common.model.Message;
import com.example.chat.service.MessageService;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl implements MessageService {
    @Override
    public Message processAndSaveMsg(String fromUser, String toUser, String content, boolean isGroup) {
        // 先简单把消息原样构建出来返回，不做存储
        return Message.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .content(content)
                .isGroup(isGroup)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Override
    public boolean recallMessage(String msgId, String operator) {
        return true; // 假装撤回成功
    }
}