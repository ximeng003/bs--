package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("test_cases")
public class TestCase implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String name;

    private String description;

    /**
     * API, WEB, APP
     */
    private String type;

    /**
     * active, inactive, draft
     */
    private String status;

    /**
     * high, medium, low
     */
    private String priority;
    
    /**
     * Stores JSON for API case or Script text for Web/App
     */
    private String content;
    
    private String environment;
    
    private LocalDateTime lastRun;
    
    private String lastResult; // success, failed, pending

    @TableField("created_by")
    private Integer createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
