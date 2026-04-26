package com.springAi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * 修复 Spring AI Alibaba 1.1.2.0 缺少 ResponseErrorHandler Bean 的BUG
 */
@Configuration
public class AiBeanConfig {

    // 手动注入一个 Spring 默认的异常处理器，解决启动报错
    @Bean
    @Primary
    public ResponseErrorHandler responseErrorHandler() {
        return new DefaultResponseErrorHandler();
    }
}