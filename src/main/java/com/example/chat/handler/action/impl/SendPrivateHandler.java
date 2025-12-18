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
 * 发送私聊消息处理器
 */
@Component
public class SendPrivateHandler extends BaseActionHandler {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
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
            
            String toUser = params.get("targetUser").asText();
            String content = params.get("content").asText().trim();
            
            if (content.isEmpty()) {
                sendError(session, "消息内容不能为空");
                return;
            }
            
            // 检查目标用户是否在线
            if (!DataCenter.ONLINE_USERS.containsKey(toUser)) {
                sendError(session, "用户 " + toUser + " 不在线");
                return;
            }
            
            // 提取 @ 列表
            List<String> atUsers = new ArrayList<>();
            if (params.has("atUsers") && params.get("atUsers").isArray()) {
                for (JsonNode atUser : params.get("atUsers")) {
                    atUsers.add(atUser.asText());
                }
            }
            
            // 创建并保存消息
            Message message = messageService.processAndSaveMsg(fromUser, toUser, content, false, atUsers);
            
            // 发送给接收者
            sendToUser(toUser, message);
            
            // 发送回执给发送者
            sendSuccess(session, message);
            
            System.out.println("私聊消息: " + fromUser + " -> " + toUser + ": " + content);
            
        } catch (Exception e) {
            sendError(session, "发送失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送消息给指定用户
     */
    private void sendToUser(String username, Message message) {
        WebSocketSession targetSession = DataCenter.ONLINE_USERS.get(username);
        if (targetSession != null && targetSession.isOpen()) {
            try {
                WsResponse response = WsResponse.builder()
                        .type("EVENT_CHAT_MSG")
                        .data(message)
                        .build();
                String json = objectMapper.writeValueAsString(response);
                targetSession.sendMessage(new org.springframework.web.socket.TextMessage(json));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
