package com.automatedtest.platform.dto;

import lombok.Data;

@Data
public class RecentActivityDTO {
    private String status; // success, failed
    private String caseName;
    private String executedBy;
    private String timeAgo; // e.g. "5分钟前"
}
