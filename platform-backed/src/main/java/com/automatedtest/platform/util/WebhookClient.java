package com.automatedtest.platform.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class WebhookClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendNotification(String webhookUrl, String title, String content) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return;
        }

        try {
            // DingTalk format
            Map<String, Object> payload = new HashMap<>();
            payload.put("msgtype", "markdown");
            
            Map<String, Object> markdown = new HashMap<>();
            markdown.put("title", title);
            markdown.put("text", "### " + title + "\n\n" + content);
            
            payload.put("markdown", markdown);

            restTemplate.postForObject(webhookUrl, payload, String.class);
            log.info("Notification sent to {}", webhookUrl);
        } catch (Exception e) {
            log.error("Failed to send notification", e);
        }
    }
}
