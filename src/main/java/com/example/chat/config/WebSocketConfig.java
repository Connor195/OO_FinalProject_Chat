package com.example.chat.config;

import com.example.chat.handler.ChatHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ChatHandler chatHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 核心：把 /chat 路径绑定到你的 Handler
        // setAllowedOrigins("*") 允许所有跨域，方便测试
        registry.addHandler(chatHandler, "/chat")
                .setAllowedOrigins("*")
                .addInterceptors(new AuthHandshakeInterceptor());
    }
}