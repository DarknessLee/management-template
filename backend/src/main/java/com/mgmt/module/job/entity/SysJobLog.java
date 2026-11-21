package com.mgmt.module.job.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务执行记录：定时触发和手动执行都会落一条，便于回溯。
 */
@Data
@TableName("sys_job_log")
public class SysJobLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long jobId;

    private String jobName;

    private String handlerKey;

    /** 触发方式：1定时 2手动 */
    private Integer triggerType;

    /** 结果：1成功 0失败 */
    private Integer status;

    private Long durationMs;

    /** 执行结果或失败原因 */
    private String result;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
