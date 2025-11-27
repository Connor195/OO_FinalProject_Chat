package com.example.chat.common.packet;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WsResponse {
    private String type;    // 响应类型
    @Builder.Default
    private int code = 200;
    @Builder.Default
    private String msg = "ok";
    private Object data;    // 业务数据

    // 快速报错的工具方法
    public static WsResponse error(String msg) {
        return WsResponse.builder().type("ERROR").code(500).msg(msg).build();
    }
}