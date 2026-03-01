package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_variables")
public class UserVariable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String keyName;
    private String value;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
