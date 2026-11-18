package com.mgmt.module.role.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleRequest {

    private String name;

    private String code;

    private String description;

    private List<String> permissions;
}
