package com.automatedtest.platform.dto;

import lombok.Data;

@Data
public class DailyTrendDTO {
    private String date; // "MM-dd"
    private long passed;
    private long failed;
}
