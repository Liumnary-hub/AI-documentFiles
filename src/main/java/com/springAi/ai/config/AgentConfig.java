//package com.springAi.ai.config;
//
//import com.alibaba.cloud.ai.graph.agent.ReactAgent;
//import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
//import com.springAi.ai.Tool.MyTools;
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.ai.tool.ToolCallback;
//import org.springframework.ai.tool.function.FunctionToolCallback;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class AgentConfig {
//
//    @Bean
//    public ReactAgent myReactAgent(ChatModel chatModel, MyTools myTools) {
//
//        // ✅ 关键修复：无参工具——改为接收 String 参数，忽略它，避免 JSON 格式错误
//        // 前提：需要修改 MyTools.getCurrentDateTime(String dummy)
//        ToolCallback dateCallback = FunctionToolCallback.builder("getCurrentDateTime", myTools::getCurrentDateTime)
//                .description("获取当前日期和时间（参数随意，可传空字符串）")
//                .inputType(String.class)   // 使用 String.class 代替 Void.class
//                .build();
//
//        ToolCallback weatherCallback = FunctionToolCallback.builder("getWeather", myTools::getWeather)
//                .description("根据城市名称获取天气信息")
//                .inputType(String.class)
//                .build();
//
//        return ReactAgent.builder()
//                .name("my_agent")          // 必须指定名称
//                .model(chatModel)
//                .tools(dateCallback, weatherCallback)
//                .systemPrompt("你是一个智能助手，可以调用工具获取实时信息。")
//                .saver(new MemorySaver())   // 添加记忆
//                .build();
//    }
//}