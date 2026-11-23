package com.mgmt.module.log;

import com.mgmt.common.I18n;
import com.mgmt.common.BizException;
import com.mgmt.module.log.entity.SysOperationLog;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 操作日志导出，筛选条件与列表页一致。
 */
@Service
@RequiredArgsConstructor
public class OperationLogExcelService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PARAMS_MAX_LENGTH = 1000;

    private final OperationLogService logService;

    public byte[] export(String module, String keyword, LocalDateTime startTime, LocalDateTime endTime) {
        List<SysOperationLog> logs = logService.listForExport(module, keyword, startTime, endTime);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("操作日志");
            String[] headers = {"操作时间", "操作人", "模块", "动作", "方法", "请求路径",
                    "IP", "耗时(ms)", "结果", "失败原因", "请求参数"};
            writeHeader(workbook, sheet, headers);

            int rowIndex = 1;
            for (SysOperationLog log : logs) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(log.getCreateTime() == null ? "" : log.getCreateTime().format(TIME_FORMATTER));
                row.createCell(1).setCellValue(nullToEmpty(log.getUsername()));
                row.createCell(2).setCellValue(nullToEmpty(log.getModule()));
                row.createCell(3).setCellValue(nullToEmpty(log.getAction()));
                row.createCell(4).setCellValue(nullToEmpty(log.getMethod()));
                row.createCell(5).setCellValue(nullToEmpty(log.getUri()));
                row.createCell(6).setCellValue(nullToEmpty(log.getIp()));
                row.createCell(7).setCellValue(log.getDurationMs() == null ? 0 : log.getDurationMs());
                row.createCell(8).setCellValue(Integer.valueOf(1).equals(log.getSuccess()) ? "成功" : "失败");
                row.createCell(9).setCellValue(nullToEmpty(log.getErrorMsg()));
                row.createCell(10).setCellValue(truncate(log.getParams()));
            }
            setWidths(sheet, 20, 12, 12, 16, 8, 30, 16, 10, 8, 24, 50);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException(I18n.get("common.exportFailed"));
        }
    }

    private void writeHeader(Workbook workbook, Sheet sheet, String[] headers) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
        sheet.createFreezePane(0, 1);
    }

    private void setWidths(Sheet sheet, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    private String truncate(String params) {
        if (params == null) {
            return "";
        }
        return params.length() <= PARAMS_MAX_LENGTH ? params : params.substring(0, PARAMS_MAX_LENGTH) + "...";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
