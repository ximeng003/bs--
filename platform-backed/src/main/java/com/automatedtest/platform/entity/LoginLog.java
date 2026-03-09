package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("login_logs")
public class LoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String ipAddress;
    private String location;
    private String device;
    private String status;
    private LocalDateTime loginTime;
    
    public void setUserId(Long userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setLocation(String location) { this.location = location; }
    public void setDevice(String device) { this.device = device; }
    public void setStatus(String status) { this.status = status; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
}
