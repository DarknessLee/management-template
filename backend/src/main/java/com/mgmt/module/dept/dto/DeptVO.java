package com.mgmt.module.dept.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DeptVO {

    private Long id;

    private Long parentId;

    private String name;

    private String leader;

    private String phone;

    private Integer sort;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private List<DeptVO> children = new ArrayList<>();
}
