package com.mgmt.module.user;

import com.mgmt.common.PageResult;
import com.mgmt.common.Result;
import com.mgmt.module.user.dto.UserCreateRequest;
import com.mgmt.module.user.dto.UserImportResult;
import com.mgmt.module.user.dto.UserUpdateRequest;
import com.mgmt.module.user.dto.UserVO;
import com.mgmt.security.OperationLog;
import com.mgmt.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/users")
@Tag(name = "用户管理", description = "用户信息的查询、维护与导入导出")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserExcelService userExcelService;

    @GetMapping("/export")
    @RequirePermission("users:view")
    @OperationLog(module = "用户管理", action = "导出用户")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String keyword) {
        return xlsx(userExcelService.export(keyword), "users-" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/import-template")
    @RequirePermission("users:add")
    public ResponseEntity<byte[]> importTemplate() {
        return xlsx(userExcelService.template(), "user-import-template.xlsx");
    }

    @PostMapping("/import")
    @RequirePermission("users:add")
    @OperationLog(module = "用户管理", action = "导入用户")
    public Result<UserImportResult> importUsers(@RequestParam("file") MultipartFile file) {
        return Result.ok(userExcelService.importUsers(file));
    }

    private ResponseEntity<byte[]> xlsx(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @Operation(summary = "分页查询用户列表")
    @GetMapping
    @RequirePermission("users:view")
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(userService.page(page, size, keyword));
    }

    @GetMapping("/{id}")
    @RequirePermission("users:view")
    public Result<UserVO> get(@PathVariable Long id) {
        return Result.ok(userService.get(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @RequirePermission("users:add")
    @OperationLog(module = "用户管理", action = "新增用户")
    public Result<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userService.create(request));
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    @RequirePermission("users:edit")
    @OperationLog(module = "用户管理", action = "编辑用户")
    public Result<UserVO> update(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return Result.ok(userService.update(id, request));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @RequirePermission("users:delete")
    @OperationLog(module = "用户管理", action = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
