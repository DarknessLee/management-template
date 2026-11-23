package com.mgmt.module.profile;

import com.mgmt.common.Result;
import com.mgmt.module.profile.dto.ChangePasswordRequest;
import com.mgmt.module.profile.dto.ProfileVO;
import com.mgmt.module.profile.dto.UpdateProfileRequest;
import com.mgmt.security.OperationLog;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人中心：只要求"已登录"，不要求任何业务权限。
 * 所有操作对象都是当前登录用户，接口里没有 userId 参数。
 */
@RestController
@RequestMapping("/profile")
@Tag(name = "个人中心", description = "当前登录用户的资料与密码管理")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public Result<ProfileVO> get() {
        return Result.ok(profileService.get());
    }

    @PutMapping
    @OperationLog(module = "个人中心", action = "修改资料")
    public Result<ProfileVO> update(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(profileService.update(request));
    }

    @PutMapping("/password")
    @OperationLog(module = "个人中心", action = "修改密码")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(request);
        return Result.ok();
    }

    @PostMapping("/avatar")
    @OperationLog(module = "个人中心", action = "更换头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(profileService.uploadAvatar(file));
    }
}
