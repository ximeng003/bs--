package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_variables")
public class UserVariable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String keyName;
    private String value;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public String getKeyName() { return keyName; }
    public String getValue() { return value; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setValue(String value) { this.value = value; }
    public String getDescription() { return description; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setDescription(String description) { this.description = description; }
}
