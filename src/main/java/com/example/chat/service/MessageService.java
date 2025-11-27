package com.example.chat.service;

import com.example.chat.common.model.Message;

public interface MessageService {
    // 修改这个方法的签名，多加一个 atUsers 参数
    Message processAndSaveMsg(String fromUser, String toUser, String content, boolean isGroup, java.util.List<String> atUsers);
    boolean recallMessage(String msgId, String operator);
}