package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("project_api_keys")
public class ProjectApiKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("project_id")
    private Integer projectId;

    @TableField("user_id")
    private Integer userId;

    @TableField("api_key")
    private String apiKey;

    private String description;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
}
