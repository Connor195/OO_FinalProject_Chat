package com.example.chat.handler;

import com.example.chat.handler.action.ActionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler 注册表 - 负责管理所有的 ActionHandler
 */
@Component
public class HandlerRegistry {
    
    private final Map<String, ActionHandler> handlerMap = new ConcurrentHashMap<>();
    
    // 通过自动注入所有实现了 ActionHandler 接口的 Bean
    @Autowired(required = false)
    private Map<String, ActionHandler> actionHandlerBeans;
    
    /**
     * 初始化注册表，自动注册所有 ActionHandler
     */
    @PostConstruct
    public void init() {
        if (actionHandlerBeans != null) {
            actionHandlerBeans.forEach((beanName, handler) -> {
                // 约定：Bean 名称以 Handler 结尾，去掉 Handler 就是 action 名称
                // 例如：LoginHandler -> LOGIN
                String action = beanName.replace("Handler", "").toUpperCase();
                registerHandler(action, handler);
                System.out.println("注册处理器: " + action + " -> " + beanName);
            });
        }
    }
    
    /**
     * 注册处理器
     * @param action 指令名称
     * @param handler 处理器实例
     */
    public void registerHandler(String action, ActionHandler handler) {
        handlerMap.put(action.toUpperCase(), handler);
    }
    
    /**
     * 根据 action 获取处理器
     * @param action 指令名称
     * @return 对应的处理器，找不到返回 null
     */
    public ActionHandler getHandler(String action) {
        return handlerMap.get(action.toUpperCase());
    }
    
    /**
     * 获取所有已注册的处理器
     * @return 处理器映射表
     */
    public Map<String, ActionHandler> getAllHandlers() {
        return new ConcurrentHashMap<>(handlerMap);
    }
}
