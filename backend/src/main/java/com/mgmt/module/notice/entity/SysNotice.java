package com.mgmt.module.notice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告。status=1 已发布，0 已下线（含草稿）。
 */
@Data
@TableName("sys_notice")
public class SysNotice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Integer status;

    /** 是否置顶：1是 0否 */
    private Integer pinned;

    /** 已读人数，仅列表展示用 */
    @TableField(exist = false)
    private Long readCount;

    /** 当前登录用户是否已读：1已读 0未读，公告中心列表用 */
    @TableField(exist = false)
    private Integer readFlag;

    /** 最近一次发布时间，下线后保留，便于追溯 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    /** 计划发布时间，到点由定时任务自动上线 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledTime;

    /** 到期时间，到点由定时任务自动下线 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /** 是否处于待发布（已定时但还没到点），仅列表展示用 */
    @TableField(exist = false)
    private Boolean scheduled;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
