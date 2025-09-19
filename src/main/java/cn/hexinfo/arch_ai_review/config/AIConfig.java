package cn.hexinfo.arch_ai_review.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI配置类
 */
@Configuration
@EnableAsync
@EnableAspectJAutoProxy
public class AIConfig {
    
    @Autowired
    private ChatModel qwenChatModel;
    
    // 我们将使用SimpleAIService代替Spring AI的ChatClient
    // SimpleAIService已在cn.hexinfo.arch_ai_review.service.impl包中实现
}