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
}
