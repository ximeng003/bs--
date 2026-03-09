package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("project_variables")
public class ProjectVariable implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("project_id")
    private Integer projectId;

    @TableField("key_name")
    private String keyName;

    private String value;

    private String description;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    public String getKeyName() { return keyName; }
    public String getValue() { return value; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public Integer getId() { return id; }
    public Integer getProjectId() { return projectId; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setValue(String value) { this.value = value; }
    public String getDescription() { return description; }
    public void setKeyName(String keyName) { this.keyName = keyName; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setDescription(String description) { this.description = description; }
}
