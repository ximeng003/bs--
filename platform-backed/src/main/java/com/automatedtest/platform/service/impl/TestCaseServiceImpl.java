package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.dto.ApiTestRequestDTO;
import com.automatedtest.platform.dto.ApiTestResponseDTO;
import com.automatedtest.platform.dto.CaseExecuteResultDTO;
import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.entity.TestReport;
import com.automatedtest.platform.mapper.TestCaseMapper;
import com.automatedtest.platform.service.TestCaseService;
import com.automatedtest.platform.service.TestReportService;
import com.automatedtest.platform.entity.User;
import com.automatedtest.platform.entity.UserVariable;
import com.automatedtest.platform.service.UserService;
import com.automatedtest.platform.service.UserVariableService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TestCaseServiceImpl extends ServiceImpl<TestCaseMapper, TestCase> implements TestCaseService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestReportService testReportService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserVariableService userVariableService;

    @Autowired
    private com.automatedtest.platform.service.SystemVariableService systemVariableService;

    @Autowired
    private com.automatedtest.platform.service.ProjectVariableService projectVariableService;
    
    @Autowired
    private com.automatedtest.platform.service.EnvironmentService environmentService;
    
    @Autowired
    private com.automatedtest.platform.service.TestPlanService testPlanService;

    @Override
    public ApiTestResponseDTO executeApiTest(ApiTestRequestDTO request) {
        return executeApiTest(request, null);
    }

    @Override
    public ApiTestResponseDTO executeApiTest(ApiTestRequestDTO request, Long userId) {
        return executeApiTest(request, userId, null);
    }

    @Override
    public ApiTestResponseDTO executeApiTest(ApiTestRequestDTO request, Long userId, Integer projectId) {
        ApiTestResponseDTO responseDTO = new ApiTestResponseDTO();
        long startTime = System.currentTimeMillis();

        try {
            // Variable Substitution
            Map<String, String> varMap = new HashMap<>();

            // 1. System Variables (Global Level)
            List<com.automatedtest.platform.entity.SystemVariable> systemVars = systemVariableService.list();
            if (systemVars != null) {
                for (com.automatedtest.platform.entity.SystemVariable var : systemVars) {
                    if (var.getKeyName() != null && var.getValue() != null) {
                        varMap.put(var.getKeyName(), var.getValue());
                    }
                }
            }

            // 2. Project Variables (Project Level - Override System)
            if (projectId != null) {
                List<com.automatedtest.platform.entity.ProjectVariable> projectVars = projectVariableService.list(
                        new QueryWrapper<com.automatedtest.platform.entity.ProjectVariable>().eq("project_id", projectId)
                );
                if (projectVars != null) {
                    for (com.automatedtest.platform.entity.ProjectVariable var : projectVars) {
                        if (var.getKeyName() != null && var.getValue() != null) {
                            varMap.put(var.getKeyName(), var.getValue());
                        }
                    }
                }
            }

            // 3. User Variables (Personal Level - Override Project/System)
            if (userId != null) {
                List<UserVariable> variables = userVariableService.list(new QueryWrapper<UserVariable>().eq("user_id", userId));
                if (variables != null) {
                    for (UserVariable var : variables) {
                        if (var.getKeyName() != null && var.getValue() != null) {
                            varMap.put(var.getKeyName(), var.getValue());
                        }
                    }
                }
            }
            
            if (projectId != null) {
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.automatedtest.platform.entity.Environment> ew =
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.automatedtest.platform.entity.Environment>()
                                .eq("project_id", projectId)
                                .eq("active", true)
                                .orderByDesc("id")
                                .last("LIMIT 1");
                com.automatedtest.platform.entity.Environment env = environmentService.getOne(ew);
                if (env != null) {
                    if (env.getBaseUrl() != null) {
                        varMap.put("baseUrl", env.getBaseUrl());
                    }
                    if (env.getDatabaseName() != null) {
                        varMap.put("databaseName", env.getDatabaseName());
                    }
                }
            }

            // Apply Variables
            if (!varMap.isEmpty()) {
                String json = objectMapper.writeValueAsString(request);
                for (Map.Entry<String, String> entry : varMap.entrySet()) {
                    String val = entry.getValue()
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r");
                    // Support {{KEY}} syntax
                    json = json.replace("{{" + entry.getKey() + "}}", val);
                }
                request = objectMapper.readValue(json, ApiTestRequestDTO.class);
            }

            // 1. Build URL with params
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(request.getUrl());
            if (request.getParams() != null) {
                for (ApiTestRequestDTO.ParamDTO param : request.getParams()) {
                    if (param.isActive() && param.getKey() != null && !param.getKey().isEmpty()) {
                        builder.queryParam(param.getKey(), param.getValue());
                    }
                }
            }
            java.net.URI finalUrl = builder.build().toUri();

            // 2. Build Headers
            HttpHeaders headers = new HttpHeaders();
            if (request.getHeaders() != null) {
                for (ApiTestRequestDTO.HeaderDTO header : request.getHeaders()) {
                    if (header.isActive() && header.getKey() != null && !header.getKey().isEmpty()) {
                        headers.add(header.getKey(), header.getValue());
                    }
                }
            }

            // 3. Build Body
            HttpEntity<?> entity;
            if (HttpMethod.GET.matches(request.getMethod())) {
                entity = new HttpEntity<>(headers);
            } else {
                entity = new HttpEntity<>(request.getBody(), headers);
            }

            // 4. Execute
            HttpMethod method = HttpMethod.valueOf(request.getMethod().toUpperCase());
            // Use byte[] to avoid default StringHttpMessageConverter encoding issues (ISO-8859-1)
            ResponseEntity<byte[]> response = restTemplate.exchange(finalUrl, method, entity, byte[].class);

            // 5. Process Response
            responseDTO.setStatusCode(response.getStatusCodeValue());
            if (response.getBody() != null) {
                responseDTO.setBody(new String(response.getBody(), StandardCharsets.UTF_8));
            }
            
            Map<String, String> responseHeaders = new HashMap<>();
            response.getHeaders().forEach((k, v) -> responseHeaders.put(k, String.join(";", v)));
            responseDTO.setHeaders(responseHeaders);

        } catch (Exception e) {
            responseDTO.setError(e.getMessage());
            // Check if it's a RestTemplate exception to extract status code if available
            if (e instanceof org.springframework.web.client.HttpStatusCodeException) {
                org.springframework.web.client.HttpStatusCodeException he = (org.springframework.web.client.HttpStatusCodeException) e;
                responseDTO.setStatusCode(he.getRawStatusCode());
                responseDTO.setBody(he.getResponseBodyAsString());
            } else {
                responseDTO.setStatusCode(500); // Internal Error or Connection Error
            }
        } finally {
            responseDTO.setTime(System.currentTimeMillis() - startTime);
        }

        // Redact secrets from response body using variables map (best effort)
        String body = responseDTO.getBody();
        if (body != null) {
            responseDTO.setBody(redactSecrets(body, new HashMap<>())); // lightweight for API ad-hoc
        }
        return responseDTO;
    }

    @Override
    public CaseExecuteResultDTO executeCaseById(Integer id, String executedBy) {
        return executeCaseById(id, executedBy, null);
    }

    @Override
    public CaseExecuteResultDTO executeCaseById(Integer id, String executedBy, Integer planId) {
        return executeCaseById(id, executedBy, planId, null);
    }

    @Override
    public CaseExecuteResultDTO executeCaseById(Integer id, String executedBy, Integer planId, Integer planRunNo) {
        return executeCaseById(id, executedBy, planId, planRunNo, null);
    }

    @Override
    public CaseExecuteResultDTO executeCaseById(Integer id, String executedBy, Integer planId, Integer planRunNo, String triggerType) {
        CaseExecuteResultDTO result = new CaseExecuteResultDTO();
        TestCase testCase = getById(id);
        if (testCase == null) {
            result.setStatus("failed");
            result.setError("测试用例不存在");
            return result;
        }
        
        // Mark as running for dashboard visibility
        testCase.setLastResult("running");
        testCase.setLastRun(LocalDateTime.now());
        updateById(testCase);
        
        try {
            return executeCaseInternal(testCase, executedBy, planId, planRunNo, triggerType, null);
        } finally {
            // No need to clear here as executeCaseInternal will update it to success/failed
        }
    }
    
    @Override
    public CaseExecuteResultDTO executeCaseByIdWithVariables(Integer id, String executedBy, java.util.Map<String, String> variables) {
        CaseExecuteResultDTO result = new CaseExecuteResultDTO();
        TestCase testCase = getById(id);
        if (testCase == null) {
            result.setStatus("failed");
            result.setError("测试用例不存在");
            return result;
        }
        testCase.setLastResult("running");
        testCase.setLastRun(LocalDateTime.now());
        updateById(testCase);
        try {
            return executeCaseInternal(testCase, executedBy, null, null, "manual", variables);
        } finally {
        }
    }

    private CaseExecuteResultDTO executeCaseInternal(TestCase testCase, String executedBy, Integer planId, Integer planRunNo, String triggerType, Map<String, String> extraVars) {
        CaseExecuteResultDTO result = new CaseExecuteResultDTO();
        Map<String, Object> contentMap = new HashMap<>();
        String content = testCase.getContent();
        
        // Variable Substitution
        Map<String, String> varMap = new HashMap<>();

        // 1. System Variables (Global Level)
        List<com.automatedtest.platform.entity.SystemVariable> systemVars = systemVariableService.list();
        if (systemVars != null) {
            for (com.automatedtest.platform.entity.SystemVariable var : systemVars) {
                if (var.getKeyName() != null && var.getValue() != null) {
                    varMap.put(var.getKeyName(), var.getValue());
                }
            }
        }
        
        // 2. Project Variables (Project Level - Override System)
        if (testCase.getProjectId() != null) {
            List<com.automatedtest.platform.entity.ProjectVariable> projectVars = projectVariableService.list(
                new QueryWrapper<com.automatedtest.platform.entity.ProjectVariable>().eq("project_id", testCase.getProjectId())
            );
            if (projectVars != null) {
                for (com.automatedtest.platform.entity.ProjectVariable var : projectVars) {
                    if (var.getKeyName() != null && var.getValue() != null) {
                        varMap.put(var.getKeyName(), var.getValue());
                    }
                }
            }
        }

        // 3. User Variables (Private Level - Override Project & System)
        if (executedBy != null && !executedBy.trim().isEmpty()) {
             User user = userService.getOne(new QueryWrapper<User>().eq("username", executedBy));
             if (user != null) {
                 List<UserVariable> variables = userVariableService.list(new QueryWrapper<UserVariable>().eq("user_id", user.getId()));
                 if (variables != null) {
                     for (UserVariable var : variables) {
                         if (var.getKeyName() != null && var.getValue() != null) {
                             varMap.put(var.getKeyName(), var.getValue());
                         }
                     }
                 }
             }
        }
        
        String envKey = null;
        if (planId != null) {
            com.automatedtest.platform.entity.TestPlan plan = testPlanService.getById(planId);
            if (plan != null && plan.getEnvironment() != null && !plan.getEnvironment().trim().isEmpty()) {
                envKey = plan.getEnvironment().trim();
            }
        }
        if (envKey == null || envKey.trim().isEmpty()) {
            envKey = testCase.getEnvironment();
        }
        if (envKey != null && !envKey.trim().isEmpty() && testCase.getProjectId() != null) {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.automatedtest.platform.entity.Environment> ew =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.automatedtest.platform.entity.Environment>()
                            .eq("project_id", testCase.getProjectId())
                            .eq("key_name", envKey)
                            .eq("active", true);
            com.automatedtest.platform.entity.Environment env = environmentService.getOne(ew);
            if (env != null) {
                if (env.getBaseUrl() != null) {
                    varMap.put("baseUrl", env.getBaseUrl());
                }
                if (env.getDatabaseName() != null) {
                    varMap.put("databaseName", env.getDatabaseName());
                }
            }
        }
        if (extraVars != null && !extraVars.isEmpty()) {
            varMap.putAll(extraVars);
        }
        // 4. Apply Substitution
        if (!varMap.isEmpty() && content != null) {
             for (Map.Entry<String, String> entry : varMap.entrySet()) {
                 String val = entry.getValue().replace("\\", "\\\\").replace("\"", "\\\"");
                 content = content.replace("{{" + entry.getKey() + "}}", val);
             }
        }
        
        if (content != null && content.trim().length() > 0) {
            try {
                contentMap = objectMapper.readValue(content, Map.class);
                if ("API".equalsIgnoreCase(testCase.getType())) {
                    Object bodyObj = contentMap.get("body");
                    if (bodyObj instanceof String) {
                        String bodyStr = ((String) bodyObj).trim();
                        if (bodyStr.startsWith("{") || bodyStr.startsWith("[")) {
                            try {
                                Object bodyJson = objectMapper.readValue(bodyStr, Object.class);
                                contentMap.put("body", bodyJson);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            } catch (Exception e) {
                contentMap.put("script", content);
            }
        }
        
        // Self-Healing: add fallback selectors for WEB/APP steps
        try {
            if (( "WEB".equalsIgnoreCase(testCase.getType()) || "APP".equalsIgnoreCase(testCase.getType()) ) && contentMap != null) {
                Object sh = contentMap.get("selfHealing");
                boolean selfHealing = (sh == null) || (sh instanceof Boolean && ((Boolean) sh));
                if (selfHealing) {
                    Object stepsObj = contentMap.get("steps");
                    if (stepsObj instanceof java.util.List) {
                        java.util.List<?> steps = (java.util.List<?>) stepsObj;
                        for (Object s : steps) {
                            if (!(s instanceof java.util.Map)) continue;
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> step = (java.util.Map<String, Object>) s;
                            String by = step.get("by") != null ? step.get("by").toString() : null;
                            String val = step.get("value") != null ? step.get("value").toString() : null;
                            java.util.List<java.util.Map<String, String>> fallbacks = new java.util.ArrayList<>();
                            if (val != null && !val.trim().isEmpty()) {
                                if ("id".equalsIgnoreCase(by)) {
                                    java.util.Map<String, String> f1 = new java.util.HashMap<>();
                                    f1.put("by", "xpath");
                                    f1.put("value", "//*[@id='" + val + "']");
                                    fallbacks.add(f1);
                                    java.util.Map<String, String> f2 = new java.util.HashMap<>();
                                    f2.put("by", "css");
                                    f2.put("value", "#" + val);
                                    fallbacks.add(f2);
                                } else if ("class".equalsIgnoreCase(by)) {
                                    java.util.Map<String, String> f1 = new java.util.HashMap<>();
                                    f1.put("by", "xpath");
                                    f1.put("value", "//*[contains(@class,'" + val + "')]");
                                    fallbacks.add(f1);
                                    java.util.Map<String, String> f2 = new java.util.HashMap<>();
                                    f2.put("by", "css");
                                    f2.put("value", "." + val);
                                    fallbacks.add(f2);
                                } else if ("accessibility_id".equalsIgnoreCase(by)) {
                                    java.util.Map<String, String> f1 = new java.util.HashMap<>();
                                    f1.put("by", "xpath");
                                    f1.put("value", "//*[@content-desc='" + val + "']");
                                    fallbacks.add(f1);
                                } else if ("xpath".equalsIgnoreCase(by)) {
                                    // Heuristic: also try text contains
                                    java.util.Map<String, String> f1 = new java.util.HashMap<>();
                                    f1.put("by", "xpath");
                                    f1.put("value", "//*[contains(text(),'" + val.replace("'", "\\'") + "')]");
                                    fallbacks.add(f1);
                                } else {
                                    java.util.Map<String, String> f1 = new java.util.HashMap<>();
                                    f1.put("by", "xpath");
                                    f1.put("value", "//*[contains(text(),'" + val.replace("'", "\\'") + "')]");
                                    fallbacks.add(f1);
                                }
                            }
                            if (!fallbacks.isEmpty()) {
                                step.put("fallbacks", fallbacks);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        try {
            com.automatedtest.platform.engine.ExecutionEngine engine;
            String t = testCase.getType() != null ? testCase.getType().toUpperCase() : "API";
            if ("WEB".equals(t)) {
                engine = new com.automatedtest.platform.engine.WebExecutionEngine();
            } else if ("APP".equals(t)) {
                engine = new com.automatedtest.platform.engine.AppExecutionEngine();
            } else {
                engine = new com.automatedtest.platform.engine.ApiExecutionEngine();
            }
            result = engine.run(testCase, contentMap, envKey, varMap);
        } catch (Exception e) {
            result.setStatus("failed");
            result.setError(e.getMessage());
        }

        boolean success = "success".equalsIgnoreCase(result.getStatus());
        testCase.setLastRun(LocalDateTime.now());
        testCase.setLastResult(success ? "success" : "failed");
        updateById(testCase);

        // Extract dynamic variables from response
        try {
            if (contentMap != null) {
                Object extractObj = contentMap.get("extract");
                if (extractObj instanceof java.util.List) {
                    java.util.List<?> exts = (java.util.List<?>) extractObj;
                    java.util.Map<String, String> planVars = new java.util.HashMap<>();
                    Object responseObj = result.getResponse(); // may be Map<String,Object>
                    java.util.Map<String, Object> respMap = null;
                    if (responseObj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> tmp = (java.util.Map<String, Object>) responseObj;
                        respMap = tmp;
                    }
                    String bodyStr = null;
                    if (respMap != null) {
                        Object b = respMap.get("body");
                        if (b != null) bodyStr = b.toString();
                    }
                    com.fasterxml.jackson.databind.JsonNode jsonBody = null;
                    if (bodyStr != null) {
                        String trimmed = bodyStr.trim();
                        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                            try { jsonBody = objectMapper.readTree(trimmed); } catch (Exception ignored) {}
                        }
                    }
                    for (Object it : exts) {
                        if (!(it instanceof java.util.Map)) continue;
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> rule = (java.util.Map<String, Object>) it;
                        Object active = rule.get("active");
                        if (active != null && active instanceof Boolean && !((Boolean) active)) {
                            continue;
                        }
                        String path = rule.get("path") != null ? rule.get("path").toString() : "";
                        String target = rule.get("target") != null ? rule.get("target").toString() : "";
                        String scope = rule.get("scope") != null ? rule.get("scope").toString() : "project";
                        if (path == null || target == null || target.trim().isEmpty()) {
                            continue;
                        }
                        String extracted = null;
                        if (path.startsWith("$.") && jsonBody != null) {
                            String p = path.substring(2);
                            String[] parts = p.split("\\.");
                            com.fasterxml.jackson.databind.JsonNode node = jsonBody;
                            for (String part : parts) {
                                if (node == null) break;
                                node = node.get(part);
                            }
                            if (node != null) {
                                extracted = node.isValueNode() ? node.asText() : node.toString();
                            }
                        } else if (path.startsWith("cookie:") && respMap != null) {
                            String name = path.substring("cookie:".length()).trim();
                            String setCookie = null;
                            Object headersObj = respMap.get("headers");
                            if (headersObj instanceof java.util.Map) {
                                Object sc = ((java.util.Map<?, ?>) headersObj).get("Set-Cookie");
                                if (sc != null) setCookie = sc.toString();
                            }
                            if (setCookie != null && !name.isEmpty()) {
                                String[] parts = setCookie.split(";");
                                for (String part : parts) {
                                    String[] kv = part.trim().split("=", 2);
                                    if (kv.length == 2) {
                                        String k = kv[0].trim();
                                        String v = kv[1].trim();
                                        if (name.equalsIgnoreCase(k)) {
                                            extracted = v;
                                            break;
                                        }
                                    }
                                }
                            }
                        } else if (respMap != null) {
                            Object exObj = respMap.get("extracted");
                            if (exObj instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> exmap = (java.util.Map<String, Object>) exObj;
                                Object val = exmap.get(path);
                                if (val != null) extracted = val.toString();
                            }
                        }
                        if (extracted == null) continue;
                        try {
                            if ("user".equalsIgnoreCase(scope)) {
                                if (executedBy != null && !executedBy.trim().isEmpty()) {
                                    User user = userService.getOne(new QueryWrapper<User>().eq("username", executedBy));
                                    if (user != null) {
                                        com.automatedtest.platform.entity.UserVariable uv = userVariableService.getOne(new QueryWrapper<com.automatedtest.platform.entity.UserVariable>()
                                                .eq("user_id", user.getId())
                                                .eq("key_name", target));
                                        if (uv == null) {
                                            uv = new com.automatedtest.platform.entity.UserVariable();
                                            uv.setUserId(user.getId());
                                            uv.setKeyName(target);
                                            uv.setValue(extracted);
                                            userVariableService.save(uv);
                                        } else {
                                            uv.setValue(extracted);
                                            uv.setUpdatedAt(LocalDateTime.now());
                                            userVariableService.updateById(uv);
                                        }
                                    }
                                }
                            } else if ("project".equalsIgnoreCase(scope)) {
                                if (testCase.getProjectId() != null) {
                                    com.automatedtest.platform.entity.ProjectVariable pv = projectVariableService.getOne(new QueryWrapper<com.automatedtest.platform.entity.ProjectVariable>()
                                            .eq("project_id", testCase.getProjectId())
                                            .eq("key_name", target));
                                    if (pv == null) {
                                        pv = new com.automatedtest.platform.entity.ProjectVariable();
                                        pv.setProjectId(testCase.getProjectId());
                                        pv.setKeyName(target);
                                        pv.setValue(extracted);
                                        projectVariableService.save(pv);
                                    } else {
                                        pv.setValue(extracted);
                                        pv.setUpdatedAt(LocalDateTime.now());
                                        projectVariableService.updateById(pv);
                                    }
                                }
                            } else if ("plan".equalsIgnoreCase(scope)) {
                                planVars.put(target, extracted);
                            }
                        } catch (Exception ignored) {}
                    }
                    if (!planVars.isEmpty()) {
                        result.setExtractedVars(planVars);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        TestReport report = new TestReport();
        if (testCase.getProjectId() != null) {
            report.setProjectId(testCase.getProjectId());
        }
        if (planId != null) {
            report.setPlanId(planId);
        }
        if (planRunNo != null && planRunNo > 0) {
            report.setPlanRunNo(planRunNo);
        }
        report.setCaseId(testCase.getId());
        report.setStatus(success ? "success" : "failed");
        if (result.getDurationMs() != null) {
            report.setExecutionTime(result.getDurationMs().intValue());
        }
        String logsToSave = result.getLogs();
        if (logsToSave == null) {
            logsToSave = "";
        }
        if (result.getError() != null && !result.getError().trim().isEmpty()) {
            if (logsToSave == null || logsToSave.trim().isEmpty()) {
                logsToSave = result.getError();
            } else {
                logsToSave = logsToSave + System.lineSeparator() + result.getError();
            }
        }
        report.setLogs(redactSecrets(logsToSave, varMap));
        report.setExecutedAt(LocalDateTime.now());
        if (executedBy != null && !executedBy.trim().isEmpty()) {
            report.setExecutedBy(executedBy.trim());
        } else {
            report.setExecutedBy("System");
        }
        
        // Handle trigger type
        if (triggerType != null && !triggerType.trim().isEmpty()) {
            report.setTriggerType(triggerType.trim());
        } else {
            // Infer from executedBy if missing
            String execBy = report.getExecutedBy();
            if ("System".equals(execBy)) {
                report.setTriggerType("schedule");
            } else if ("Project API Key".equals(execBy) || "OpenAPI".equals(execBy)) {
                report.setTriggerType("openapi");
            } else {
                report.setTriggerType("manual");
            }
        }
        
        testReportService.save(report);
        if (report.getId() != null) {
            result.setReportId(report.getId());
        }

        // Lightweight assertion evaluation for API cases (status & simple $.a.b jsonpath)
        try {
            if ("API".equalsIgnoreCase(testCase.getType())) {
                Object assertionsObj = contentMap.get("assertions");
                int total = 0, passed = 0;
                if (assertionsObj instanceof java.util.List) {
                    java.util.List<?> arr = (java.util.List<?>) assertionsObj;
                    Object respMapObj = result.getResponse();
                    Integer statusCode = null;
                    String bodyStr = null;
                    java.util.Map<String, Object> respMap = null;
                    if (respMapObj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> tmp = (java.util.Map<String, Object>) respMapObj;
                        respMap = tmp;
                        Object sc = tmp.get("statusCode");
                        if (sc instanceof Number) {
                            statusCode = ((Number) sc).intValue();
                        } else if (sc != null) {
                            try { statusCode = Integer.parseInt(sc.toString()); } catch (Exception ignored) {}
                        }
                        Object b = tmp.get("body");
                        if (b != null) bodyStr = b.toString();
                    }
                    com.fasterxml.jackson.databind.JsonNode jsonBody = null;
                    if (bodyStr != null) {
                        String trimmed = bodyStr.trim();
                        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                            try { jsonBody = objectMapper.readTree(trimmed); } catch (Exception ignored) {}
                        }
                    }
                    for (Object it : arr) {
                        if (!(it instanceof java.util.Map)) continue;
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> a = (java.util.Map<String, Object>) it;
                        Object active = a.get("active");
                        if (active != null && active instanceof Boolean && !((Boolean) active)) {
                            continue;
                        }
                        total++;
                        String type = a.get("type") != null ? a.get("type").toString() : "";
                        String path = a.get("path") != null ? a.get("path").toString() : "";
                        String value = a.get("value") != null ? a.get("value").toString() : null;
                        boolean ok = false;
                        if ("status".equalsIgnoreCase(type)) {
                            if (statusCode != null && value != null && value.equals(String.valueOf(statusCode))) {
                                ok = true;
                            }
                        } else if (( "jsonpath".equalsIgnoreCase(type) || "json".equalsIgnoreCase(type) ) && jsonBody != null && value != null) {
                            if (path != null && path.startsWith("$.")) {
                                String p = path.substring(2); // remove "$."
                                String[] parts = p.split("\\.");
                                com.fasterxml.jackson.databind.JsonNode node = jsonBody;
                                for (String part : parts) {
                                    if (node == null) break;
                                    node = node.get(part);
                                }
                                if (node != null) {
                                    String actual = node.isValueNode() ? node.asText() : node.toString();
                                    ok = value.equals(actual);
                                }
                            }
                        } else if ("header".equalsIgnoreCase(type) && value != null) {
                            // Expect path to be header key
                            if (respMap != null) {
                                Object hdrObj = respMap.get("headers");
                                java.util.Map<?, ?> hdrs = null;
                                if (hdrObj instanceof java.util.Map) {
                                    hdrs = (java.util.Map<?, ?>) hdrObj;
                                }
                                if (hdrs != null && path != null) {
                                    Object hv = hdrs.get(path);
                                    if (hv != null) {
                                        String actual = hv.toString();
                                        ok = value.equals(actual);
                                    }
                                }
                            }
                        } else if ("regex".equalsIgnoreCase(type) && value != null) {
                            if (bodyStr != null) {
                                try {
                                    java.util.regex.Pattern pat = java.util.regex.Pattern.compile(value);
                                    ok = pat.matcher(bodyStr).find();
                                } catch (Exception ignored) {}
                            }
                        }
                        if (ok) passed++;
                    }
                    result.setAssertsTotal(total);
                    result.setAssertsPassed(passed);
                    result.setAssertsFailed(total - passed);
                    // Persist to report
                    report.setAssertsTotal(result.getAssertsTotal());
                    report.setAssertsPassed(result.getAssertsPassed());
                    report.setAssertsFailed(result.getAssertsFailed());
                    testReportService.updateById(report);
                }
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    private String redactSecrets(String text, Map<String, String> varMap) {
        if (text == null || text.isEmpty()) return text;
        String redacted = text;
        if (varMap != null && !varMap.isEmpty()) {
            for (Map.Entry<String, String> entry : varMap.entrySet()) {
                String val = entry.getValue();
                if (val == null) continue;
                String trimmed = val.trim();
                if (trimmed.length() < 3) continue;
                String key = entry.getKey() != null ? entry.getKey().toUpperCase() : "";
                boolean likelySecret = key.contains("KEY") || key.contains("TOKEN") || key.contains("SECRET") || key.contains("PWD") || key.contains("PASS");
                if (!likelySecret) continue;
                redacted = redacted.replace(trimmed, "[ENCRYPTED]");
            }
        }
        return redacted;
    }
}
