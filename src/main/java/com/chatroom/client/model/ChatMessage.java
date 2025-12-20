package com.chatroom.client.model;

import java.util.List;
import java.util.Map;

/**
 * 聊天消息对象
 */
public class ChatMessage {
    private String msgId;
    private String fromUser;
    private String toUser;
    private boolean isGroup;
    private String content;
    private String type; // 消息类型，如 "text"
    private long timestamp;
    private List<String> atUsers; // @ 的用户列表
    private Map<String, Integer> reactions; // 点赞/点踩等反应，如 {"like": 5, "dislike": 2}
    private List<String> readBy; // 已读用户列表

    public ChatMessage() {
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getAtUsers() {
        return atUsers;
    }

    public void setAtUsers(List<String> atUsers) {
        this.atUsers = atUsers;
    }

    public Map<String, Integer> getReactions() {
        return reactions;
    }

    public void setReactions(Map<String, Integer> reactions) {
        this.reactions = reactions;
    }

    public List<String> getReadBy() {
        return readBy;
    }

    public void setReadBy(List<String> readBy) {
        this.readBy = readBy;
    }
}

