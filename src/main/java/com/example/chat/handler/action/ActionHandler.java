package com.example.chat.handler.action;

import com.example.chat.common.packet.WsRequest;
import org.springframework.web.socket.WebSocketSession;
/**
 * ActionHandler 接口 - 策略模式的核心接口
 */
public interface ActionHandler {
    /**
     * 处理具体的 WebSocket 请求
     * @param session WebSocket 会话
     * @param request 请求数据
     */
    void handle(WebSocketSession session, WsRequest request);
}