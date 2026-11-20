package com.mgmt.module.auth;

import com.mgmt.common.Result;
import com.mgmt.module.auth.dto.OnlineUserVO;
import com.mgmt.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/online-users")
@RequiredArgsConstructor
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    @GetMapping
    @RequirePermission("online:view")
    public Result<List<OnlineUserVO>> list() {
        return Result.ok(onlineUserService.list());
    }
}
