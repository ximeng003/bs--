package com.automatedtest.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("projects")
public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String name;

    private String description;

    @TableField("team_id")
    private Integer teamId;

    @TableField("created_by")
    private Integer createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(exist = false)
    private String role;

    @TableField("status")
    private String status;

    @TableField("is_deleted")
    private Boolean isDeleted;

    @TableField("health_score")
    private Double healthScore;

    @TableField("is_public")
    private Boolean isPublic;
    
    public Boolean getIsDeleted() { return isDeleted; }
    public Integer getTeamId() { return teamId; }
    public void setTeamId(Integer teamId) { this.teamId = teamId; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public void setRole(String role) { this.role = role; }
    public Integer getId() { return id; }
    public String getName() { return name; }
    public void setStatus(String status) { this.status = status; }
    public void setId(Integer id) { this.id = id; }
    public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
