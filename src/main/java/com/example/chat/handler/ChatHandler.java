package com.example.chat.handler;

import com.example.chat.common.model.User;
import com.example.chat.common.packet.WsRequest;
import com.example.chat.common.packet.WsResponse;
import com.example.chat.repository.DataCenter;
import com.example.chat.service.MessageService;
import com.example.chat.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.Executor;

/**
 * 这是你的核心工作类：所有 WebSocket 消息都会流向这里
 */
@Component
public class ChatHandler extends TextWebSocketHandler {

    // 1. 注入队友的服务 (B同学和C同学的代码)
    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    // 2. 注入线程池 (我们刚才在 AppConfig 里配的)
    @Autowired
    @Qualifier("chatExecutor")
    private Executor threadPool;

    // 3. 注入 JSON 工具
    @Autowired
    private ObjectMapper jsonMapper;

    // --- 下面这三个方法是 WebSocket 的生命周期 ---

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("新用户连接: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("收到消息: " + payload);

        // 1. 解析 JSON
        WsRequest request;
        try {
            request = jsonMapper.readValue(payload, WsRequest.class);
        } catch (Exception e) {
            sendError(session, "JSON格式错误");
            return;
        }

        // 2. 检查 Action
        if (request.getAction() == null) {
            return;
        }

        // 3. 路由分发
        switch (request.getAction()) {
            case "LOGIN":
                handleLogin(session, request);
                break;

            case "SEND_PRIVATE":
            case "SEND_GROUP":
                threadPool.execute(() -> handleMessage(session, request));
                break;

            // --- 新增以下 Case ---

            case "CREATE_GROUP":
                handleCreateGroup(session, request);
                break;

            case "RECALL_MSG":
                handleRecallMessage(session, request);
                break;

            case "GET_ONLINE":
                handleGetOnline(session);
                break;

            case "HEARTBEAT":
                // 心跳包通常不需要回复，或者简单回个 pong
                // 什么都不做也行，主要为了保持连接不断
                break;

            default:
                sendError(session, "未知指令: " + request.getAction());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 从 session 属性里拿到用户名
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            DataCenter.ONLINE_USERS.remove(username);
            System.out.println("用户下线: " + username);
        }
    }
    private void handleLogin(WebSocketSession session, WsRequest request) {
        // 1. 拿参数 (注意判空，这里假设前端传了 username)
        if (!request.getParams().has("username")) {
            sendError(session, "缺少 username 参数");
            return;
        }
        String username = request.getParams().get("username").asText();
        String avatar = request.getParams().has("avatar") ? request.getParams().get("avatar").asText() : "";

        // 2. 存入内存 (建立 username -> session 的映射)
        DataCenter.ONLINE_USERS.put(username, session);

        // 可选：把 username 绑在 session 属性里，方便后面用
        session.getAttributes().put("username", username);

        // 3. 调用 Service (虽然现在是假的，但不影响流程)
        User user = userService.login(username, avatar);

        // 4. 告诉前端：登录成功
        WsResponse response = WsResponse.builder()
                .type("LOGIN_RESP")
                .data(user)
                .build();
        sendJson(session, response);

        System.out.println("用户登录成功: " + username);
    }
    private void handleMessage(WebSocketSession session, WsRequest request) {
        try {
            // 1. 既然已经登录了，我们可以直接从 Session 里拿发送者名字
            String fromUser = (String) session.getAttributes().get("username");
            if (fromUser == null) {
                sendError(session, "请先登录 (LOGIN)");
                return;
            }

            // 2. 解析参数
            String toId = request.getParams().get("targetUser").asText(); // 可能是人名，也可能是群ID
            String content = request.getParams().get("content").asText();
            boolean isGroup = "SEND_GROUP".equals(request.getAction()); // 判断是私聊还是群聊

            // 3. 调用 Service 构建消息对象 (这一步会生成时间戳和ID)
            // (注意：这里的 isGroup 参数是根据 action 判断的)
            com.example.chat.common.model.Message msgObj = messageService.processAndSaveMsg(fromUser, toId, content, isGroup);

            // 4. 构造推送给前端的数据包
            WsResponse pushMsg = WsResponse.builder()
                    .type("EVENT_CHAT_MSG")
                    .data(msgObj)
                    .build();

            // 5. 真正的发送环节！
            if (isGroup) {
                // TODO: 群聊逻辑 - 暂时广播给所有人 (方便测试)，后续等 User 模块写好群成员逻辑再改
                DataCenter.ONLINE_USERS.values().forEach(s -> sendJson(s, pushMsg));
            } else {
                // 私聊逻辑 - 找到目标的 Session
                WebSocketSession toSession = DataCenter.ONLINE_USERS.get(toId);
                if (toSession != null) {
                    sendJson(toSession, pushMsg);
                }
                // 别忘了给自己也发一份 (为了让前端显示自己发的消息)
                sendJson(session, pushMsg);
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "消息发送失败");
        }
    }
    private void handleCreateGroup(WebSocketSession session, WsRequest request) {
        // 1. 检查参数
        if (!request.getParams().has("groupName")) {
            sendError(session, "缺少 groupName");
            return;
        }
        String groupName = request.getParams().get("groupName").asText();
        String owner = (String) session.getAttributes().get("username");

        // 2. 调用 Service (用户模块)
        // 注意：这里我们传入 null 作为初始成员，后续让 B同学去实现具体逻辑
        com.example.chat.common.model.Group group = userService.createGroup(groupName, owner, null);

        // 3. 返回结果 (告诉前端群建好了)
        if (group != null) {
            WsResponse response = WsResponse.builder()
                    .type("GROUP_CREATED")
                    .data(group)
                    .build();
            sendJson(session, response);

            // 补充：其实还应该给所有初始成员发一个 SYSTEM_NOTICE，这里先略过
        } else {
            sendError(session, "创建群组失败");
        }
    }
    private void handleRecallMessage(WebSocketSession session, WsRequest request) {
        String msgId = request.getParams().get("msgId").asText();
        String operator = (String) session.getAttributes().get("username");

        // 1. 调用 Service (消息模块) 尝试撤回
        boolean success = messageService.recallMessage(msgId, operator);

        if (success) {
            // 2. 撤回成功，必须广播通知大家！
            // 构造一个通知包
            WsResponse recallNotice = WsResponse.builder()
                    .type("EVENT_MSG_RECALLED")
                    .data(new Object() { // 匿名内部类构建临时 JSON 数据
                        public String recalledMsgId = msgId;
                        public String operatorName = operator;
                    })
                    .build();

            // 3. 广播给所有人 (简单粗暴版：实际应该只发给相关群/人)
            DataCenter.ONLINE_USERS.values().forEach(s -> sendJson(s, recallNotice));
        } else {
            sendError(session, "撤回失败(超时或无权限)");
        }
    }
    private void handleGetOnline(WebSocketSession session) {
        // 直接获取所有在线用户的名字集合
        java.util.Set<String> users = DataCenter.ONLINE_USERS.keySet();

        WsResponse response = WsResponse.builder()
                .type("ONLINE_LIST") // 对应前端文档
                .data(users)
                .build();

        sendJson(session, response);
    }
    /**
     * 核心辅助方法：给指定会话发送 JSON 数据
     * 加上 synchronized 是为了防止多线程同时写入导致数据错乱
     */
    private void sendJson(WebSocketSession session, Object response) {
        try {
            // 1. 把 Java 对象转成 JSON 字符串
            String json = jsonMapper.writeValueAsString(response);
            // 2. 发送
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 辅助方法：给前端发一个错误提示
     */
    private void sendError(WebSocketSession session, String errorMsg) {
        WsResponse response = WsResponse.error(errorMsg);
        sendJson(session, response);
    }
}