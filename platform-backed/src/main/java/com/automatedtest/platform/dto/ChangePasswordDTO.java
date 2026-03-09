package com.automatedtest.platform.dto;

import lombok.Data;

@Data
public class ChangePasswordDTO {
    private String oldPassword;
    private String newPassword;
    
    public String getOldPassword() { return oldPassword; }
    public String getNewPassword() { return newPassword; }
}
