package com.mgmt.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果：逐行处理，成功的直接入库，失败的行返回原因。
 */
@Data
public class UserImportResult {

    private int total;

    private int success;

    private int failed;

    private List<RowError> errors = new ArrayList<>();

    @Data
    @AllArgsConstructor
    public static class RowError {

        /** Excel 中的行号（含表头，从 1 开始） */
        private int row;

        private String message;
    }
}
