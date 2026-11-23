package com.mgmt.module.auth;

import com.mgmt.common.Result;
import com.mgmt.module.auth.dto.OnlineUserVO;
import com.mgmt.security.RequirePermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/online-users")
@Tag(name = "在线用户", description = "当前在线用户列表查询")
@RequiredArgsConstructor
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    @GetMapping
    @RequirePermission("online:view")
    public Result<List<OnlineUserVO>> list() {
        return Result.ok(onlineUserService.list());
    }
}
