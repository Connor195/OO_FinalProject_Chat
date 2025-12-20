package com.example.chat.handler.action;

import com.example.chat.common.packet.WsResponse;
import com.example.chat.repository.DataCenter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * 所有 ActionHandler 的基类，提供公共方法
 */
public abstract class BaseActionHandler implements ActionHandler {
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    /**
     * 发送响应给客户端
     */
    protected void sendResponse(WebSocketSession session, WsResponse response) {
        if (session == null || !session.isOpen()) return;
        try {
            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 发送错误信息
     */
    protected void sendError(WebSocketSession session, String errorMsg) {
        sendResponse(session, WsResponse.error(errorMsg));
    }
    
    /**
     * 发送成功响应
     */
    protected void sendSuccess(WebSocketSession session, Object data) {
        sendResponse(session, WsResponse.builder()
                .type("SUCCESS")
                .data(data)
                .build());
    }
    
    /**
     * 获取当前登录的用户名
     */
    protected String getCurrentUser(WebSocketSession session) {
        return (String) session.getAttributes().get("username");
    }
    
    /**
     * 检查用户是否在线
     */
    protected boolean isUserOnline(String username) {
        return DataCenter.ONLINE_USERS.containsKey(username);
    }
    
    /**
     * 检查是否是管理员
     */
    protected boolean isAdmin(String username) {
        com.example.chat.common.model.User user = DataCenter.USERS.get(username);
        return user != null && user.isAdmin();
    }
}
