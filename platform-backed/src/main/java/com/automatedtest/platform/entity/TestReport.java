package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("test_reports")
public class TestReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("plan_id")
    private Integer planId;

    @TableField("plan_run_no")
    private Integer planRunNo;

    @TableField("case_id")
    private Integer caseId;

    private String status; // success, failed

    @TableField("execution_time")
    private Integer executionTime;

    private String logs;

    @TableField(value = "executed_at", fill = FieldFill.INSERT)
    private LocalDateTime executedAt;

    @TableField("executed_by")
    private String executedBy;

    @TableField(exist = false)
    private String caseName;

    @TableField(exist = false)
    private String planName;

    @TableField(exist = false)
    private String environment;

    @TableField(exist = false)
    private String caseType;
}
