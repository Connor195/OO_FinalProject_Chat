package com.example.chat.handler.action.impl;

import com.example.chat.common.packet.WsRequest;
import com.example.chat.common.model.Message;
import com.example.chat.handler.action.BaseActionHandler;
import com.example.chat.repository.DataCenter;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 获取历史消息处理器
 */
@Component
public class GetHistoryHandler extends BaseActionHandler {
    
    Long beforeTime = null;
    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        try {
            String currentUser = getCurrentUser(session);
            if (currentUser == null) {
                sendError(session, "请先登录");
                return;
            }
            
            JsonNode params = request.getParams();
            
            if (params != null && params.has("beforeTime")) {
                beforeTime = params.get("beforeTime").asLong();
            }
            
            // 获取历史消息，按时间戳倒序排序
            List<Message> allMessages = DataCenter.MSG_HISTORY.values().stream()
                    .sorted(Comparator.comparing(Message::getTimestamp).reversed())
                    .collect(Collectors.toList());
            
            // 如果有 beforeTime，过滤出更早的消息
            if (beforeTime != null) {
                allMessages = allMessages.stream()
                        .filter(msg -> msg.getTimestamp() < beforeTime)
                        .collect(Collectors.toList());
            }
            
            // 只取最近20条
            List<Message> result = allMessages.stream()
                    .limit(20)
                    .collect(Collectors.toList());
            
            // 恢复时间顺序
            result.sort(Comparator.comparing(Message::getTimestamp));
            
            sendSuccess(session, result);
            
        } catch (Exception e) {
            sendError(session, "获取历史消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
