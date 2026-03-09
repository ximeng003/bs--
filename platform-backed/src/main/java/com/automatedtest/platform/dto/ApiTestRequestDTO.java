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
    
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getBodyType() { return bodyType; }
    public void setBodyType(String bodyType) { this.bodyType = bodyType; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public List<HeaderDTO> getHeaders() { return headers; }
    public void setHeaders(List<HeaderDTO> headers) { this.headers = headers; }
    public List<ParamDTO> getParams() { return params; }
    public void setParams(List<ParamDTO> params) { this.params = params; }

    @Data
    public static class HeaderDTO {
        private String key;
        private String value;
        private boolean active;
        
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    @Data
    public static class ParamDTO {
        private String key;
        private String value;
        private boolean active;
        
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
