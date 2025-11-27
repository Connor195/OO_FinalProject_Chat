package com.example.chat.repository;

import com.example.chat.common.model.Group;
import com.example.chat.common.model.Message;
import com.example.chat.common.model.User;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataCenter {
    // 1. 在线用户会话 (Key: Username, Value: Session)
    public static final Map<String, WebSocketSession> ONLINE_USERS = new ConcurrentHashMap<>();

    // 2. 用户信息 (Key: Username)
    public static final Map<String, User> USERS = new ConcurrentHashMap<>();

    // 3. 群组信息 (Key: GroupId)
    public static final Map<String, Group> GROUPS = new ConcurrentHashMap<>();

    // 4. 历史消息缓存 (Key: MsgId)
    public static final Map<String, Message> MSG_HISTORY = new ConcurrentHashMap<>();
}