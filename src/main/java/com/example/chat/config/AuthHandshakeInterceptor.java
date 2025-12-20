package com.example.chat.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 握手拦截器：在 WebSocket 连接建立之前执行
 */
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * 握手前 (Before Handshake)
     * 返回 true = 允许连接；返回 false = 拒绝连接
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        // 1. 获取客户端 IP 地址
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String ipAddress = servletRequest.getServletRequest().getRemoteAddr();

            // 模拟黑名单逻辑
            if ("192.168.1.100".equals(ipAddress)) {
                System.out.println("拦截黑名单 IP: " + ipAddress);
                return false; // ⛔️ 拒绝连接
            }

            // 2. 将 IP 存入 WebSocket 的 Session 属性 (上下文)
            // 这样你在 ChatHandler 里就能通过 session.getAttributes().get("clientIp") 拿到了
            attributes.put("clientIp", ipAddress);
            System.out.println("访客 IP 已记录: " + ipAddress);
        }

        return true; // ✅ 放行
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后的收尾工作，通常不用写
    }
}