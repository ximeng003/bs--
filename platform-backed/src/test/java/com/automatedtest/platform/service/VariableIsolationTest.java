package com.automatedtest.platform.service;

import com.automatedtest.platform.dto.ApiTestRequestDTO;
import com.automatedtest.platform.dto.ApiTestResponseDTO;
import com.automatedtest.platform.entity.ProjectVariable;
import com.automatedtest.platform.entity.SystemVariable;
import com.automatedtest.platform.entity.UserVariable;
import com.automatedtest.platform.service.impl.TestCaseServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class VariableIsolationTest {

    @InjectMocks
    private TestCaseServiceImpl testCaseService;

    @Mock
    private SystemVariableService systemVariableService;

    @Mock
    private ProjectVariableService projectVariableService;

    @Mock
    private UserVariableService userVariableService;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Inject mock restTemplate into the service instance
        ReflectionTestUtils.setField(testCaseService, "restTemplate", restTemplate);
    }

    @Test
    void testSystemVariableSubstitution() {
        // Mock System Variables
        List<SystemVariable> sysVars = new ArrayList<>();
        SystemVariable sv = new SystemVariable();
        sv.setKeyName("VAR");
        sv.setValue("SystemValue");
        sysVars.add(sv);
        when(systemVariableService.list()).thenReturn(sysVars);

        // Mock empty others
        when(projectVariableService.list(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        when(userVariableService.list(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

        // Mock RestTemplate response
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    String url = invocation.getArgument(0);
                    // Verify URL has substituted value
                    if (url.contains("SystemValue")) {
                        return new ResponseEntity<>("Success", HttpStatus.OK);
                    }
                    return new ResponseEntity<>("Failed: " + url, HttpStatus.BAD_REQUEST);
                });

        ApiTestRequestDTO request = new ApiTestRequestDTO();
        request.setUrl("http://test.com?q={{VAR}}");
        request.setMethod("GET");

        ApiTestResponseDTO response = testCaseService.executeApiTest(request, 1L, 1);
        
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void testProjectVariableOverridesSystem() {
        // Mock System Variables
        List<SystemVariable> sysVars = new ArrayList<>();
        SystemVariable sv = new SystemVariable();
        sv.setKeyName("VAR");
        sv.setValue("SystemValue");
        sysVars.add(sv);
        when(systemVariableService.list()).thenReturn(sysVars);

        // Mock Project Variables
        List<ProjectVariable> projVars = new ArrayList<>();
        ProjectVariable pv = new ProjectVariable();
        pv.setKeyName("VAR");
        pv.setValue("ProjectValue");
        projVars.add(pv);
        when(projectVariableService.list(any(QueryWrapper.class))).thenReturn(projVars);

        // Mock empty User Variables
        when(userVariableService.list(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

        // Mock RestTemplate response
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    String url = invocation.getArgument(0);
                    // Verify URL has substituted value
                    if (url.contains("ProjectValue")) {
                        return new ResponseEntity<>("Success", HttpStatus.OK);
                    }
                    return new ResponseEntity<>("Failed: " + url, HttpStatus.BAD_REQUEST);
                });

        ApiTestRequestDTO request = new ApiTestRequestDTO();
        request.setUrl("http://test.com?q={{VAR}}");
        request.setMethod("GET");

        ApiTestResponseDTO response = testCaseService.executeApiTest(request, 1L, 1);

        assertEquals(200, response.getStatusCode());
    }

    @Test
    void testUserVariableOverridesAll() {
        // Mock System Variables
        List<SystemVariable> sysVars = new ArrayList<>();
        SystemVariable sv = new SystemVariable();
        sv.setKeyName("VAR");
        sv.setValue("SystemValue");
        sysVars.add(sv);
        when(systemVariableService.list()).thenReturn(sysVars);

        // Mock Project Variables
        List<ProjectVariable> projVars = new ArrayList<>();
        ProjectVariable pv = new ProjectVariable();
        pv.setKeyName("VAR");
        pv.setValue("ProjectValue");
        projVars.add(pv);
        when(projectVariableService.list(any(QueryWrapper.class))).thenReturn(projVars);

        // Mock User Variables
        List<UserVariable> userVars = new ArrayList<>();
        UserVariable uv = new UserVariable();
        uv.setKeyName("VAR");
        uv.setValue("UserValue");
        userVars.add(uv);
        when(userVariableService.list(any(QueryWrapper.class))).thenReturn(userVars);

        // Mock RestTemplate response
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    String url = invocation.getArgument(0);
                    // Verify URL has substituted value
                    if (url.contains("UserValue")) {
                        return new ResponseEntity<>("Success", HttpStatus.OK);
                    }
                    return new ResponseEntity<>("Failed: " + url, HttpStatus.BAD_REQUEST);
                });

        ApiTestRequestDTO request = new ApiTestRequestDTO();
        request.setUrl("http://test.com?q={{VAR}}");
        request.setMethod("GET");

        ApiTestResponseDTO response = testCaseService.executeApiTest(request, 1L, 1);

        assertEquals(200, response.getStatusCode());
    }
}
