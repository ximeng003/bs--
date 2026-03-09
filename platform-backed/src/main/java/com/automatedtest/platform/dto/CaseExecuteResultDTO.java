package com.automatedtest.platform.dto;

import lombok.Data;
import java.util.Map;

@Data
public class CaseExecuteResultDTO {
    private String status;
    private Long durationMs;
    private String logs;
    private String error;
    private Map<String, Object> response;
    private Integer reportId;
    private Integer assertsTotal;
    private Integer assertsPassed;
    private Integer assertsFailed;
    private java.util.Map<String, String> extractedVars;
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getLogs() { return logs; }
    public void setLogs(String logs) { this.logs = logs; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Map<String, Object> getResponse() { return response; }
    public void setResponse(Map<String, Object> response) { this.response = response; }
    public Integer getReportId() { return reportId; }
    public void setReportId(Integer reportId) { this.reportId = reportId; }
    public Integer getAssertsTotal() { return assertsTotal; }
    public void setAssertsTotal(Integer assertsTotal) { this.assertsTotal = assertsTotal; }
    public Integer getAssertsPassed() { return assertsPassed; }
    public void setAssertsPassed(Integer assertsPassed) { this.assertsPassed = assertsPassed; }
    public Integer getAssertsFailed() { return assertsFailed; }
    public void setAssertsFailed(Integer assertsFailed) { this.assertsFailed = assertsFailed; }
    public java.util.Map<String, String> getExtractedVars() { return extractedVars; }
    public void setExtractedVars(java.util.Map<String, String> extractedVars) { this.extractedVars = extractedVars; }
}
