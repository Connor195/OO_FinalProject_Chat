package com.example.chat.handler.action.impl;

import com.example.chat.common.packet.WsRequest;
import com.example.chat.common.model.Message;
import com.example.chat.handler.action.BaseActionHandler;
import com.example.chat.repository.DataCenter;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 获取历史消息处理器
 */
@Component
public class Get_HistoryHandler extends BaseActionHandler {

    private static final int PAGE_SIZE = 20;

    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        try {
            String currentUser = getCurrentUser(session);
            if (currentUser == null) {
                sendError(session, "请先登录");
                return;
            }

            // 获取参数
            JsonNode params = request.getParams();
            Long beforeTime = params != null && params.has("beforeTime")
                    ? params.get("beforeTime").asLong()
                    : null;

            // 收集所有相关消息
            List<Message> allMessages = new ArrayList<>();

            // 1. 私聊消息（自己发送或接收的）
            DataCenter.MSG_HISTORY.values().forEach(msg -> {
                if (!msg.isGroup()) {
                    if (msg.getFromUser().equals(currentUser) ||
                            msg.getToUser().equals(currentUser)) {
                        allMessages.add(msg);
                    }
                }
            });

            // 2. 群聊消息（自己所在的群组）
            DataCenter.GROUPS.forEach((groupId, group) -> {
                if (group.getMembers().contains(currentUser)) {
                    DataCenter.MSG_HISTORY.values().forEach(msg -> {
                        if (msg.isGroup() && msg.getToUser().equals(groupId)) {
                            allMessages.add(msg);
                        }
                    });
                }
            });

            // 过滤、排序、分页
            List<Message> filtered = allMessages.stream()
                    .filter(msg -> beforeTime == null || msg.getTimestamp() < beforeTime)
                    .sorted(Comparator.comparing(Message::getTimestamp).reversed())
                    .limit(PAGE_SIZE)
                    .sorted(Comparator.comparing(Message::getTimestamp))
                    .collect(Collectors.toList());

            // 构造响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("messages", filtered);
            responseData.put("hasMore", filtered.size() == PAGE_SIZE);
            if (!filtered.isEmpty()) {
                responseData.put("nextBeforeTime", filtered.get(filtered.size() - 1).getTimestamp());
            }

            sendSuccess(session, responseData);

            System.out.println("历史消息获取成功: 用户[" + currentUser + "], 返回消息数: " + filtered.size() + ", 是否有更多: "
                    + (filtered.size() == PAGE_SIZE));

        } catch (Exception e) {
            sendError(session, "获取历史消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
