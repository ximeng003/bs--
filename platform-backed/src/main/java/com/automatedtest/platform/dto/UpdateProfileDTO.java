package com.automatedtest.platform.dto;

import lombok.Data;

@Data
public class UpdateProfileDTO {
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private String notificationWebhook;
    private Boolean enableNotification;
    private String notifyRule;
    private Integer notifyThreshold;
    
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatar() { return avatar; }
    public String getNotificationWebhook() { return notificationWebhook; }
    public Boolean getEnableNotification() { return enableNotification; }
    public String getNotifyRule() { return notifyRule; }
    public Integer getNotifyThreshold() { return notifyThreshold; }
}
