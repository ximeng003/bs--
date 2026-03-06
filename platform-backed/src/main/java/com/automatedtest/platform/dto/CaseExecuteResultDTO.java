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
}
