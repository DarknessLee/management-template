package com.mgmt.module.dict.dto;

import lombok.Data;

@Data
public class DictItemRequest {

    private Long typeId;

    private String label;

    private String value;

    private Integer sort;
}
