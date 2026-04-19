package com.hmdp.config;

import com.hmdp.ai.AiAssistant;
import com.hmdp.ai.AiTools;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * AI 配置类 (通义千问 - OpenAI 兼容模式)
 */
@Slf4j
@Configuration
public class AiConfiguration {

    @Value("${langchain4j.open-ai.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.model-name}")
    private String modelName;

    @Resource
    private AiTools aiTools;

    /**
     * 创建 ChatLanguageModel Bean
     * 使用 OpenAI 兼容模式连接通义千问
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化 AI 模型: baseUrl={}, modelName={}", baseUrl, modelName);

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.7)
                .maxTokens(2000)
                .build();
    }

    /**
     * 创建 AiAssistant Bean
     * 将 ChatLanguageModel 和 Tools 绑定到 AI 服务
     */
    @Bean
    public AiAssistant aiAssistant(ChatLanguageModel chatLanguageModel) {
        log.info("初始化 AiAssistant, 绑定工具类");
        return AiServices.builder(AiAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(aiTools)
                .build();
    }
}
