package com.automatedtest.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportDetailDTO {
    private Integer id;
    private Integer planId;
    private Integer planRunNo;
    private Integer caseId;
    private String caseName;
    private String caseType;
    private String status;
    private Integer executionTime;
    private String logs;
    private LocalDateTime executedAt;
    private String executedBy;
    private String triggerType;
    private String environment;
    private Integer assertsTotal;
    private Integer assertsPassed;
    private Integer assertsFailed;
    
    public void setCaseId(Integer caseId) { this.caseId = caseId; }
    public void setStatus(String status) { this.status = status; }
    public void setExecutionTime(Integer executionTime) { this.executionTime = executionTime; }
    public void setLogs(String logs) { this.logs = logs; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
    public void setCaseName(String caseName) { this.caseName = caseName; }
    public void setCaseType(String caseType) { this.caseType = caseType; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public void setId(Integer id) { this.id = id; }
    public void setPlanId(Integer planId) { this.planId = planId; }
    public void setPlanRunNo(Integer planRunNo) { this.planRunNo = planRunNo; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public void setAssertsTotal(Integer assertsTotal) { this.assertsTotal = assertsTotal; }
    public void setAssertsPassed(Integer assertsPassed) { this.assertsPassed = assertsPassed; }
    public void setAssertsFailed(Integer assertsFailed) { this.assertsFailed = assertsFailed; }
}
