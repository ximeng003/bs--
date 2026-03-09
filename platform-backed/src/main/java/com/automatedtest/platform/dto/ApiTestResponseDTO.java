package com.automatedtest.platform.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ApiTestResponseDTO {
    private int statusCode;
    private long time; // ms
    private Map<String, String> headers;
    private String body;
    private String error; // if any exception occurs
    
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
