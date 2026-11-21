package com.mgmt.module.notice.dto;

import lombok.Data;

@Data
public class NoticeReadStatsVO {

    private Long noticeId;

    private Long readCount;

    private Long totalUsers;

    private Long unreadCount;
}
