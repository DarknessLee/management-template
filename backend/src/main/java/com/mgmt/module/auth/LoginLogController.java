package com.mgmt.module.auth;

import com.mgmt.common.PageResult;
import com.mgmt.common.Result;
import com.mgmt.module.auth.entity.LoginLog;
import com.mgmt.security.RequirePermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 登录日志只读接口，与操作日志共用 logs:view 权限。
 */
@RestController
@RequestMapping("/login-logs")
@Tag(name = "登录日志", description = "用户登录记录的查询")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @GetMapping
    @RequirePermission("logs:view")
    public Result<PageResult<LoginLog>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(loginLogService.page(page, size, username, startTime, endTime));
    }
}
