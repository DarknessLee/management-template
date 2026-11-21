package com.mgmt.module.role.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleRequest {

    private String name;

    private String code;

    private String description;

    /** 数据范围：1全部 2本部门 3本部门及以下 4仅本人 */
    private Integer dataScope;

    private List<String> permissions;
}
