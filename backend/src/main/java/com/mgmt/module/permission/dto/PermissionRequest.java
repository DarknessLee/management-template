package com.mgmt.module.permission.dto;

import lombok.Data;

@Data
public class PermissionRequest {

    private String code;

    private String name;

    private String module;
}
