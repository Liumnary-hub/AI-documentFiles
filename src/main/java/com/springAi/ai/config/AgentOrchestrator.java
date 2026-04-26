package com.springAi.ai.config;

import com.springAi.ai.Tool.MyTools;
import com.springAi.ai.repository.ChatMessageRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentOrchestrator {

    private final ChatClient chatClient;
    private final MyTools myTools;
    private final ChatMessageRepository messageRepo;   // 新增

    public AgentOrchestrator(ChatClient.Builder builder, MyTools myTools, ChatMessageRepository messageRepo) {
        this.chatClient = builder.build();
        this.myTools = myTools;
        this.messageRepo = messageRepo;
    }

    public String chat(String userMessage, String chatId) {
        // 1. 保存用户消息
        messageRepo.saveMessage(chatId, "user", userMessage);

        StringBuilder history = new StringBuilder();
        history.append("用户：").append(userMessage).append("\n");
        int maxSteps = 5;
        int step = 0;
        String finalAnswer = null;

        while (step < maxSteps) {
            step++;
            String response = chatClient.prompt()
                    .user(getPromptWithHistory(history.toString()))
                    .call()
                    .content();

            System.out.println("模型输出：\n" + response);

            if (response.contains("最终答案：")) {
                finalAnswer = extractFinalAnswer(response);
                break;
            }

            String action = extractAction(response);
            if (action != null) {
                String toolResult = executeTool(action);
                history.append("助手：").append(response).append("\n");
                history.append("工具结果：").append(toolResult).append("\n");
            } else {
                history.append("助手：").append(response).append("\n");
            }
        }

        if (finalAnswer == null) {
            finalAnswer = "抱歉，任务未能完成。";
        }

        // 2. 保存助手回复
        messageRepo.saveMessage(chatId, "assistant", finalAnswer);

        return finalAnswer;
    }

    private String getPromptWithHistory(String history) {
        return """
            你是一个只有两种输出格式的机器。不允许说任何废话。
            
            如果用户需要实时信息，你**必须**输出一行：
            行动：工具名(参数)
            工具名只能是 getCurrentDateTime 或 getWeather。
            
            如果你已经可以回答问题，你**必须**输出一行：
            最终答案：你的回答
            
            不要输出“好的”、“我可以”、“让我们想想”等任何多余文字。
            不要重复输出同一句话。
            
            现在，请立刻根据以下对话输出一行：
            """ + history + "\n";
    }

    private String extractAction(String response) {
        Pattern pattern = Pattern.compile("行动：([a-zA-Z]+)\\(([^)]*)\\)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1) + "(" + matcher.group(2) + ")";
        }
        return null;
    }

    private String executeTool(String action) {
        // 解析 getWeather(北京)
        if (action.startsWith("getWeather(")) {
            String city = action.substring(11, action.length() - 1); // 提取参数
            return myTools.getWeather(city);
        } else if (action.startsWith("getCurrentDateTime")) {
            return myTools.getCurrentDateTime();  // 无参方法
        }
        return "未知工具：" + action;
    }

    private String extractFinalAnswer(String response) {
        int idx = response.indexOf("最终答案：");
        if (idx != -1) {
            return response.substring(idx + 5).trim();
        }
        return response;
    }
}