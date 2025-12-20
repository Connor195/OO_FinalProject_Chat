package com.chatroom.client.listener;

import com.chatroom.client.model.ChatMessage;
import com.chatroom.client.model.UserInfo;

import java.util.List;

/**
 * 聊天事件监听器接口
 */
public interface ChatEventListener {
    /**
     * 登录成功回调
     * @param username 用户名
     * @param role 角色（ADMIN/USER）
     * @param avatar 头像
     * @param muteEndTime 禁言结束时间（0表示未禁言）
     */
    void onLoginSuccess(String username, String role, String avatar, long muteEndTime);

    /**
     * 收到新消息
     */
    void onChatMessage(ChatMessage message);

    /**
     * 在线列表更新
     */
    void onOnlineListUpdate(List<UserInfo> userList);

    /**
     * 历史消息列表
     */
    void onHistoryList(List<ChatMessage> messages);

    /**
     * 消息被撤回
     */
    void onMessageRecalled(String recalledMsgId, String operator);

    /**
     * 消息已读回执
     * @param msgId 消息ID
     * @param reader 阅读者
     * @param readCount 已读数量
     */
    void onMessageRead(String msgId, String reader, int readCount);

    /**
     * 消息反应更新（点赞/点踩）
     * @param msgId 消息ID
     * @param reactType 反应类型（like/dislike等）
     * @param operator 操作者
     * @param isAdd true=新增，false=取消
     * @param count 当前总数
     */
    void onMessageReact(String msgId, String reactType, String operator, boolean isAdd, int count);

    /**
     * 对方正在输入
     * @param username 正在输入的用户名
     */
    void onTyping(String username);

    /**
     * 系统通知
     */
    void onSystemNotice(String text);

    /**
     * 错误事件
     */
    void onError(int code, String message);

    /**
     * 连接状态变化
     */
    void onConnectionStateChanged(boolean connected);
}

