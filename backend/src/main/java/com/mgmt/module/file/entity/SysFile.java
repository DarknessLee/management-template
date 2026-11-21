package com.mgmt.module.file.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始文件名，仅用于展示和下载 */
    private String originalName;

    /** 服务端存储相对路径，如 202611/uuid.pdf，不直接暴露给前端 */
    private String storedName;

    /** 静态访问路径，如 /uploads/files/202611/uuid.pdf */
    private String url;

    private Long size;

    private String contentType;

    private Long uploaderId;

    private String uploaderName;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
