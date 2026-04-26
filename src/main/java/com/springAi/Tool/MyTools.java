package com.springAi.Tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class MyTools {

    @Tool(description = "获取当前的日期和时间")
    public String getCurrentDateTime() {
        System.out.println("[Agent: 执行工具 - 获取时间]");
        return LocalDateTime.now().toString();
    }

    @Tool(description = "根据城市名称获取天气信息")
    public String getWeather(String city) {
        System.out.println("[Agent: 执行工具 - 获取城市: " + city + " 的天气]");
        return city + " 的天气是晴天，温度20°C。";
    }
}