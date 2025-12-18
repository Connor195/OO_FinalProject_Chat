package com.example.chat.handler.action.impl;

import com.example.chat.common.packet.WsRequest;
import com.example.chat.handler.action.BaseActionHandler;
import com.example.chat.repository.DataCenter;
import com.example.chat.common.packet.WsResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 获取在线用户列表处理器
 */
@Component
public class GetOnlineHandler extends BaseActionHandler {
    
    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        try {
            String currentUser = getCurrentUser(session);
            if (currentUser == null) {
                sendError(session, "请先登录");
                return;
            }
            
            List<Map<String, Object>> onlineList = new ArrayList<>();
            
            for (Map.Entry<String, WebSocketSession> entry : DataCenter.ONLINE_USERS.entrySet()) {
                String username = entry.getKey();
                com.example.chat.common.model.User user = DataCenter.USERS.get(username);
                
                if (user != null) {
                    onlineList.add(Map.of(
                            "username", username,
                            "role", user.getRole(),
                            "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                            "muted", user.isMuted()
                    ));
                }
            }
            
            // 发送在线列表
            WsResponse response = WsResponse.builder()
                    .type("ONLINE_LIST")
                    .data(onlineList)
                    .build();
            
            sendResponse(session, response);
            
        } catch (Exception e) {
            sendError(session, "获取在线列表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
