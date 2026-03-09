package com.automatedtest.platform.service;

import com.automatedtest.platform.dto.ApiTestRequestDTO;
import com.automatedtest.platform.dto.ApiTestResponseDTO;
import com.automatedtest.platform.dto.CaseExecuteResultDTO;
import com.automatedtest.platform.entity.TestCase;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TestCaseService extends IService<TestCase> {
    ApiTestResponseDTO executeApiTest(ApiTestRequestDTO request);
    ApiTestResponseDTO executeApiTest(ApiTestRequestDTO request, Long userId);
    ApiTestResponseDTO executeApiTest(ApiTestRequestDTO request, Long userId, Integer projectId);
    CaseExecuteResultDTO executeCaseById(Integer id, String executedBy);
    CaseExecuteResultDTO executeCaseById(Integer id, String executedBy, Integer planId);
    CaseExecuteResultDTO executeCaseById(Integer id, String executedBy, Integer planId, Integer planRunNo);
    CaseExecuteResultDTO executeCaseById(Integer id, String executedBy, Integer planId, Integer planRunNo, String triggerType);
    CaseExecuteResultDTO executeCaseByIdWithVariables(Integer id, String executedBy, java.util.Map<String, String> variables);
}
