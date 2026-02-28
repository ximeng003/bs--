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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            String finalUrl = builder.toUriString();

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
            ResponseEntity<String> response = restTemplate.exchange(finalUrl, method, entity, String.class);

            // 5. Process Response
            responseDTO.setStatusCode(response.getStatusCodeValue());
            responseDTO.setBody(response.getBody());
            
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
        CaseExecuteResultDTO result = new CaseExecuteResultDTO();
        TestCase testCase = getById(id);
        if (testCase == null) {
            result.setStatus("failed");
            result.setError("测试用例不存在");
            return result;
        }
        return executeCaseInternal(testCase, executedBy, planId, planRunNo);
    }

    private CaseExecuteResultDTO executeCaseInternal(TestCase testCase, String executedBy, Integer planId, Integer planRunNo) {
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
        Map<String, Object> payload = new HashMap<>();
        payload.put("caseId", testCase.getId());
        payload.put("type", testCase.getType());
        payload.put("environment", testCase.getEnvironment());
        payload.put("content", contentMap);
        String output = "";
        try {
            String input = objectMapper.writeValueAsString(payload);
            String pythonBin = System.getenv("PYTHON_BIN");
            if (pythonBin == null || pythonBin.trim().length() == 0) {
                pythonBin = "python";
            }
            Path enginePath = Paths.get(System.getProperty("user.dir")).resolve("..").resolve("engine").resolve("test_driver.py").normalize();
            ProcessBuilder processBuilder = new ProcessBuilder(pythonBin, enginePath.toString());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            try (OutputStream os = process.getOutputStream()) {
                os.write(input.getBytes(StandardCharsets.UTF_8));
            }
            try (InputStream is = process.getInputStream();
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                output = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            }
            int exitCode = process.waitFor();
            if (exitCode == 0 && output != null && output.trim().length() > 0) {
                result = objectMapper.readValue(output, CaseExecuteResultDTO.class);
            } else {
                result.setStatus("failed");
                result.setError("执行失败");
                result.setLogs(output);
            }
        } catch (Exception e) {
            result.setStatus("failed");
            result.setError(e.getMessage());
            result.setLogs(output);
        }

        boolean success = "success".equalsIgnoreCase(result.getStatus());
        testCase.setLastRun(LocalDateTime.now());
        testCase.setLastResult(success ? "success" : "failed");
        updateById(testCase);

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
        if (logsToSave == null || logsToSave.trim().isEmpty()) {
            logsToSave = output;
        }
        if (result.getError() != null && !result.getError().trim().isEmpty()) {
            if (logsToSave == null || logsToSave.trim().isEmpty()) {
                logsToSave = result.getError();
            } else {
                logsToSave = logsToSave + System.lineSeparator() + result.getError();
            }
        }
        report.setLogs(logsToSave);
        report.setExecutedAt(LocalDateTime.now());
        if (executedBy != null && !executedBy.trim().isEmpty()) {
            report.setExecutedBy(executedBy.trim());
        } else {
            report.setExecutedBy("System");
        }
        testReportService.save(report);
        if (report.getId() != null) {
            result.setReportId(report.getId());
        }

        return result;
    }
}
