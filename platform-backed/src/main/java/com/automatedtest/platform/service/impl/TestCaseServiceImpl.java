package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.dto.ApiTestRequestDTO;
import com.automatedtest.platform.dto.ApiTestResponseDTO;
import com.automatedtest.platform.dto.CaseExecuteResultDTO;
import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.entity.TestReport;
import com.automatedtest.platform.mapper.TestCaseMapper;
import com.automatedtest.platform.service.TestCaseService;
import com.automatedtest.platform.service.TestReportService;
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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class TestCaseServiceImpl extends ServiceImpl<TestCaseMapper, TestCase> implements TestCaseService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestReportService testReportService;

    @Override
    public ApiTestResponseDTO executeApiTest(ApiTestRequestDTO request) {
        ApiTestResponseDTO responseDTO = new ApiTestResponseDTO();
        long startTime = System.currentTimeMillis();

        try {
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
    public CaseExecuteResultDTO executeCaseById(Integer id) {
        CaseExecuteResultDTO result = new CaseExecuteResultDTO();
        TestCase testCase = getById(id);
        if (testCase == null) {
            result.setStatus("failed");
            result.setError("测试用例不存在");
            return result;
        }
        Map<String, Object> contentMap = new HashMap<>();
        String content = testCase.getContent();
        if (content != null && !content.isBlank()) {
            try {
                contentMap = objectMapper.readValue(content, Map.class);
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
            if (pythonBin == null || pythonBin.isBlank()) {
                pythonBin = "python";
            }
            Path enginePath = Paths.get(System.getProperty("user.dir")).resolve("..").resolve("engine").resolve("test_driver.py").normalize();
            ProcessBuilder processBuilder = new ProcessBuilder(pythonBin, enginePath.toString());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            try (OutputStream os = process.getOutputStream()) {
                os.write(input.getBytes(StandardCharsets.UTF_8));
            }
            try (InputStream is = process.getInputStream()) {
                output = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            int exitCode = process.waitFor();
            if (exitCode == 0 && output != null && !output.isBlank()) {
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
        report.setCaseId(testCase.getId());
        report.setStatus(success ? "success" : "failed");
        if (result.getDurationMs() != null) {
            report.setExecutionTime(result.getDurationMs().intValue());
        }
        report.setLogs(result.getLogs() != null ? result.getLogs() : output);
        report.setExecutedBy("System");
        testReportService.save(report);

        return result;
    }
}
