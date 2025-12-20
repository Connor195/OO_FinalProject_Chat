package com.chatroom.client;

import com.chatroom.client.listener.ChatEventListener;
import com.chatroom.client.model.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 聊天客户端核心类
 * 基于 API 文档 v2.0 实现
 */
public class ChatClient {
    private WebSocketClient wsClient;
    private final Gson gson = new Gson();
    private ChatEventListener eventListener;
    private String currentUsername;
    private String currentPassword;
    private boolean isConnected = false;
    private ScheduledExecutorService heartbeatExecutor;
    private ScheduledExecutorService reconnectExecutor;
    private final String serverUrl;
    private static final int HEARTBEAT_INTERVAL = 30; // 30秒心跳
    private static final int RECONNECT_DELAY = 3; // 3秒后重连

    public ChatClient(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * 设置事件监听器
     */
    public void setEventListener(ChatEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * 连接到服务器
     */
    public void connect() {
        try {
            URI serverUri = new URI(serverUrl);
            wsClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    isConnected = true;
                    System.out.println("连接已建立");
                    if (eventListener != null) {
                        eventListener.onConnectionStateChanged(true);
                    }
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected = false;
                    System.out.println("连接已关闭: " + reason);
                    if (eventListener != null) {
                        eventListener.onConnectionStateChanged(false);
                    }
                    // 启动重连
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("WebSocket 错误: " + ex.getMessage());
                    ex.printStackTrace();
                }
            };

            wsClient.connect();
        } catch (Exception e) {
            System.err.println("连接失败: " + e.getMessage());
            e.printStackTrace();
            scheduleReconnect();
        }
    }

