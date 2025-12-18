package com.example.chat.handler.action.impl;

import static org.mockito.Mockito.*;

import com.example.chat.common.packet.WsRequest;
import com.example.chat.common.model.Message;
import com.example.chat.common.model.Group;
import com.example.chat.repository.DataCenter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@ExtendWith(MockitoExtension.class)
class GetHistoryHandlerTest {
    
    @Mock private WebSocketSession session;
    @Mock private JsonNode params;
    private GetHistoryHandler handler;
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() {
        handler = new GetHistoryHandler();
        // 清空测试数据
        DataCenter.MSG_HISTORY.clear();
        DataCenter.GROUPS.clear();
        DataCenter.ONLINE_USERS.clear();
        
        // 模拟会话属性
        when(session.getAttributes()).thenReturn(new HashMap<>());
    }
    
    @Test
    void testGetHistory_UserNotLoggedIn() {
        // 模拟未登录用户
        when(session.getAttributes()).thenReturn(new HashMap<>());
        
        WsRequest request = new WsRequest();
        request.setAction("GET_HISTORY");
        request.setParams(params);
        
        // 调用handle方法
        handler.handle(session, request);
        
        // 验证应该发送错误消息（可以通过模拟sendError方法或验证异常）
        // 这里需要你根据BaseActionHandler的实现来验证
    }
    
    @Test
    void testGetHistory_WithPrivateMessages() {
        // 准备测试数据
        String userA = "Alice";
        String userB = "Bob";
        
        // 创建测试消息
        Message msg1 = Message.builder()
            .msgId("msg1")
            .fromUser(userA)
            .toUser(userB)
            .isGroup(false)
            .content("Hello Bob")
            .timestamp(System.currentTimeMillis() - 10000)
            .build();
            
        Message msg2 = Message.builder()
            .msgId("msg2")
            .fromUser(userB)
            .toUser(userA)
            .isGroup(false)
            .content("Hi Alice")
            .timestamp(System.currentTimeMillis() - 5000)
            .build();
            
        // 存储到数据中心
        DataCenter.MSG_HISTORY.put("msg1", msg1);
        DataCenter.MSG_HISTORY.put("msg2", msg2);
        
        // 模拟登录用户
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("username", userA);
        when(session.getAttributes()).thenReturn(attrs);
        
        // 创建请求
        WsRequest request = new WsRequest();
        request.setAction("GET_HISTORY");
        ObjectNode paramsNode = objectMapper.createObjectNode();
        request.setParams(paramsNode);
        
        // 测试：应该只返回与Alice相关的消息
        handler.handle(session, request);
        
        // 这里需要验证返回的消息列表
        // 可以通过捕获sendSuccess调用的参数来验证
    }
    
    @Test
    void testGetHistory_WithGroupMessages() {
        // 准备测试数据
        String userA = "Alice";
        String userB = "Bob";
        String groupId = "group1";
        
        // 创建群组
        Group group = new Group();
        group.setGroupId(groupId);
        group.setGroupName("Test Group");
        group.setOwner(userA);
        group.setMembers(new CopyOnWriteArrayList<>(Arrays.asList(userA, userB)));
        DataCenter.GROUPS.put(groupId, group);
        
        // 创建群消息
        Message msg1 = Message.builder()
            .msgId("msg1")
            .fromUser(userA)
            .toUser(groupId)
            .isGroup(true)
            .content("Hello everyone")
            .timestamp(System.currentTimeMillis() - 10000)
            .build();
            
        DataCenter.MSG_HISTORY.put("msg1", msg1);
        
        // 模拟登录用户
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("username", userA);
        when(session.getAttributes()).thenReturn(attrs);
        
        // 测试：Alice应该能看到群消息
        WsRequest request = new WsRequest();
        request.setAction("GET_HISTORY");
        handler.handle(session, request);
        
        // 验证返回结果包含群消息
    }
    
    @Test
    void testGetHistory_Pagination() {
        // 创建多个测试消息
        String userA = "Alice";
        String userB = "Bob";
        
        for (int i = 1; i <= 30; i++) {
            Message msg = Message.builder()
                .msgId("msg" + i)
                .fromUser(i % 2 == 0 ? userA : userB)
                .toUser(i % 2 == 0 ? userB : userA)
                .isGroup(false)
                .content("Message " + i)
                .timestamp(System.currentTimeMillis() - i * 1000)
                .build();
            DataCenter.MSG_HISTORY.put("msg" + i, msg);
        }
        
        // 模拟登录用户
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("username", userA);
        when(session.getAttributes()).thenReturn(attrs);
        
        // 测试：应该只返回最近20条
        WsRequest request = new WsRequest();
        request.setAction("GET_HISTORY");
        handler.handle(session, request);
        
        // 验证返回20条消息
    }
}
