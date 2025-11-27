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

import java.io.IOException;
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
            case "KICK_USER":
                handleKickUser(session, request);
                break;
            case "MUTE_USER":
                handleMuteUser(session, request);
                break;
            case "MSG_READ":
                handleMsgRead(session, request);
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
    /**
     * 处理登录逻辑 (包含密码验证与顶号机制)
     */
    private void handleLogin(WebSocketSession session, WsRequest request) {
        // 1. 参数校验
        if (request.getParams() == null || !request.getParams().has("username")) {
            sendError(session, "登录失败: 缺少 username 参数");
            return;
        }

        // 提取参数
        String username = request.getParams().get("username").asText();
        // 如果前端没传密码，给一个默认空字符串，防止 Service 层空指针
        String password = request.getParams().has("password")
                ? request.getParams().get("password").asText()
                : "";

        try {
            // 2. 调用业务层 (UserService) 进行登录/注册
            // 约定：如果密码错误，Service 层会抛出 IllegalArgumentException
            // 约定：如果用户不存在，Service 层会自动注册并返回 User 对象
            User user = userService.login(username, password);

            // 3. 处理“顶号”逻辑 (如果该账号已经在别处登录)
            WebSocketSession oldSession = DataCenter.ONLINE_USERS.get(username);
            if (oldSession != null && oldSession.isOpen()) {
                // 如果旧连接不是当前连接 (防止自己重连误踢)
                if (!oldSession.getId().equals(session.getId())) {
                    sendError(oldSession, "您的账号已在别处登录，您已被强制下线！");
                    try {
                        oldSession.close(); // 踢掉旧连接
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // 4. 绑定会话 (更新内存表)
            DataCenter.ONLINE_USERS.put(username, session);

            // [关键] 将 username 存入 Session 的属性中
            // 这样后续发送消息时，可以直接从 session.getAttributes() 拿到是谁发的，防止伪造身份
            session.getAttributes().put("username", username);

            // 5. 返回成功响应
            WsResponse response = WsResponse.builder()
                    .type("LOGIN_RESP")
                    .data(user) // 返回用户信息(头像、好友列表等)给前端
                    .build();
            sendJson(session, response);

            System.out.println("用户登录成功: " + username);

        } catch (IllegalArgumentException e) {
            // 捕获抛出的“密码错误”异常
            sendError(session, "登录失败: " + e.getMessage());
            System.out.println("登录失败(" + username + "): " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "系统内部错误");
        }
    }
    private void handleMessage(WebSocketSession session, WsRequest request) {
        String fromUsername = (String) session.getAttributes().get("username");
        User user = DataCenter.USERS.get(fromUsername);

        // --- 核心修改：禁言检查 ---
        if (user != null && user.isMuted()) {
            long remaining = (user.getMuteEndTime() - System.currentTimeMillis()) / 1000;
            sendError(session, "您处于禁言状态，剩余 " + remaining + " 秒");
            return; // ⛔️ 直接阻断，不让发消息
        }
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
            // --- 新增：解析 @ 用户列表 ---
            java.util.List<String> atList = new java.util.ArrayList<>();
            if (request.getParams().has("atUsers")) {
                com.fasterxml.jackson.databind.JsonNode atNode = request.getParams().get("atUsers");
                if (atNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode node : atNode) {
                        atList.add(node.asText());
                    }
                }
            }
            // -------------------------

            // 3.调用 Service (这一步会生成时间戳和ID)
            com.example.chat.common.model.Message msgObj = messageService.processAndSaveMsg(
                    fromUser, toId, content, isGroup, atList
            );

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
        // 以前是: Set<String> users = ...
        // 现在改成: 返回 List<User>
        // 注意：因为 User 类加了 @JsonIgnore password，所以这里发过去是安全的

        java.util.List<User> onlineUserList = new java.util.ArrayList<>();

        for (String username : DataCenter.ONLINE_USERS.keySet()) {
            User u = DataCenter.USERS.get(username);
            if (u != null) {
                onlineUserList.add(u);
            }
        }

        WsResponse response = WsResponse.builder()
                .type("ONLINE_LIST")
                .data(onlineUserList) // 前端会收到 [{username:"admin", role:"ADMIN"}, ...]
                .build();

        sendJson(session, response);
    }
    /**
     * 处理“已读”逻辑
     * 前端只要看到消息出现在屏幕上，就发一个 MSG_READ 包过来
     */
    /**
     * 处理“已读”逻辑
     * (已修复：使用 Map 替代匿名内部类，解决字段引用报错)
     */
    private void handleMsgRead(WebSocketSession session, WsRequest request) {
        // 1. 谁读了？
        String reader = (String) session.getAttributes().get("username");
        if (reader == null) return;

        // 2. 读了哪条？
        if (!request.getParams().has("msgId")) return;
        String msgId = request.getParams().get("msgId").asText();

        // 3. 找消息对象
        com.example.chat.common.model.Message msg = DataCenter.MSG_HISTORY.get(msgId);
        if (msg == null) return; // 消息可能太久远被清理了，忽略

        // 4. 记录状态 (Set会自动去重)
        // 如果已经记录过他读了，就不用再广播了
        if (msg.getReadBy().contains(reader)) {
            return;
        }
        msg.getReadBy().add(reader);

        // 5. 通知发送者 ("你的消息被 xxx 读了")
        String senderName = msg.getFromUser();
        WebSocketSession senderSession = DataCenter.ONLINE_USERS.get(senderName);

        if (senderSession != null && senderSession.isOpen()) {
            // --- 修改点开始：使用 Map 封装数据 ---
            java.util.Map<String, Object> readData = new java.util.HashMap<>();
            readData.put("msgId", msg.getMsgId());
            readData.put("reader", reader); // 这里 reader 指向局部变量，不会冲突
            readData.put("readCount", msg.getReadBy().size());
            // ----------------------------------

            WsResponse resp = WsResponse.builder()
                    .type("EVENT_MSG_READ")
                    .data(readData) // 放入 Map
                    .build();
            sendJson(senderSession, resp);
        }
    }
    /**
     * 管理员功能：踢人
     */
    private void handleKickUser(WebSocketSession session, WsRequest request) {
        if (!checkAdmin(session)) return; // 1. 检查权限

        String targetUser = request.getParams().get("targetUser").asText();
        WebSocketSession targetSession = DataCenter.ONLINE_USERS.get(targetUser);

        if (targetSession != null) {
            // 发送通知并断开
            sendError(targetSession, "【系统通知】您已被管理员强制下线！");
            try {
                targetSession.close();
            } catch (Exception e) { e.printStackTrace(); }

            // 回复管理员
            sendJson(session, WsResponse.builder().type("SYS_NOTICE").msg("已踢出 " + targetUser).build());
        }
    }

    /**
     * 管理员功能：禁言
     * Params: { "targetUser": "Tom", "duration": 60 } (单位秒)
     */
    private void handleMuteUser(WebSocketSession session, WsRequest request) {
        if (!checkAdmin(session)) return; // 1. 检查权限

        String targetName = request.getParams().get("targetUser").asText();
        long duration = request.getParams().get("duration").asLong(); // 禁言多少秒

        User targetUser = DataCenter.USERS.get(targetName);
        if (targetUser != null) {
            // 设置禁言截止时间 = 当前时间 + 持续秒数 * 1000
            targetUser.setMuteEndTime(System.currentTimeMillis() + (duration * 1000));

            // 通知管理员
            sendJson(session, WsResponse.builder().type("SYS_NOTICE").msg("已禁言 " + targetName).build());

            // 通知被禁言的人
            WebSocketSession targetSession = DataCenter.ONLINE_USERS.get(targetName);
            if (targetSession != null) {
                sendError(targetSession, "【系统通知】您被管理员禁言 " + duration + " 秒");
            }
        }
    }

    // 辅助方法：检查当前 Session 是否是管理员
    private boolean checkAdmin(WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");
        User user = DataCenter.USERS.get(username);
        if (user == null || !user.isAdmin()) {
            sendError(session, "权限不足：仅管理员可用");
            return false;
        }
        return true;
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
    private void handleLogout(WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            // 1. 从在线列表移除
            DataCenter.ONLINE_USERS.remove(username);
            // 2. 告诉前端一声
            sendJson(session, WsResponse.builder().type("LOGOUT_SUCCESS").build());
            // 3. 关闭连接
            try {
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}