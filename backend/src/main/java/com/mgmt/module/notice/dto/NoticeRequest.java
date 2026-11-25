package com.mgmt.module.notice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeRequest {

    private String title;

    private String content;

    private Integer pinned;

    /** 到期自动下线时间，可空 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
