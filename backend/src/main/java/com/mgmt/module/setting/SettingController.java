package com.mgmt.module.setting;

import com.mgmt.common.Result;
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
    public Result<Map<String, Object>> get() {
        return Result.ok(settingService.get());
    }

    @PutMapping
    public Result<Void> update(@RequestBody Map<String, Object> body) {
        settingService.update(body);
        return Result.ok();
    }
}
