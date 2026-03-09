package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("environments")
public class Environment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String name;
    
    @TableField("key_name")
    private String keyName; // 'dev', 'staging', 'production'
    
    @TableField("base_url")
    private String baseUrl;
    
    private String databaseName;
    
    private Boolean active;

    @TableField("project_id")
    private Integer projectId;
    
    public String getBaseUrl() { return baseUrl; }
    public String getDatabaseName() { return databaseName; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public Integer getId() { return id; }
    public Integer getProjectId() { return projectId; }
}
