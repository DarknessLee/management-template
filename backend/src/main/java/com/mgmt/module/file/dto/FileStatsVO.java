package com.mgmt.module.file.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件管理统计：文件总数、占用空间与类型分布 Top3。
 */
@Data
public class FileStatsVO {

    private long totalFiles;
    /** 所有文件大小之和（字节） */
    private long totalSize;

    private List<TypeCount> types = new ArrayList<>();

    @Data
    public static class TypeCount {
        /** 分类标识：image/pdf/office/text/archive/other */
        private String type;
        private long count;

        public TypeCount(String type, long count) {
            this.type = type;
            this.count = count;
        }
    }
}
