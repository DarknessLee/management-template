package com.mgmt.module.dept.dto;

import lombok.Data;

@Data
public class DeptRequest {

    /** 上级部门ID，0 或不传表示顶级 */
    private Long parentId;

    private String name;

    private String leader;

    private String phone;

    private Integer sort;
}
