package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("test_reports")
public class TestReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("project_id")
    private Integer projectId;

    @TableField("plan_id")
    private Integer planId;

    @TableField("plan_run_no")
    private Integer planRunNo;

    @TableField("case_id")
    private Integer caseId;

    private String status; // success, failed

    @TableField("execution_time")
    private Integer executionTime;

    private String logs;

    @TableField(value = "executed_at", fill = FieldFill.INSERT)
    private LocalDateTime executedAt;

    @TableField("executed_by")
    private String executedBy;

    private String triggerType; // manual, schedule, openapi

    @TableField("asserts_total")
    private Integer assertsTotal;

    @TableField("asserts_passed")
    private Integer assertsPassed;

    @TableField("asserts_failed")
    private Integer assertsFailed;

    @TableField(exist = false)
    private String caseName;

    @TableField(exist = false)
    private String planName;

    @TableField(exist = false)
    private String environment;

    @TableField(exist = false)
    private String caseType;

    @TableField("is_deleted")
    private Boolean isDeleted;

    public Integer getCaseId() { return caseId; }
    public String getStatus() { return status; }
    public String getExecutedBy() { return executedBy; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public Integer getId() { return id; }
    public Integer getPlanId() { return planId; }
    public void setCaseName(String caseName) { this.caseName = caseName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public Integer getProjectId() { return projectId; }
    public Integer getExecutionTime() { return executionTime; }
    public String getLogs() { return logs; }
    public Integer getAssertsTotal() { return assertsTotal; }
    public Integer getAssertsPassed() { return assertsPassed; }
    public Integer getAssertsFailed() { return assertsFailed; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
    public Integer getPlanRunNo() { return planRunNo; }
    public void setPlanRunNo(Integer planRunNo) { this.planRunNo = planRunNo; }
    public void setCaseId(Integer caseId) { this.caseId = caseId; }
    public void setStatus(String status) { this.status = status; }
    public void setExecutionTime(Integer executionTime) { this.executionTime = executionTime; }
    public void setLogs(String logs) { this.logs = logs; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
    public void setAssertsTotal(Integer assertsTotal) { this.assertsTotal = assertsTotal; }
    public void setAssertsPassed(Integer assertsPassed) { this.assertsPassed = assertsPassed; }
    public void setAssertsFailed(Integer assertsFailed) { this.assertsFailed = assertsFailed; }
    public String getEnvironment() { return environment; }
    public void setCaseType(String caseType) { this.caseType = caseType; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
}
