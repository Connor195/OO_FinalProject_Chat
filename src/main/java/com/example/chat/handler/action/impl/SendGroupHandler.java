package com.example.chat.handler.action.impl;

import com.example.chat.common.packet.WsRequest;
import com.example.chat.common.model.Message;
import com.example.chat.common.packet.WsResponse;
import com.example.chat.handler.action.BaseActionHandler;
import com.example.chat.repository.DataCenter;
import com.example.chat.service.MessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;

/**
 * 发送群聊消息处理器
 */
@Component
public class SendGroupHandler extends BaseActionHandler {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private ObjectMapper objectMapper;

    // setters
    public void setMessageService(MessageService messageService) {
        this.messageService = messageService;
    }
    
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        try {
            String fromUser = getCurrentUser(session);
            if (fromUser == null) {
                sendError(session, "请先登录");
                return;
            }
            
            JsonNode params = request.getParams();
            if (params == null || !params.has("targetUser") || !params.has("content")) {
                sendError(session, "参数错误：缺少 targetUser 或 content");
                return;
            }
            
            String groupId = params.get("targetUser").asText();
            String content = params.get("content").asText().trim();
            
            if (content.isEmpty()) {
                sendError(session, "消息内容不能为空");
                return;
            }
            
            // 检查群组是否存在
            com.example.chat.common.model.Group group = DataCenter.GROUPS.get(groupId);
            if (group == null) {
                sendError(session, "群组 " + groupId + " 不存在");
                return;
            }
            
            // 检查是否群成员
            if (!group.getMembers().contains(fromUser)) {
                sendError(session, "您不在群组 " + groupId + " 中");
                return;
            }
            
            // 提取 @ 列表
            List<String> atUsers = new ArrayList<>();
            if (params.has("atUsers") && params.get("atUsers").isArray()) {
                for (JsonNode atUser : params.get("atUsers")) {
                    String username = atUser.asText();
                    if (group.getMembers().contains(username)) {
                        atUsers.add(username);
                    }
                }
            }
            
            // 创建并保存消息
            Message message = messageService.processAndSaveMsg(fromUser, groupId, content, true, atUsers);
            
            // 广播给群成员
            broadcastToGroup(group, message, fromUser);
            
            // 发送回执给发送者
            sendSuccess(session, message);
            
            System.out.println("群聊消息: " + fromUser + " -> 群组 " + groupId + ": " + content);
            
        } catch (Exception e) {
            sendError(session, "发送失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 广播消息给群组成员
     */
    private void broadcastToGroup(com.example.chat.common.model.Group group, Message message, String excludeUser) {
        for (String member : group.getMembers()) {
            // 不发送给发送者自己
            if (member.equals(excludeUser)) continue;
            
            WebSocketSession memberSession = DataCenter.ONLINE_USERS.get(member);
            if (memberSession != null && memberSession.isOpen()) {
                try {
                    WsResponse response = WsResponse.builder()
                            .type("EVENT_CHAT_MSG")
                            .data(message)
                            .build();
                    String json = objectMapper.writeValueAsString(response);
                    memberSession.sendMessage(new org.springframework.web.socket.TextMessage(json));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
