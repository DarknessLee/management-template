package com.mgmt.module.dept;

import com.mgmt.common.Result;
import com.mgmt.module.dept.dto.DeptRequest;
import com.mgmt.module.dept.dto.DeptVO;
import com.mgmt.security.OperationLog;
import com.mgmt.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    @RequirePermission("dept:view")
    public Result<List<DeptVO>> tree() {
        return Result.ok(deptService.tree());
    }

    /**
     * 用户表单选择部门时使用，不额外要求部门管理权限。
     */
    @GetMapping("/options")
    public Result<List<DeptVO>> options() {
        return Result.ok(deptService.options());
    }

    @PostMapping
    @RequirePermission("dept:edit")
    @OperationLog(module = "部门管理", action = "新增部门")
    public Result<DeptVO> create(@RequestBody DeptRequest request) {
        return Result.ok(deptService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("dept:edit")
    @OperationLog(module = "部门管理", action = "编辑部门")
    public Result<DeptVO> update(@PathVariable Long id, @RequestBody DeptRequest request) {
        return Result.ok(deptService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("dept:edit")
    @OperationLog(module = "部门管理", action = "删除部门")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}
