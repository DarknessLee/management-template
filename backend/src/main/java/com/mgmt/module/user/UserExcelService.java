package com.mgmt.module.user;

import com.mgmt.common.I18n;
import com.mgmt.common.BizException;
import com.mgmt.module.user.dto.UserCreateRequest;
import com.mgmt.module.user.dto.UserImportResult;
import com.mgmt.module.user.dto.UserVO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * 用户 Excel 导入导出。导入逐行处理：成功行直接入库，失败行返回原因，不影响其他行。
 */
@Service
@RequiredArgsConstructor
public class UserExcelService {

    private static final String[] IMPORT_HEADERS = {"用户名", "姓名", "邮箱", "手机号", "角色编码", "密码"};
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_PASSWORD = "123456";

    private final UserService userService;
    private final Validator validator;

    public byte[] export(String keyword) {
        List<UserVO> users = userService.listForExport(keyword);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户");
            String[] headers = {"ID", "用户名", "姓名", "部门", "邮箱", "手机号", "角色编码", "状态", "创建时间"};
            writeHeader(workbook, sheet, headers);

            int rowIndex = 1;
            for (UserVO user : users) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(nullToEmpty(user.getUsername()));
                row.createCell(2).setCellValue(nullToEmpty(user.getRealName()));
                row.createCell(3).setCellValue(nullToEmpty(user.getDeptName()));
                row.createCell(4).setCellValue(nullToEmpty(user.getEmail()));
                row.createCell(5).setCellValue(nullToEmpty(user.getPhone()));
                row.createCell(6).setCellValue(nullToEmpty(user.getRole()));
                row.createCell(7).setCellValue(user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用");
                row.createCell(8).setCellValue(user.getCreateTime() == null ? "" : user.getCreateTime().format(TIME_FORMATTER));
            }
            setWidths(sheet, 8, 16, 12, 16, 28, 18, 12, 10, 22);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException(I18n.get("common.exportFailed"));
        }
    }

    public byte[] template() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户导入");
            writeHeader(workbook, sheet, IMPORT_HEADERS);
            setWidths(sheet, 18, 14, 26, 18, 14, 16);

            String[][] notes = {
                    {"列名", "填写说明"},
                    {"用户名", "必填，登录名，不能与已有用户重复"},
                    {"姓名", "必填"},
                    {"邮箱", "必填，格式需正确"},
                    {"手机号", "必填"},
                    {"角色编码", "必填，需与角色管理中的编码一致，如 admin、editor、viewer"},
                    {"密码", "选填，留空默认 123456，填写时不少于 6 位"}
            };
            Sheet help = workbook.createSheet("填写说明");
            for (int i = 0; i < notes.length; i++) {
                Row row = help.createRow(i);
                row.createCell(0).setCellValue(notes[i][0]);
                row.createCell(1).setCellValue(notes[i][1]);
            }
            setWidths(help, 14, 70);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException(I18n.get("user.templateGenerateFailed"));
        }
    }

    public UserImportResult importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(I18n.get("user.importFileRequired"));
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BizException(I18n.get("user.onlyXlsx"));
        }

        UserImportResult result = new UserImportResult();
        try (InputStream in = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String username = cellValue(row.getCell(0));
                String realName = cellValue(row.getCell(1));
                String email = cellValue(row.getCell(2));
                String phone = cellValue(row.getCell(3));
                String role = cellValue(row.getCell(4));
                String password = cellValue(row.getCell(5));
                if (!StringUtils.hasText(username) && !StringUtils.hasText(realName)
                        && !StringUtils.hasText(email) && !StringUtils.hasText(phone)
                        && !StringUtils.hasText(role) && !StringUtils.hasText(password)) {
                    continue;
                }

                result.setTotal(result.getTotal() + 1);
                try {
                    UserCreateRequest request = new UserCreateRequest();
                    request.setUsername(username);
                    request.setRealName(realName);
                    request.setEmail(email);
                    request.setPhone(phone);
                    request.setRole(role);
                    request.setPassword(StringUtils.hasText(password) ? password : DEFAULT_PASSWORD);
                    request.setStatus(1);

                    if (!StringUtils.hasText(email)) {
                        throw new BizException(I18n.get("validation.emailRequired"));
                    }
                    if (!StringUtils.hasText(phone)) {
                        throw new BizException(I18n.get("validation.phoneRequired"));
                    }
                    Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);
                    if (!violations.isEmpty()) {
                        throw new BizException(violations.iterator().next().getMessage());
                    }

                    userService.create(request);
                    result.setSuccess(result.getSuccess() + 1);
                } catch (BizException e) {
                    result.getErrors().add(new UserImportResult.RowError(i + 1, e.getMessage()));
                } catch (Exception e) {
                    result.getErrors().add(new UserImportResult.RowError(i + 1, "导入失败"));
                }
            }
        } catch (IOException e) {
            throw new BizException(I18n.get("user.parseFailed"));
        }

        result.setFailed(result.getTotal() - result.getSuccess());
        return result;
    }

    private String cellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        // 手机号等纯数字单元格在 Excel 中可能是数值型，直接转字符串避免出现科学计数法
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        return new DataFormatter().formatCellValue(cell).trim();
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
