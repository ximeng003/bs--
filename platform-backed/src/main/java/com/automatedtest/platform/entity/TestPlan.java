package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("test_plans")
public class TestPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String name;

    private String description;

    @TableField("cron_expression")
    private String cronExpression;

    /**
     * active, inactive
     */
    private String status;

    private String environment;

    @TableField("test_case_ids")
    private String testCaseIds;

    @TableField("project_id")
    private Integer projectId;

    @TableField("created_by")
    private Integer createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("last_run_status")
    private String lastRunStatus;

    @TableField("last_run_time")
    private LocalDateTime lastRunTime;
    
    @TableField("api_id")
    private String apiId;

    @TableField("allow_open_api")
    private Boolean allowOpenApi;
    
    @TableField("flow_json")
    private String flowJson;

    public String getEnvironment() { return environment; }
    public Integer getId() { return id; }
    public String getName() { return name; }
    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public String getTestCaseIds() { return testCaseIds; }
    public void setLastRunStatus(String lastRunStatus) { this.lastRunStatus = lastRunStatus; }
    public void setLastRunTime(LocalDateTime lastRunTime) { this.lastRunTime = lastRunTime; }
}
