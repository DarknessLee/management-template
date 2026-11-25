package com.mgmt.module.notice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布公告：发布时间晚于当前时间则进入待发布，由定时任务自动上线。
 */
@Data
public class NoticeScheduleRequest {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledTime;

    /** 到期自动下线时间，可空 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
