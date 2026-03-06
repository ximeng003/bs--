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
}
