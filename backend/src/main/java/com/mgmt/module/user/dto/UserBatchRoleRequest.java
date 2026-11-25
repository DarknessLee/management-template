package com.mgmt.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UserBatchRoleRequest {

    @NotEmpty(message = "请选择用户")
    private List<Long> ids;

    @NotBlank(message = "角色不能为空")
    private String role;
}
