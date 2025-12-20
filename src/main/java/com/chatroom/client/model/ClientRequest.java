package com.chatroom.client.model;

import com.google.gson.JsonObject;

/**
 * 客户端请求数据包
 */
public class ClientRequest {
    private String action;
    private JsonObject params;

    public ClientRequest() {
    }

    public ClientRequest(String action, JsonObject params) {
        this.action = action;
        this.params = params;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public JsonObject getParams() {
        return params;
    }

    public void setParams(JsonObject params) {
        this.params = params;
    }
}

