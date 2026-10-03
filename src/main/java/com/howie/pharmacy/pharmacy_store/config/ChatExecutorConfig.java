package com.howie.pharmacy.pharmacy_store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ChatExecutorConfig {

    @Bean("chatTaskExecutor")
    public ThreadPoolTaskExecutor chatTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(40);
        executor.setThreadNamePrefix("pharmacy-chat-");
        return executor;
    }
}