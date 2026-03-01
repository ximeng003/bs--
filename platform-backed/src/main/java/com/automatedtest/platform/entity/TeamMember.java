package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("team_members")
public class TeamMember implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("team_id")
    private Integer teamId;

    @TableField("user_id")
    private Integer userId;

    private String role; // admin, member

    @TableField(value = "joined_at", fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;

    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String nickname;

    @TableField(exist = false)
    private String avatar;
}