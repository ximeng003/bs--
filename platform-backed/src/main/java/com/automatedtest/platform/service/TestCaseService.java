package com.automatedtest.platform.service;

import com.automatedtest.platform.dto.ApiTestRequestDTO;
import com.automatedtest.platform.dto.ApiTestResponseDTO;
import com.automatedtest.platform.dto.CaseExecuteResultDTO;
import com.automatedtest.platform.entity.TestCase;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TestCaseService extends IService<TestCase> {
    ApiTestResponseDTO executeApiTest(ApiTestRequestDTO request);
    CaseExecuteResultDTO executeCaseById(Integer id, String executedBy);
    CaseExecuteResultDTO executeCaseById(Integer id, String executedBy, Integer planId);
    CaseExecuteResultDTO executeCaseById(Integer id, String executedBy, Integer planId, Integer planRunNo);
}
