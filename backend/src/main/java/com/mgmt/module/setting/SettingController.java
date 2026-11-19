package com.mgmt.module.setting;

import com.mgmt.common.Result;
import com.mgmt.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    @RequirePermission("settings:view")
    public Result<Map<String, Object>> get() {
        return Result.ok(settingService.get());
    }

    @PutMapping
    @RequirePermission("settings:edit")
    public Result<Void> update(@RequestBody Map<String, Object> body) {
        settingService.update(body);
        return Result.ok();
    }
}
