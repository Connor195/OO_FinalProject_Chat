package com.chatroom.client.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 服务端响应/推送数据包
 */
public class ServerResponse {
    private String type;
    private int code;
    private String msg;
    private JsonElement data; // 支持对象和数组

    public ServerResponse() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public JsonElement getData() {
        return data;
    }

    public void setData(JsonElement data) {
        this.data = data;
    }

    /**
     * 获取 data 作为 JsonObject（如果 data 是对象）
     */
    public JsonObject getDataAsObject() {
        if (data != null && data.isJsonObject()) {
            return data.getAsJsonObject();
        }
        return null;
    }

    public boolean isSuccess() {
        return code == 200;
    }
}

