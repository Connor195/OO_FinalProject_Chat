package com.example.chat.handler;

import com.example.chat.common.packet.WsRequest;
import com.example.chat.common.packet.WsResponse;
import com.example.chat.handler.action.ActionHandler;
import com.example.chat.repository.DataCenter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * Refactored WebSocket 主处理器.
 *
 * 这个类的职责被大大简化，现在只负责：
 * 1. 管理 WebSocket 的生命周期 (连接建立/关闭).
 * 2. 解析传入的文本消息.
 * 3. 执行中央认证检查.
 * 4. 使用 HandlerRegistry 将消息路由到对应的 ActionHandler (策略模式).
 */
@Component
public class ChatHandler extends TextWebSocketHandler {

    @Autowired
    private HandlerRegistry handlerRegistry;

    @Autowired
    private ObjectMapper jsonMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String ip = (String) session.getAttributes().get("clientIp");
        System.out.println("新连接接入: " + session.getId() + ", IP: " + ip);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            DataCenter.ONLINE_USERS.remove(username);
            System.out.println("用户下线: " + username + ", Status: " + status);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            WsRequest request = jsonMapper.readValue(message.getPayload(), WsRequest.class);
            String action = request.getAction();

            if (action == null) {
                sendError(session, "无效的指令: Action 为空");
                return;
            }

            // 中央认证检查：除了登录和心跳，其他所有操作都需要认证
            if (!"LOGIN".equals(action) && !"HEARTBEAT".equals(action)) {
                if (session.getAttributes().get("username") == null) {
                    sendError(session, "请先登录 (LOGIN)！");
                    return;
                }
            }

            // 从注册表查找对应的处理器
            ActionHandler handler = handlerRegistry.getHandler(action);

            if (handler != null) {
                // 找到处理器，执行 handle 方法
                handler.handle(session, request);
            } else {
                // 未找到处理器
                sendError(session, "未知指令: " + action);
            }
        } catch (IOException e) {
            e.printStackTrace();
            sendError(session, "JSON 格式错误");
        }
    }

    /**
     * 内部辅助方法，用于发送错误信息。
     * 因为此类不再继承 BaseActionHandler，所以需要一个自己的发送方法。
     */
    private void sendError(WebSocketSession session, String errorMsg) {
        if (session == null || !session.isOpen()) return;
        try {
            WsResponse response = WsResponse.error(errorMsg);
            String json = jsonMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
