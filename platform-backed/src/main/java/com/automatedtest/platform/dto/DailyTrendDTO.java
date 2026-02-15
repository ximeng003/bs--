package com.automatedtest.platform.dto;

import lombok.Data;

@Data
public class DailyTrendDTO {
    private String date; // "MM-dd"
    private long passed;
    private long failed;
    private long apiCount;
    private long webCount;
    private long appCount;
    private double apiPassRate;
    private double webPassRate;
    private double appPassRate;
}
