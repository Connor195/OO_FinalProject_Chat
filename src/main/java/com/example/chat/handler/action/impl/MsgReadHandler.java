package com.example.chat.handler.action.impl;

import com.example.chat.common.packet.WsRequest;
import com.example.chat.common.model.Message;
import com.example.chat.handler.action.BaseActionHandler;
import com.example.chat.repository.DataCenter;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class MsgReadHandler extends BaseActionHandler {
    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        String reader = getCurrentUser(session);
        JsonNode params = request.getParams();
        String msgId = params.get("msgId").asText();
        
        Message message = DataCenter.MSG_HISTORY.get(msgId);
        if (message != null) {
            message.getReadBy().add(reader);
            // 可以在这里广播已读事件给消息发送者
        }
        sendSuccess(session, "已读标记成功");
    }
}
