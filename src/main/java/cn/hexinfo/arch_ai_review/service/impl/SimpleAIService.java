package cn.hexinfo.arch_ai_review.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单的AI服务实现，直接调用通义千问API
 */
@Service
@Slf4j
public class SimpleAIService {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Value("${spring.ai.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${spring.ai.dashscope.chat.options.model:qwen3-0.6b}")
    private String model;

    @Value("${spring.ai.dashscope.chat.options.temperature:0.7}")
    private double temperature;

    @Value("${spring.ai.dashscope.chat.options.max-tokens:2000}")
    private int maxTokens;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 调用通义千问API
     * 
     * @param prompt 提示词
     * @return AI响应内容
     */
    public String callQwenApi(String prompt) {
        // 参数验证
        if (prompt == null || prompt.trim().isEmpty()) {
            log.error("提示词为空");
            throw new IllegalArgumentException("提示词不能为空");
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("API密钥未配置");
            throw new IllegalArgumentException("API密钥未正确配置，请检查application.properties中的spring.ai.qwen.api-key配置");
        }
        
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            log.error("API基础URL未配置");
            throw new IllegalArgumentException("API基础URL未正确配置，请检查application.properties中的spring.ai.qwen.base-url配置");
        }
        
        if (model == null || model.trim().isEmpty()) {
            log.warn("模型名称未配置，使用默认值qwen3-0.6b");
            model = "qwen3-0.6b";
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 构建符合OpenAI兼容模式的请求体
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            Map<String, Object> parameters = new HashMap<>();
            
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            
            // 设置参数
            requestBody.put("temperature", 0.2); // 降低温度以获得更确定性的输出
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("enable_thinking", false); // 必须设置为false，否则会报错
            
            // 确保返回JSON格式
            Map<String, String> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = baseUrl + "/chat/completions";

            log.debug("发送到通义千问API的请求: {}", objectMapper.writeValueAsString(requestBody));
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            String responseBody = response.getBody();
            log.debug("通义千问API的原始响应: {}", responseBody);
            
            // 解析API响应，添加安全检查
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            
            if (responseMap == null) {
                throw new RuntimeException("API响应为空");
            }
            
            // 检查是否有错误信息
            if (responseMap.containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) responseMap.get("error");
                String errorType = String.valueOf(error.get("type"));
                String errorMessage = String.valueOf(error.get("message"));
                throw new RuntimeException("API错误: " + errorType + " - " + errorMessage);
            }
            
            // 检查choices字段
            if (!responseMap.containsKey("choices") || responseMap.get("choices") == null) {
                log.error("API响应中没有choices字段: {}", responseBody);
                throw new RuntimeException("API响应格式错误: 缺少choices字段");
            }
            
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            
            if (choices.isEmpty()) {
                log.error("API响应中choices为空列表");
                throw new RuntimeException("API响应格式错误: choices为空列表");
            }
            
            Map<String, Object> choice = choices.get(0);
            
            // 检查message字段
            if (!choice.containsKey("message") || choice.get("message") == null) {
                log.error("API响应中没有message字段: {}", choice);
                
                // 尝试直接从choice中获取文本
                if (choice.containsKey("text")) {
                    String text = (String) choice.get("text");
                    if (text != null) {
                        log.debug("从choice.text字段获取内容: {}", text);
                        return preprocessJsonContent(text);
                    }
                }
                
                throw new RuntimeException("API响应格式错误: 缺少message字段");
            }
            
            Map<String, Object> messageMap = (Map<String, Object>) choice.get("message");
            
            // 检查content字段
            if (!messageMap.containsKey("content") || messageMap.get("content") == null) {
                log.error("API响应中没有content字段: {}", messageMap);
                throw new RuntimeException("API响应格式错误: 缺少content字段");
            }
            
            String content = (String) messageMap.get("content");
            
            // 尝试预处理内容，确保是有效的JSON
            if (content != null) {
                // 检查是否是数字值
                if (content.matches("-?\\d+(\\.\\d+)?")) {
                    log.warn("API返回了数字值而非JSON: {}", content);
                    // 构造一个基本的JSON响应
                    content = String.format("""
                        {
                            "score": 0,
                            "status": "NEED_REVIEW",
                            "issues": [
                                {
                                    "type": "CONTENT",
                                    "severity": "HIGH",
                                    "title": "AI评审失败",
                                    "description": "AI返回了非预期的响应: %s",
                                    "suggestion": "请手动评审文档"
                                }
                            ],
                            "summary": "AI评审失败，请手动评审"
                        }
                        """, content);
                } else {
                    content = preprocessJsonContent(content);
                }
            } else {
                throw new RuntimeException("API响应中content为null");
            }
            
            log.debug("通义千问API的处理后内容响应: {}", content);

            return content;
        } catch (Exception e) {
            log.error("调用通义千问API失败", e);
            throw new RuntimeException("调用通义千问API失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 预处理JSON内容，尝试移除非JSON部分
     */
    private String preprocessJsonContent(String content) {
        log.debug("预处理前的内容: {}", content);
        
        // 检查是否包含Markdown代码块
        if (content.contains("```")) {
            // 尝试提取JSON代码块
            Pattern pattern = Pattern.compile("```(?:json)?([\\s\\S]*?)```");
            Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                // 提取代码块内容
                String jsonContent = matcher.group(1).trim();
                log.debug("从Markdown代码块提取的JSON: {}", jsonContent);
                return jsonContent;
            }
        }
        
        // 查找JSON开始的位置
        int jsonStart = content.indexOf('{');
        if (jsonStart >= 0) {
            // 查找匹配的结束括号
            int depth = 1;
            int jsonEnd = -1;
            
            for (int i = jsonStart + 1; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        jsonEnd = i;
                        break;
                    }
                }
            }
            
            if (jsonEnd > jsonStart) {
                String jsonContent = content.substring(jsonStart, jsonEnd + 1);
                log.debug("提取的JSON对象: {}", jsonContent);
                return jsonContent;
            }
        }
        
        // 如果没有找到有效的JSON，返回原始内容
        return content;
    }
}