    /**
     * 处理收到的消息
     */
    private void handleMessage(String message) {
        try {
            ServerResponse response = gson.fromJson(message, ServerResponse.class);
            if (response == null || response.getType() == null) {
                return;
            }

            String type = response.getType();
            JsonElement dataElement = response.getData();
            JsonObject data = response.getDataAsObject();

            switch (type) {
                case EventTypeConstants.LOGIN_RESP:
                    handleLoginResp(data);
                    break;
                case EventTypeConstants.EVENT_CHAT_MSG:
                    handleChatMessage(data);
                    break;
                case EventTypeConstants.ONLINE_LIST:
                    handleOnlineList(dataElement);
                    break;
                case EventTypeConstants.HISTORY_LIST:
                    handleHistoryList(dataElement);
                    break;
                case EventTypeConstants.EVENT_MSG_RECALLED:
                    handleMessageRecalled(data);
                    break;
                case EventTypeConstants.EVENT_MSG_READ:
                    handleMessageRead(data);
                    break;
                case EventTypeConstants.EVENT_MSG_REACT:
                    handleMessageReact(data);
                    break;
                case EventTypeConstants.EVENT_TYPING:
                    handleTyping(dataElement);
                    break;
                case EventTypeConstants.SYS_NOTICE:
                    handleSystemNotice(dataElement);
                    break;
                case EventTypeConstants.ERROR:
                    handleError(response);
                    break;
                default:
                    System.out.println("未知事件类型: " + type);
            }
        } catch (Exception e) {
            System.err.println("解析消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理登录响应
     */
    private void handleLoginResp(JsonObject data) {
        if (data != null && eventListener != null) {
            String username = data.has("username") ? data.get("username").getAsString() : null;
            String role = data.has("role") ? data.get("role").getAsString() : "USER";
            String avatar = data.has("avatar") ? data.get("avatar").getAsString() : null;
            long muteEndTime = data.has("muteEndTime") ? data.get("muteEndTime").getAsLong() : 0;
            eventListener.onLoginSuccess(username, role, avatar, muteEndTime);
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(JsonObject data) {
        if (data != null && eventListener != null) {
            ChatMessage message = gson.fromJson(data, ChatMessage.class);
            eventListener.onChatMessage(message);
        }
    }

    /**
     * 处理在线列表
     */
    private void handleOnlineList(JsonElement dataElement) {
        if (eventListener != null) {
            JsonArray userArray = null;
            // 根据 API 文档，ONLINE_LIST 的 data 是数组
            if (dataElement != null && dataElement.isJsonArray()) {
                userArray = dataElement.getAsJsonArray();
            } else if (dataElement != null && dataElement.isJsonObject()) {
                JsonObject data = dataElement.getAsJsonObject();
                if (data.has("data") && data.get("data").isJsonArray()) {
                    userArray = data.getAsJsonArray("data");
                }
            }
            if (userArray != null) {
                Type listType = new TypeToken<List<UserInfo>>(){}.getType();
                List<UserInfo> userList = gson.fromJson(userArray, listType);
                eventListener.onOnlineListUpdate(userList);
            }
        }
    }

    /**
     * 处理历史消息列表
     */
    private void handleHistoryList(JsonElement dataElement) {
        if (eventListener != null) {
            JsonArray msgArray = null;
            // data 可能是数组，也可能是包含 messages 字段的对象
            if (dataElement != null && dataElement.isJsonArray()) {
                msgArray = dataElement.getAsJsonArray();
            } else if (dataElement != null && dataElement.isJsonObject()) {
                JsonObject data = dataElement.getAsJsonObject();
                if (data.has("messages") && data.get("messages").isJsonArray()) {
                    msgArray = data.getAsJsonArray("messages");
                }
            }
            if (msgArray != null) {
                Type listType = new TypeToken<List<ChatMessage>>(){}.getType();
                List<ChatMessage> messages = gson.fromJson(msgArray, listType);
                eventListener.onHistoryList(messages);
            }
        }
    }

    /**
     * 处理消息撤回
     */
    private void handleMessageRecalled(JsonObject data) {
        if (data != null && eventListener != null) {
            String recalledMsgId = data.has("recalledMsgId") ? data.get("recalledMsgId").getAsString() : null;
            String operator = data.has("operator") ? data.get("operator").getAsString() : null;
            eventListener.onMessageRecalled(recalledMsgId, operator);
        }
    }

    /**
     * 处理消息已读回执
     */
    private void handleMessageRead(JsonObject data) {
        if (data != null && eventListener != null) {
            String msgId = data.has("msgId") ? data.get("msgId").getAsString() : null;
            String reader = data.has("reader") ? data.get("reader").getAsString() : null;
            int readCount = data.has("readCount") ? data.get("readCount").getAsInt() : 0;
            eventListener.onMessageRead(msgId, reader, readCount);
        }
    }

    /**
     * 处理消息反应
     */
    private void handleMessageReact(JsonObject data) {
        if (data != null && eventListener != null) {
            String msgId = data.has("msgId") ? data.get("msgId").getAsString() : null;
            String reactType = data.has("reactType") ? data.get("reactType").getAsString() : null;
            String operator = data.has("operator") ? data.get("operator").getAsString() : null;
            boolean isAdd = data.has("isAdd") ? data.get("isAdd").getAsBoolean() : true;
            int count = data.has("count") ? data.get("count").getAsInt() : 0;
            eventListener.onMessageReact(msgId, reactType, operator, isAdd, count);
        }
    }

    /**
     * 处理正在输入
     */
    private void handleTyping(JsonElement dataElement) {
        if (dataElement != null && eventListener != null) {
            // EVENT_TYPING 的 data 可能是字符串（用户名）或对象
            String username = null;
            if (dataElement.isJsonPrimitive() && dataElement.getAsJsonPrimitive().isString()) {
                username = dataElement.getAsString();
            } else if (dataElement.isJsonObject()) {
                JsonObject data = dataElement.getAsJsonObject();
                username = data.has("username") ? data.get("username").getAsString() : 
                          data.has("data") ? data.get("data").getAsString() : null;
            }
            if (username != null) {
                eventListener.onTyping(username);
            }
        }
    }

    /**
     * 处理系统通知
     */
    private void handleSystemNotice(JsonElement dataElement) {
        if (dataElement != null && eventListener != null) {
            // SYS_NOTICE 的 data 可能是字符串或对象
            String text = null;
            if (dataElement.isJsonPrimitive() && dataElement.getAsJsonPrimitive().isString()) {
                text = dataElement.getAsString();
            } else if (dataElement.isJsonObject()) {
                JsonObject data = dataElement.getAsJsonObject();
                text = data.has("text") ? data.get("text").getAsString() :
                      data.has("msg") ? data.get("msg").getAsString() :
                      data.has("data") ? data.get("data").getAsString() : null;
            }
            if (text != null) {
                eventListener.onSystemNotice(text);
            }
        }
    }

    /**
     * 处理错误
     */
    private void handleError(ServerResponse response) {
        if (eventListener != null) {
            eventListener.onError(response.getCode(), response.getMsg());
        }
    }

    /**
     * 发送请求
     */
    private void sendRequest(ClientRequest request) {
        if (!isConnected || wsClient == null) {
            System.err.println("未连接，无法发送消息");
            return;
        }

        try {
            String json = gson.toJson(request);
            wsClient.send(json);
        } catch (Exception e) {
            System.err.println("发送消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 用户登录（支持密码）
     * @param username 用户名
     * @param password 密码（可为空，新用户自动注册）
     */
    public void login(String username, String password) {
        this.currentUsername = username;
        this.currentPassword = password;
        JsonObject params = new JsonObject();
        params.addProperty("username", username);
        if (password != null && !password.isEmpty()) {
            params.addProperty("password", password);
        }

        ClientRequest request = new ClientRequest(ActionConstants.LOGIN, params);
        sendRequest(request);
    }

    /**
     * 用户登出
     */
    public void logout() {
        JsonObject params = new JsonObject();
        ClientRequest request = new ClientRequest(ActionConstants.LOGOUT, params);
        sendRequest(request);
    }

    /**
     * 发送私聊消息
     * @param targetUser 接收者用户名
     * @param content 消息内容
     * @param atUsers @ 的用户列表（可为空）
     */
    public void sendPrivateMessage(String targetUser, String content, List<String> atUsers) {
        JsonObject params = new JsonObject();
        params.addProperty("targetUser", targetUser);
        params.addProperty("content", content);
        params.addProperty("type", "text");
        if (atUsers != null && !atUsers.isEmpty()) {
            params.add("atUsers", gson.toJsonTree(atUsers));
        }

        ClientRequest request = new ClientRequest(ActionConstants.SEND_PRIVATE, params);
        sendRequest(request);
    }

    /**
     * 发送群聊消息
     * @param targetUser 群ID（如 "Group1"）
     * @param content 消息内容
     * @param atUsers @ 的用户列表（可为空）
     */
    public void sendGroupMessage(String targetUser, String content, List<String> atUsers) {
        JsonObject params = new JsonObject();
        params.addProperty("targetUser", targetUser);
        params.addProperty("content", content);
        params.addProperty("type", "text");
        if (atUsers != null && !atUsers.isEmpty()) {
            params.add("atUsers", gson.toJsonTree(atUsers));
        }

        ClientRequest request = new ClientRequest(ActionConstants.SEND_GROUP, params);
        sendRequest(request);
    }

    /**
     * 撤回消息（2分钟内）
     * @param msgId 消息ID
     */
    public void recallMessage(String msgId) {
        JsonObject params = new JsonObject();
        params.addProperty("msgId", msgId);

        ClientRequest request = new ClientRequest(ActionConstants.RECALL_MSG, params);
        sendRequest(request);
    }

    /**
     * 获取在线列表
     */
    public void getOnlineList() {
        JsonObject params = new JsonObject();
        ClientRequest request = new ClientRequest(ActionConstants.GET_ONLINE, params);
        sendRequest(request);
    }

    /**
     * 获取历史消息（支持翻页）
     * @param beforeTime 获取此时间点之前的20条消息，首次加载不传（传null）
     */
    public void getHistory(Long beforeTime) {
        JsonObject params = new JsonObject();
        if (beforeTime != null) {
            params.addProperty("beforeTime", beforeTime);
        }

        ClientRequest request = new ClientRequest(ActionConstants.GET_HISTORY, params);
        sendRequest(request);
    }

    /**
     * 发送已读回执
     * @param msgId 消息ID
     */
    public void sendMessageRead(String msgId) {
        JsonObject params = new JsonObject();
        params.addProperty("msgId", msgId);

        ClientRequest request = new ClientRequest(ActionConstants.MSG_READ, params);
        sendRequest(request);
    }

    /**
     * 发送正在输入提示
     * @param targetUser 目标用户（私聊时）或群ID（群聊时）
     */
    public void sendTypingStart(String targetUser) {
        JsonObject params = new JsonObject();
        params.addProperty("targetUser", targetUser);

        ClientRequest request = new ClientRequest(ActionConstants.TYPING_START, params);
        sendRequest(request);
    }

    /**
     * 消息点赞/点踩
     * @param msgId 消息ID
     * @param reactType 反应类型（如 "like", "dislike", "heart" 等）
     */
    public void sendMessageReact(String msgId, String reactType) {
        JsonObject params = new JsonObject();
        params.addProperty("msgId", msgId);
        params.addProperty("reactType", reactType);

        ClientRequest request = new ClientRequest(ActionConstants.MSG_REACT, params);
        sendRequest(request);
    }

    /**
     * 强制踢人（管理员功能）
     * @param targetUser 目标用户名
     */
    public void kickUser(String targetUser) {
        JsonObject params = new JsonObject();
        params.addProperty("targetUser", targetUser);

        ClientRequest request = new ClientRequest(ActionConstants.KICK_USER, params);
        sendRequest(request);
    }

    /**
     * 禁言用户（管理员功能）
     * @param targetUser 目标用户名
     * @param duration 禁言时长（秒）
     */
    public void muteUser(String targetUser, int duration) {
        JsonObject params = new JsonObject();
        params.addProperty("targetUser", targetUser);
        params.addProperty("duration", duration);

        ClientRequest request = new ClientRequest(ActionConstants.MUTE_USER, params);
        sendRequest(request);
    }

    /**
     * 发送心跳
     */
    public void sendHeartbeat() {
        JsonObject params = new JsonObject();
        ClientRequest request = new ClientRequest(ActionConstants.HEARTBEAT, params);
        sendRequest(request);
    }

    /**
     * 启动心跳定时任务
     */
    public void startHeartbeat() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
        }
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleAtFixedRate(
            this::sendHeartbeat,
            HEARTBEAT_INTERVAL,
            HEARTBEAT_INTERVAL,
            TimeUnit.SECONDS
        );
    }

    /**
     * 停止心跳
     */
    public void stopHeartbeat() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
        }
    }

    /**
     * 安排重连
     */
    private void scheduleReconnect() {
        if (reconnectExecutor != null) {
            reconnectExecutor.shutdown();
        }
        reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
        reconnectExecutor.schedule(() -> {
            System.out.println("尝试重新连接...");
            connect();
            // 如果已登录，重新登录
            if (currentUsername != null) {
                try {
                    Thread.sleep(1000); // 等待连接建立
                    login(currentUsername, currentPassword);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, RECONNECT_DELAY, TimeUnit.SECONDS);
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        stopHeartbeat();
        if (reconnectExecutor != null) {
            reconnectExecutor.shutdown();
        }
        if (wsClient != null) {
            wsClient.close();
        }
        isConnected = false;
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return isConnected && wsClient != null && wsClient.isOpen();
    }

    /**
     * 获取当前用户名
     */
    public String getCurrentUsername() {
        return currentUsername;
    }
}
