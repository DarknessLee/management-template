package com.mgmt.module.log;

import com.mgmt.common.I18n;
import com.mgmt.common.BizException;
import com.mgmt.common.PageResult;
import com.mgmt.common.Result;
import com.mgmt.module.log.dto.OperationLogStatsVO;
import com.mgmt.module.log.entity.SysOperationLog;
import com.mgmt.security.OperationLog;
import com.mgmt.security.RequirePermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作日志查询、导出与清理接口。直接返回实体，因为查询结果不含敏感字段，
 * 再套一层 VO 只会增加维护成本（不是所有接口都必须有 VO）。
 */
@RestController
@RequestMapping("/logs")
@Tag(name = "操作日志", description = "操作日志的查询、导出与清理")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogService logService;
    private final OperationLogExcelService logExcelService;

    @GetMapping("/export")
    @RequirePermission("logs:view")
    @OperationLog(module = "操作日志", action = "导出日志")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        byte[] data = logExcelService.export(module, keyword, startTime, endTime);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"operation-logs-" + LocalDate.now() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /**
     * 手动清理指定时间之前的日志，分批删除，返回删除条数。
     */
    @DeleteMapping("/clean")
    @RequirePermission("logs:clean")
    @OperationLog(module = "操作日志", action = "清理日志")
    public Result<Integer> clean(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime before) {
        if (before.isAfter(LocalDateTime.now())) {
            throw new BizException(I18n.get("log.cleanTimeFuture"));
        }
        return Result.ok(logService.deleteBefore(before, 1000));
    }

    @GetMapping
    @RequirePermission("logs:view")
    public Result<PageResult<SysOperationLog>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(logService.page(page, size, module, keyword, startTime, endTime));
    }

    @GetMapping("/stats")
    @RequirePermission("logs:view")
    public Result<OperationLogStatsVO> stats() {
        return Result.ok(logService.stats());
    }
}
