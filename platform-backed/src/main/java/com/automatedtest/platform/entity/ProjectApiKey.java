package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("project_api_keys")
public class ProjectApiKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("project_id")
    private Integer projectId;

    @TableField("user_id")
    private Integer userId;

    @TableField("api_key")
    private String apiKey;

    private String description;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
    
    public Integer getProjectId() { return projectId; }
    public Integer getUserId() { return userId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setDescription(String description) { this.description = description; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
