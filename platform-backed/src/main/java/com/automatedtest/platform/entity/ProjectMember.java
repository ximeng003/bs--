package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("project_members")
public class ProjectMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("project_id")
    private Integer projectId;

    @TableField("user_id")
    private Integer userId;

    /**
     * admin, member, viewer
     */
    private String role;

    @TableField(value = "joined_at", fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;
}
