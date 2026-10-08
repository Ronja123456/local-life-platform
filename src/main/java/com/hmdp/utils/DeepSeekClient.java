package com.hmdp.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DeepSeekClient {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String model;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 调用 DeepSeek 大模型
     */
    public String chat(String prompt) {
        String url = baseUrl + "/chat/completions";

        // 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);

// 构造 message
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

// 构造 messages 列表
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message);

        body.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            // 解析返回
            JSONObject json = JSON.parseObject(response.getBody());
            JSONArray choices = json.getJSONArray("choices");
            JSONObject msg = choices.getJSONObject(0).getJSONObject("message");
            return msg.getString("content");
        } catch (Exception e) {
            log.error("调用 DeepSeek 失败", e);
            return "抱歉，AI 服务暂时不可用";
        }
    }
    @Value("${deepseek.api-key}")


    @PostConstruct
    public void init() {
        // 只打印前 10 位，防止泄露
        String prefix = apiKey != null && apiKey.length() > 10
                ? apiKey.substring(0, 10) : "null或空";
        log.info("DeepSeek API Key 前缀：{}，长度：{}", prefix, apiKey == null ? 0 : apiKey.length());
        log.info("base-url：{}", baseUrl);
        log.info("model：{}", model);
    }

}
