package com.mgmt.module.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserBatchStatusRequest {

    @NotEmpty(message = "请选择用户")
    private List<Long> ids;

    /** 1 启用 0 禁用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
