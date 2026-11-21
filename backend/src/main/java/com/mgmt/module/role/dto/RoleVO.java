package com.mgmt.module.role.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class RoleVO {

    private Long id;
    private String name;
    private String code;
    private String description;
    /** 数据范围：1全部 2本部门 3本部门及以下 4仅本人 */
    private Integer dataScope;
    private List<String> permissions = new ArrayList<>();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
