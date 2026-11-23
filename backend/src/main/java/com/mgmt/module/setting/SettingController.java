package com.mgmt.module.setting;

import com.mgmt.common.Result;
import com.mgmt.security.OperationLog;
import com.mgmt.security.RequirePermission;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/settings")
@Tag(name = "系统设置", description = "系统参数的查询与修改")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    @RequirePermission("settings:view")
    public Result<Map<String, Object>> get() {
        return Result.ok(settingService.get());
    }

    /**
     * 登录页与侧边栏展示品牌信息，不要求登录和设置权限。
     */
    @SecurityRequirements
    @GetMapping("/public")
    public Result<Map<String, Object>> publicBasic() {
        return Result.ok(settingService.publicBasic());
    }

    @PutMapping
    @RequirePermission("settings:edit")
    @OperationLog(module = "系统设置", action = "修改设置")
    public Result<Void> update(@RequestBody Map<String, Object> body) {
        settingService.update(body);
        return Result.ok();
    }
}
