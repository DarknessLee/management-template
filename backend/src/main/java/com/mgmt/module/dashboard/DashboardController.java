package com.mgmt.module.dashboard;

import com.mgmt.common.Result;
import com.mgmt.module.dashboard.dto.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public Result<DashboardVO> overview() {
        return Result.ok(dashboardService.overview());
    }
}
