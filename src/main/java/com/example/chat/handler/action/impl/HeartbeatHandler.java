package com.example.chat.handler.action.impl;

import com.example.chat.common.packet.WsRequest;
import com.example.chat.handler.action.BaseActionHandler;
import com.example.chat.common.packet.WsResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 心跳处理器
 */
@Component
public class HeartbeatHandler extends BaseActionHandler {
    
    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        // 心跳包只需要简单响应，不需要业务逻辑
        WsResponse response = WsResponse.builder()
                .type("HEARTBEAT_RESP")
                .msg("pong")
                .build();
        sendResponse(session, response);
    }
}
