package com.example.chat.handler.action.impl;

import com.example.chat.common.packet.WsRequest;
import com.example.chat.common.packet.WsResponse;
import com.example.chat.common.model.Message;
import com.example.chat.common.model.Group;
import com.example.chat.common.model.User;
import com.example.chat.handler.action.BaseActionHandler;
import com.example.chat.repository.DataCenter;
import com.example.chat.service.MessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 撤回消息处理器
 * 支持：
 * 1. 消息发送者撤回自己的消息
 * 2. 管理员撤回任何消息
 * 3. 群主撤回群内消息
 * 撤回时间限制：2分钟
 */
@Component
public class RecallMsgHandler extends BaseActionHandler {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public void handle(WebSocketSession session, WsRequest request) {
        try {
            String operator = getCurrentUser(session);
            if (operator == null) {
                sendError(session, "请先登录");
                return;
            }
            
            JsonNode params = request.getParams();
            
            // 1. 参数校验
            if (params == null || !params.has("msgId")) {
                sendError(session, "参数错误：缺少 msgId");
                return;
            }
            
            String msgId = params.get("msgId").asText();
            
            if (msgId == null || msgId.trim().isEmpty()) {
                sendError(session, "消息ID不能为空");
                return;
            }
            
            // 2. 获取消息（在撤回前先获取消息信息，用于广播）
            Message message = DataCenter.MSG_HISTORY.get(msgId);
            if (message == null) {
                sendError(session, "消息不存在或已被撤回");
                return;
            }
            
            // 3. 执行撤回操作
            boolean recallSuccess = messageService.recallMessage(msgId, operator);
            
            if (!recallSuccess) {
                // 撤回失败的具体原因
                String errorMsg = determineRecallFailureReason(message, operator);
                sendError(session, errorMsg);
                return;
            }
            
            // 4. 广播撤回事件
            broadcastRecallEvent(message, operator);
            
            // 5. 返回成功响应给操作者
            ObjectNode responseData = objectMapper.createObjectNode();
            responseData.put("msgId", msgId);
            responseData.put("message", "消息撤回成功");
            sendSuccess(session, responseData);
            
            System.out.println("消息撤回成功: " + operator + " 撤回了消息 " + msgId);
            
        } catch (Exception e) {
            sendError(session, "撤回消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 确定撤回失败的具体原因
     */
    private String determineRecallFailureReason(Message message, String operator) {
        // 检查权限
        if (!isOperatorAuthorized(message, operator)) {
            if (message.isGroup()) {
                return "您无权撤回此群消息（只有发送者、管理员或群主可以撤回）";
            } else {
                return "您无权撤回此消息（只有发送者可以撤回）";
            }
        }
        
        // 检查时间限制
        long currentTime = System.currentTimeMillis();
        long messageTime = message.getTimestamp();
        long timeLimit = 2 * 60 * 1000; // 2分钟
        
        if (currentTime - messageTime > timeLimit) {
            return "消息发送时间已超过2分钟，无法撤回";
        }
        
        return "撤回失败，未知原因";
    }
    
    /**
     * 检查操作者是否有撤回权限
     * （与MessageServiceImpl中的逻辑保持一致）
     */
    private boolean isOperatorAuthorized(Message message, String operator) {
        // 消息发送者可以撤回
        if (message.getFromUser().equals(operator)) {
            return true;
        }
        
        // 检查操作者是否是管理员
        User user = DataCenter.USERS.get(operator);
        if (user != null && user.isAdmin()) {
            return true;
        }
        
        // 如果是群消息，检查是否是群主
        if (message.isGroup()) {
            Group group = DataCenter.GROUPS.get(message.getToUser());
            if (group != null && group.getOwner().equals(operator)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 广播撤回事件给相关用户
     */
    private void broadcastRecallEvent(Message recalledMessage, String operator) {
        try {
            // 构建事件数据
            ObjectNode eventData = objectMapper.createObjectNode();
            eventData.put("recalledMsgId", recalledMessage.getMsgId());
            eventData.put("operator", operator);
            eventData.put("fromUser", recalledMessage.getFromUser());
            eventData.put("toUser", recalledMessage.getToUser());
            eventData.put("isGroup", recalledMessage.isGroup());
            
            // 如果是群聊，还可以包含群信息
            if (recalledMessage.isGroup()) {
                eventData.put("groupName", getGroupName(recalledMessage.getToUser()));
            }
            
            // 构建事件响应
            WsResponse event = WsResponse.builder()
                    .type("EVENT_MSG_RECALLED")
                    .data(eventData)
                    .build();
            
            String eventJson = objectMapper.writeValueAsString(event);
            
            // 确定广播范围
            Set<String> receivers = determineRecallEventReceivers(recalledMessage);
            
            // 广播给相关用户
            for (String receiver : receivers) {
                WebSocketSession receiverSession = DataCenter.ONLINE_USERS.get(receiver);
                if (receiverSession != null && receiverSession.isOpen()) {
                    synchronized (receiverSession) {
                        receiverSession.sendMessage(new TextMessage(eventJson));
                    }
                }
            }
            
            // 记录日志
            System.out.println("广播撤回事件: 消息 " + recalledMessage.getMsgId() + 
                             " 被 " + operator + " 撤回，通知了 " + receivers.size() + " 个用户");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("广播撤回事件失败: " + e.getMessage());
        }
    }
    
    /**
     * 确定需要接收撤回事件通知的用户
     */
    private Set<String> determineRecallEventReceivers(Message recalledMessage) {
        Set<String> receivers = ConcurrentHashMap.newKeySet();
        
        // 总是包括操作者自己
        // receivers.add(operator); // 注意：这里operator已经通过成功响应知道了
        
        // 包括消息的发送者（如果发送者不是操作者）
        if (!recalledMessage.getFromUser().equals(getCurrentUser(null))) {
            receivers.add(recalledMessage.getFromUser());
        }
        
        if (recalledMessage.isGroup()) {
            // 群聊：广播给所有在线群成员
            Group group = DataCenter.GROUPS.get(recalledMessage.getToUser());
            if (group != null) {
                receivers.addAll(group.getMembers());
            }
        } else {
            // 私聊：包括发送者和接收者
            receivers.add(recalledMessage.getFromUser());
            receivers.add(recalledMessage.getToUser());
        }
        
        // 移除当前用户（避免重复通知，因为操作者已经通过成功响应知道了）
        String currentUser = getCurrentUser(null);
        if (currentUser != null) {
            receivers.remove(currentUser);
        }
        
        // 确保接收者在线
        receivers.removeIf(user -> !DataCenter.ONLINE_USERS.containsKey(user));
        
        return receivers;
    }
    
    /**
     * 获取群组名称（辅助方法）
     */
    private String getGroupName(String groupId) {
        Group group = DataCenter.GROUPS.get(groupId);
        return group != null ? group.getGroupName() : "未知群组";
    }
}
