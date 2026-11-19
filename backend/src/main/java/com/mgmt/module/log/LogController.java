package com.mgmt.module.log;

import com.mgmt.common.PageResult;
import com.mgmt.common.Result;
import com.mgmt.module.log.entity.SysOperationLog;
import com.mgmt.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 操作日志只读接口。直接返回实体，因为查询结果不含敏感字段，
 * 再套一层 VO 只会增加维护成本（不是所有接口都必须有 VO）。
 */
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogService logService;

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
}
