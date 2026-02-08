package com.automatedtest.platform.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ApiTestRequestDTO {
    private String method;
    private String url;
    private String bodyType; // json, form-data, etc.
    private String body;
    private List<HeaderDTO> headers;
    private List<ParamDTO> params;

    @Data
    public static class HeaderDTO {
        private String key;
        private String value;
        private boolean active;
    }

    @Data
    public static class ParamDTO {
        private String key;
        private String value;
        private boolean active;
    }
}
