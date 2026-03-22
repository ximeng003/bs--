package com.automatedtest.platform.service;

import com.automatedtest.platform.entity.TestReport;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TestReportService extends IService<TestReport> {
    IPage<TestReport> listUnified(Page<TestReport> page, Integer projectId, String status, String keyword, String date);
}
