package com.automatedtest.platform.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecentActivityDTO {
    private String status; // success, failed
    private String caseName;
    private String executedBy;
    private String timeAgo; // e.g. "5分钟前"
    private LocalDateTime timestamp;
    
    public void setCaseName(String caseName) { this.caseName = caseName; }
    public void setStatus(String status) { this.status = status; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
