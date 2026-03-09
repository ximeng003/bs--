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
    
    public void setDate(String date) { this.date = date; }
    public void setPassed(long passed) { this.passed = passed; }
    public void setFailed(long failed) { this.failed = failed; }
    public void setApiCount(long apiCount) { this.apiCount = apiCount; }
    public void setWebCount(long webCount) { this.webCount = webCount; }
    public void setAppCount(long appCount) { this.appCount = appCount; }
    public void setApiPassRate(double apiPassRate) { this.apiPassRate = apiPassRate; }
    public void setWebPassRate(double webPassRate) { this.webPassRate = webPassRate; }
    public void setAppPassRate(double appPassRate) { this.appPassRate = appPassRate; }
    public long getPassed() { return passed; }
    public long getFailed() { return failed; }
    public String getDate() { return date; }
}
