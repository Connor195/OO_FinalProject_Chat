package com.example.chat.handler.action.impl;

import com.example.chat.common.packet.WsRequest;
import com.example.chat.common.packet.WsResponse;
import com.example.chat.common.model.Message;
import com.example.chat.common.model.Group;
import com.example.chat.handler.action.BaseActionHandler;
import com.example.chat.repository.DataCenter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息反应（点赞/点踩等）处理器
 * 支持添加/取消反应
 */
@Component
public class MsgReactHandler extends BaseActionHandler {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 支持的默认反应类型
    private static final Set<String> DEFAULT_REACT_TYPES = new HashSet<String>() {{
        add("like");     // 点赞
        add("dislike");  // 点踩
        add("heart");    // 爱心
        add("laugh");    // 大笑
        add("sad");      // 难过
        add("angry");    // 生气
    }};
    
    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        try {
            String operator = getCurrentUser(session);
            JsonNode params = request.getParams();
            
            // 1. 参数校验
            if (params == null || !params.has("msgId") || !params.has("reactType")) {
                sendError(session, "参数错误：缺少 msgId 或 reactType");
                return;
            }
            
            String msgId = params.get("msgId").asText();
            String reactType = params.get("reactType").asText().toLowerCase();
            
            if (msgId == null || msgId.trim().isEmpty()) {
                sendError(session, "消息ID不能为空");
                return;
            }
            
            if (reactType == null || reactType.trim().isEmpty()) {
                sendError(session, "反应类型不能为空");
                return;
            }

            // 检查反应类型是否合法
            if (!isValidReactType(reactType)) {
                sendError(session, "不支持的反应类型: " + reactType + 
                    "。支持的类型: " + String.join(", ", DEFAULT_REACT_TYPES));
                return;
            }
            
            // 2. 获取消息
            Message message = DataCenter.MSG_HISTORY.get(msgId);
            if (message == null) {
                sendError(session, "消息不存在");
                return;
            }
            
            // 3. 权限检查：用户是否可以对该消息进行反应
            if (!canReactToMessage(operator, message)) {
                sendError(session, "无权对此消息进行反应");
                return;
            }
            
            // 4. 处理反应（线程安全）
            boolean isAdd = processReaction(message, operator, reactType);
            
            // 5. 获取当前反应计数
            int count = getReactionCount(message, reactType);
            
            // 6. 广播反应事件
            broadcastReactionEvent(message, operator, reactType, isAdd, count);
            
            // 7. 返回成功响应
            ObjectNode responseData = objectMapper.createObjectNode();
            responseData.put("msgId", msgId);
            responseData.put("reactType", reactType);
            responseData.put("isAdd", isAdd);
            responseData.put("count", count);
            sendSuccess(session, responseData);
            
        } catch (Exception e) {
            sendError(session, "处理反应失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 验证反应类型是否合法
     */
    private boolean isValidReactType(String reactType) {
        return DEFAULT_REACT_TYPES.contains(reactType);
    }
    
    /**
     * 检查用户是否可以对该消息进行反应
     */
    private boolean canReactToMessage(String operator, Message message) {
        // 用户必须在线
        if (!DataCenter.ONLINE_USERS.containsKey(operator)) {
            return false;
        }
        
        // 如果是私聊消息，必须是发送者或接收者
        if (!message.isGroup()) {
            return message.getFromUser().equals(operator) || 
                   message.getToUser().equals(operator);
        }
        
        // 如果是群聊消息，必须是群成员
        Group group = DataCenter.GROUPS.get(message.getToUser());
        if (group != null) {
            return group.getMembers().contains(operator);
        }
        
        return false;
    }
    
    /**
     * 处理反应逻辑（线程安全）
     * @return true=添加反应, false=取消反应
     */
    private boolean processReaction(Message message, String operator, String reactType) {
        // 同步操作确保线程安全
        synchronized (message) {
            Set<String> reactors = message.getReactions().computeIfAbsent(
                reactType, k -> ConcurrentHashMap.newKeySet()
            );
            
            boolean isAdd;
            if (reactors.contains(operator)) {
                // 已存在，移除（取消反应）
                reactors.remove(operator);
                isAdd = false;
                
                // 如果该类型没有用户了，清理空集合
                if (reactors.isEmpty()) {
                    message.getReactions().remove(reactType);
                }
            } else {
                // 先移除用户的其他反应（一个用户对一条消息只能有一种反应）
                removeUserFromOtherReactions(message, operator);
                
                // 添加新反应
                reactors.add(operator);
                isAdd = true;
            }
            
            return isAdd;
        }
    }
    
    /**
     * 从其他反应类型中移除用户
     */
    private void removeUserFromOtherReactions(Message message, String operator) {
        message.getReactions().forEach((reactType, users) -> {
            users.remove(operator);
        });
        
        // 清理空集合
        message.getReactions().entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    
    /**
     * 获取指定反应类型的计数
     */
    private int getReactionCount(Message message, String reactType) {
        Set<String> reactors = message.getReactions().get(reactType);
        return reactors != null ? reactors.size() : 0;
    }
    
    /**
     * 广播反应事件给相关用户
     */
    private void broadcastReactionEvent(Message message, String operator, 
                                       String reactType, boolean isAdd, int count) {
        try {
            // 构建事件数据
            ObjectNode eventData = objectMapper.createObjectNode();
            eventData.put("msgId", message.getMsgId());
            eventData.put("reactType", reactType);
            eventData.put("operator", operator);
            eventData.put("isAdd", isAdd);
            eventData.put("count", count);
            
            // 构建事件响应
            WsResponse event = WsResponse.builder()
                    .type("EVENT_MSG_REACT")
                    .data(eventData)
                    .build();
            
            String eventJson = objectMapper.writeValueAsString(event);
            
            // 确定广播范围
            Set<String> receivers = determineReceivers(message, operator);
            
            // 广播给相关用户
            for (String receiver : receivers) {
                WebSocketSession receiverSession = DataCenter.ONLINE_USERS.get(receiver);
                if (receiverSession != null && receiverSession.isOpen()) {
                    synchronized (receiverSession) {
                        receiverSession.sendMessage(new TextMessage(eventJson));
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 确定需要接收事件通知的用户
     */
    private Set<String> determineReceivers(Message message, String operator) {
        Set<String> receivers = ConcurrentHashMap.newKeySet();
        
        // 总是包括操作者自己（前端需要更新UI）
        receivers.add(operator);
        
        // 包括消息的发送者
        receivers.add(message.getFromUser());
        
        if (message.isGroup()) {
            // 群聊：广播给所有在线群成员
            Group group = DataCenter.GROUPS.get(message.getToUser());
            if (group != null) {
                receivers.addAll(group.getMembers());
            }
        } else {
            // 私聊：只包括发送者和接收者
            receivers.add(message.getToUser());
        }
        
        return receivers;
    }
}
