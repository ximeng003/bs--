package com.automatedtest.platform.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class WebhookClient {
    private static final Logger log = LoggerFactory.getLogger(WebhookClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendNotification(String webhookUrl, String title, String content) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return;
        }

        try {
            String lower = webhookUrl.toLowerCase();
            if (lower.contains("oapi.dingtalk.com")) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("msgtype", "markdown");
                Map<String, Object> markdown = new HashMap<>();
                markdown.put("title", title);
                markdown.put("text", "### " + title + "\n\n" + content);
                payload.put("markdown", markdown);
                restTemplate.postForObject(webhookUrl, payload, String.class);
            } else if (lower.contains("open.feishu.cn") || lower.contains("feishu.cn")) {
                Map<String, Object> payload = new HashMap<>();
                Map<String, Object> msg = new HashMap<>();
                msg.put("title", title);
                // Feishu interactive card or simple text; use simple text for compatibility
                payload.put("msg_type", "text");
                payload.put("content", new HashMap<String, Object>() {{
                    put("text", title + "\n\n" + content);
                }});
                restTemplate.postForObject(webhookUrl, payload, String.class);
            } else if (lower.contains("qyapi.weixin.qq.com")) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("msgtype", "markdown");
                Map<String, Object> markdown = new HashMap<>();
                markdown.put("content", "**" + title + "**\n\n" + content);
                payload.put("markdown", markdown);
                restTemplate.postForObject(webhookUrl, payload, String.class);
            } else {
                // Fallback: send plain text JSON
                Map<String, Object> payload = new HashMap<>();
                payload.put("title", title);
                payload.put("content", content);
                restTemplate.postForObject(webhookUrl, payload, String.class);
            }
            log.info("Notification sent to {}", webhookUrl);
        } catch (Exception e) {
            log.error("Failed to send notification", e);
        }
    }
}
