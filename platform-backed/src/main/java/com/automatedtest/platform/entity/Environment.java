package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("environments")
public class Environment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String name;
    
    @TableField("key_name")
    private String keyName; // 'dev', 'staging', 'production'
    
    @TableField("base_url")
    private String baseUrl;
    
    private String databaseName;
    
    private Boolean active;
}
