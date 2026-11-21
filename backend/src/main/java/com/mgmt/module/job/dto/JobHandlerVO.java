package com.mgmt.module.job.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobHandlerVO {

    private String key;

    private String name;

    private String description;
}
