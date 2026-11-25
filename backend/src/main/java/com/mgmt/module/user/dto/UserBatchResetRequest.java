package com.mgmt.module.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UserBatchResetRequest {

    @NotEmpty(message = "请选择用户")
    private List<Long> ids;
}
