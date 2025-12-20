package com.example.chat.service.impl;

import com.example.chat.common.model.Message;
import com.example.chat.repository.DataCenter;
import com.example.chat.service.MessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {
    
    // 撤回时间限制：2分钟（毫秒）
    private static final long RECALL_TIME_LIMIT = 2 * 60 * 1000;
    
    @Override
    public Message processAndSaveMsg(String fromUser, String toUser, String content, 
                                   boolean isGroup, List<String> atUsers) {
        // 生成唯一ID
        String msgId = java.util.UUID.randomUUID().toString();

        Message msg = Message.builder()
                .msgId(msgId)
                .fromUser(fromUser)
                .toUser(toUser)
                .content(content)
                .isGroup(isGroup)
                .timestamp(System.currentTimeMillis())
                .atUsers(atUsers)
                .build();

        // 存入历史记录 (DataCenter)
        DataCenter.MSG_HISTORY.put(msgId, msg);

        return msg;
    }

    @Override
    public boolean recallMessage(String msgId, String operator) {
        Message message = DataCenter.MSG_HISTORY.get(msgId);
        
        if (message == null) {
            return false; // 消息不存在
        }
        
        // 检查操作者是否有权限撤回
        if (!canRecallMessage(message, operator)) {
            return false; // 无权限撤回
        }
        
        // 检查是否在2分钟内
        long currentTime = System.currentTimeMillis();
        long messageTime = message.getTimestamp();
        
        if (currentTime - messageTime > RECALL_TIME_LIMIT) {
            return false; // 超过2分钟，无法撤回
        }
        
        // 从历史记录中移除消息（实现"删除"效果）
        DataCenter.MSG_HISTORY.remove(msgId);
        
        return true;
    }
    
    /**
     * 检查操作者是否有权限撤回消息
     * 规则：
     * 1. 消息发送者可以撤回自己的消息
     * 2. 管理员可以撤回任何消息
     * 3. 群主可以撤回群内消息
     */
    private boolean canRecallMessage(Message message, String operator) {
        // 消息发送者可以撤回
        if (message.getFromUser().equals(operator)) {
            return true;
        }
        
        // 检查操作者是否是管理员
        com.example.chat.common.model.User user = DataCenter.USERS.get(operator);
        if (user != null && user.isAdmin()) {
            return true;
        }
        
        // 如果是群消息，检查是否是群主
        if (message.isGroup()) {
            com.example.chat.common.model.Group group = 
                DataCenter.GROUPS.get(message.getToUser());
            if (group != null && group.getOwner().equals(operator)) {
                return true;
            }
        }
        
        return false;
    }
}
