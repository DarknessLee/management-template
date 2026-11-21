package com.mgmt.module.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件列表视图：不返回 storedName，避免前端拼出非预期的存储路径。
 */
@Data
public class FileVO {

    private Long id;

    private String originalName;

    private String url;

    private Long size;

    private String contentType;

    private String uploaderName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
