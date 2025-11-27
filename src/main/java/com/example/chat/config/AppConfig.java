package com.example.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration // 告诉 Spring 这是一个配置类
public class AppConfig {

    // 1. 注册 JSON 工具，方便在任何地方使用
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // 2. 配置线程池 (ChatExecutor)
    @Bean(name = "chatExecutor")
    public Executor chatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：平时保留 10 个工人
        executor.setCorePoolSize(10);
        // 最大线程数：忙的时候招到 20 个
        executor.setMaxPoolSize(20);
        // 队列容量：如果工人都在忙，让 200 个任务排队
        executor.setQueueCapacity(200);
        // 线程名前缀，方便查日志
        executor.setThreadNamePrefix("Chat-Worker-");

        // 拒绝策略：如果队伍也排满了，由调用者自己去跑（CallerRunsPolicy），保证不丢消息
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}