package com.example.chat.common.packet;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class WsRequest {
    private String action;      // 指令: LOGIN, SEND_PRIVATE 等
    private JsonNode params;    // 具体参数（比如用户名、内容）
}