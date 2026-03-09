package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String role;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    @TableField("notification_webhook")
    private String notificationWebhook;

    @TableField("enable_notification")
    private Boolean enableNotification;

    @TableField("notification_rule")
    private String notificationRule;

    @TableField("notification_threshold")
    private Integer notificationThreshold;

    @TableField("max_teams")
    private Integer maxTeams;

    @TableField("max_projects")
    private Integer maxProjects;

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    @TableField("last_login_ip")
    private String lastLoginIp;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    public String getRole() { return role; }
    public String getUsername() { return username; }
    public Long getId() { return id; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setRole(String role) { this.role = role; }
    public Integer getMaxTeams() { return maxTeams; }
    public String getNickname() { return nickname; }
    public String getAvatar() { return avatar; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    public Integer getMaxProjects() { return maxProjects; }
    public Boolean getEnableNotification() { return enableNotification; }
    public String getNotificationWebhook() { return notificationWebhook; }
    public String getNotificationRule() { return notificationRule; }
    public Integer getNotificationThreshold() { return notificationThreshold; }
    public void setMaxProjects(Integer maxProjects) { this.maxProjects = maxProjects; }
    public void setNotificationWebhook(String notificationWebhook) { this.notificationWebhook = notificationWebhook; }
    public void setEnableNotification(Boolean enableNotification) { this.enableNotification = enableNotification; }
    public void setNotificationRule(String notificationRule) { this.notificationRule = notificationRule; }
    public void setNotificationThreshold(Integer notificationThreshold) { this.notificationThreshold = notificationThreshold; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}
