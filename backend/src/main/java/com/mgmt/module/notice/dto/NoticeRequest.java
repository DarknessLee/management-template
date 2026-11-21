package com.mgmt.module.notice.dto;

import lombok.Data;

@Data
public class NoticeRequest {

    private String title;

    private String content;

    private Integer pinned;
}
