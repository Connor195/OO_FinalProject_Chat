package com.chatroom.client.model;

/**
 * 用户信息对象（用于在线列表）
 */
public class UserInfo {
    private String username;
    private String role; // "ADMIN" 或 "USER"
    private String avatar;

    public UserInfo() {
    }

    public UserInfo(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
