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
    private String environment;
}
