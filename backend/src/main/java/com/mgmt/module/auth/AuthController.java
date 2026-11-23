package com.mgmt.module.auth;

import com.mgmt.common.IpUtils;
import com.mgmt.common.Result;
import com.mgmt.module.auth.dto.CaptchaVO;
import com.mgmt.module.auth.dto.LoginRequest;
import com.mgmt.module.auth.dto.LoginResponse;
import com.mgmt.module.auth.dto.UserInfoVO;
import com.mgmt.security.OperationLog;
import com.mgmt.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "认证", description = "登录、验证码与当前用户信息")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER = "Bearer ";

    private final AuthService authService;
    private final CaptchaService captchaService;

    @PostMapping("/kickout/{userId}")
    @RequirePermission("users:edit")
    @OperationLog(module = "用户管理", action = "强制下线")
    public Result<Integer> kickout(@PathVariable Long userId) {
        return Result.ok(authService.invalidateAllSessions(userId));
    }

    @Operation(summary = "获取图形验证码")
    @SecurityRequirements
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(captchaService.generate());
    }

    @Operation(summary = "用户登录")
    @SecurityRequirements
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return Result.ok(authService.login(request, IpUtils.clientIp(servletRequest)));
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/userinfo")
    public Result<UserInfoVO> userInfo() {
        return Result.ok(authService.userInfo());
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization != null && authorization.startsWith(BEARER)
                ? authorization.substring(BEARER.length())
                : null;
        if (token != null) {
            authService.logout(token);
        }
        return Result.ok();
    }
}
