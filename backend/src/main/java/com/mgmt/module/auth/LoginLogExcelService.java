package com.mgmt.module.auth;

import com.mgmt.common.BizException;
import com.mgmt.common.I18n;
import com.mgmt.module.auth.entity.LoginLog;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 登录日志导出，筛选条件与列表页一致。
 */
@Service
@RequiredArgsConstructor
public class LoginLogExcelService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LoginLogService loginLogService;

    public byte[] export(String username, LocalDateTime startTime, LocalDateTime endTime) {
        List<LoginLog> logs = loginLogService.listForExport(username, startTime, endTime);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("登录日志");
            String[] headers = {"登录时间", "登录名", "用户ID", "退出时间", "在线时长(分钟)", "状态"};
            writeHeader(workbook, sheet, headers);

            int rowIndex = 1;
            for (LoginLog log : logs) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(log.getLoginTime() == null ? "" : log.getLoginTime().format(TIME_FORMATTER));
                row.createCell(1).setCellValue(nullToEmpty(log.getUsername()));
                row.createCell(2).setCellValue(log.getUserId() == null ? "" : String.valueOf(log.getUserId()));
                row.createCell(3).setCellValue(log.getLogoutTime() == null ? "" : log.getLogoutTime().format(TIME_FORMATTER));
                if (log.getLogoutTime() == null) {
                    row.createCell(4).setCellValue("");
                    row.createCell(5).setCellValue("在线");
                } else {
                    long minutes = durationMinutes(log.getLoginTime(), log.getLogoutTime());
                    row.createCell(4).setCellValue(minutes);
                    row.createCell(5).setCellValue("已退出");
                }
            }
            setWidths(sheet, 20, 16, 10, 20, 16, 10);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException(I18n.get("common.exportFailed"));
        }
    }

    private long durationMinutes(LocalDateTime loginTime, LocalDateTime logoutTime) {
        if (loginTime == null || logoutTime == null) {
            return 0;
        }
        return Math.max(0, Duration.between(loginTime, logoutTime).toMinutes());
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
