package com.example.chat.handler.action.impl;

import com.example.chat.common.packet.WsRequest;
import com.example.chat.handler.action.BaseActionHandler;
import com.example.chat.repository.DataCenter;
import com.example.chat.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 登录处理器
 */
@Component
public class LoginHandler extends BaseActionHandler {
    
    @Autowired
    private UserService userService;
    
    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        try {
            JsonNode params = request.getParams();
            if (params == null || !params.has("username")) {
                sendError(session, "参数错误：缺少 username");
                return;
            }
            
            String username = params.get("username").asText().trim();
            String password = params.has("password") ? params.get("password").asText() : "";
            String avatar = params.has("avatar") ? params.get("avatar").asText() : "";
            
            if (username.isEmpty()) {
                sendError(session, "用户名不能为空");
                return;
            }
            
            // 1. 检查是否已在其他设备登录（踢下线）
            WebSocketSession oldSession = DataCenter.ONLINE_USERS.get(username);
            if (oldSession != null && oldSession.isOpen()) {
                try {
                    oldSession.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                DataCenter.ONLINE_USERS.remove(username);
                System.out.println("用户 " + username + " 被踢下线");
            }
            
            // 2. 登录或注册
            com.example.chat.common.model.User user = userService.login(username, avatar);
            
            // 3. 保存会话
            session.getAttributes().put("username", username);
            DataCenter.ONLINE_USERS.put(username, session);
            
            // 4. 发送登录成功响应
            sendSuccess(session, user);
            
            System.out.println("用户 " + username + " 登录成功");
            
        } catch (Exception e) {
            sendError(session, "登录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
