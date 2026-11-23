package com.mgmt.module.dashboard;

import com.mgmt.common.Result;
import com.mgmt.module.dashboard.dto.DashboardVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "仪表盘", description = "首页统计数据概览")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public Result<DashboardVO> overview() {
        return Result.ok(dashboardService.overview());
    }
}
