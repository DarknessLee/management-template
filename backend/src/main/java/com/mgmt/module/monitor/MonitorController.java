package com.mgmt.module.monitor;

import com.mgmt.common.Result;
import com.mgmt.module.monitor.dto.MonitorInfo;
import com.mgmt.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping
    @RequirePermission("monitor:view")
    public Result<MonitorInfo> info() {
        return Result.ok(monitorService.info());
    }
}
