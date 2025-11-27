package com.example.chat.service;

import com.example.chat.common.model.Message;

public interface MessageService {
    Message processAndSaveMsg(String fromUser, String toUser, String content, boolean isGroup);
    boolean recallMessage(String msgId, String operator);
}