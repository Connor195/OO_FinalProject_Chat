package com.example.chat.handler.action.impl;

import com.example.chat.common.packet.WsRequest;
import com.example.chat.common.packet.WsResponse;
import com.example.chat.common.model.Message;
import com.example.chat.handler.action.BaseActionHandler;
import com.example.chat.repository.DataCenter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

@Component("MSG_READ")
public class Msg_ReadHandler extends BaseActionHandler {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        try {
            String reader = getCurrentUser(session);
            JsonNode params = request.getParams();

            // 1. 参数校验
            if (params == null || !params.has("msgId") || params.get("msgId").isNull()) {
                sendError(session, "参数错误: 缺少msgId");
                return;
            }

            String msgId = params.get("msgId").asText();

            // 2. 获取消息
            Message message = DataCenter.MSG_HISTORY.get(msgId);
            if (message == null) {
                sendError(session, "消息不存在: " + msgId);
                return;
            }

            // 3. 权限检查
            // 检查：如果消息不是发给当前用户的，不能标记已读
            if (!message.getToUser().equals(reader) && !message.getFromUser().equals(reader)) {
                // 如果是群消息，检查用户是否在群里
                if (message.isGroup()) {
                } else {
                    sendError(session, "无权标记此消息已读");
                    return;
                }
            }

            // 4. 标记已读（去重）
            Set<String> readBy = message.getReadBy();
            boolean firstTimeRead = !readBy.contains(reader);

            if (firstTimeRead) {
                readBy.add(reader);

                // 5. 广播已读事件给消息发送者
                broadcastReadEvent(message, reader);
            }

            // 6. 返回成功响应
            sendSuccess(session, "已读标记成功");

            System.out.println("消息已读标记成功: 消息[" + msgId + "], 标记用户: " + reader + ", 已读人数: "
                    + (message != null ? message.getReadBy().size() : 0));

        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "标记已读失败: " + e.getMessage());
        }
    }

    /**
     * 广播已读事件给消息发送者
     */
    private void broadcastReadEvent(Message message, String reader) {
        try {
            // 构建事件数据
            ObjectNode eventData = objectMapper.createObjectNode();
            eventData.put("msgId", message.getMsgId());
            eventData.put("reader", reader);
            eventData.put("readCount", message.getReadBy().size());

            // 构建事件响应
            WsResponse event = WsResponse.builder()
                    .type("EVENT_MSG_READ")
                    .data(eventData)
                    .build();

            String eventJson = objectMapper.writeValueAsString(event);

            // 发送给消息的发送者
            String sender = message.getFromUser();
            WebSocketSession senderSession = DataCenter.ONLINE_USERS.get(sender);

            if (senderSession != null && senderSession.isOpen()) {
                synchronized (senderSession) {
                    senderSession.sendMessage(new TextMessage(eventJson));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
