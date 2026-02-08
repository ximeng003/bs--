package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.dto.ApiTestRequestDTO;
import com.automatedtest.platform.dto.ApiTestResponseDTO;
import com.automatedtest.platform.entity.TestCase;
import com.automatedtest.platform.mapper.TestCaseMapper;
import com.automatedtest.platform.service.TestCaseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TestCaseServiceImpl extends ServiceImpl<TestCaseMapper, TestCase> implements TestCaseService {

    private final RestTemplate restTemplate = new RestTemplate();

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
}
