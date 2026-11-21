package com.mgmt.module.job.dto;

import lombok.Data;

@Data
public class JobRequest {

    private String name;

    /** 创建后不可修改 */
    private String handlerKey;

    private String cronExpression;

    /** 1启用 0停用 */
    private Integer status;

    private String remark;
}
