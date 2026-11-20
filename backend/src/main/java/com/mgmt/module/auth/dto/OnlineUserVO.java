package com.mgmt.module.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在线用户：同一用户的多个登录会话聚合为一行。
 */
@Data
public class OnlineUserVO {

    private Long userId;

    private String username;

    private String realName;

    /** 角色编码 */
    private String role;

    private String ip;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    /** 该用户当前有效的会话数量 */
    private int sessionCount;
}
