package com.mgmt.module.dict;

import com.mgmt.common.PageResult;
import com.mgmt.common.Result;
import com.mgmt.module.dict.dto.DictItemRequest;
import com.mgmt.module.dict.dto.DictTypeRequest;
import com.mgmt.module.dict.entity.SysDictItem;
import com.mgmt.module.dict.entity.SysDictType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping("/types")
    @RequirePermission("dict:view")
    public Result<PageResult<SysDictType>> pageTypes(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) String keyword) {
        return Result.ok(dictService.pageTypes(page, size, keyword));
    }

    @PostMapping("/types")
    @RequirePermission("dict:edit")
    @OperationLog(module = "数据字典", action = "新增字典类型")
    public Result<SysDictType> createType(@RequestBody DictTypeRequest request) {
        return Result.ok(dictService.createType(request));
    }

    @PutMapping("/types/{id}")
    @RequirePermission("dict:edit")
    @OperationLog(module = "数据字典", action = "编辑字典类型")
    public Result<SysDictType> updateType(@PathVariable Long id, @RequestBody DictTypeRequest request) {
        return Result.ok(dictService.updateType(id, request));
    }

    @DeleteMapping("/types/{id}")
    @RequirePermission("dict:edit")
    @OperationLog(module = "数据字典", action = "删除字典类型")
    public Result<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return Result.ok();
    }

    @GetMapping("/types/{typeId}/items")
    @RequirePermission("dict:view")
    public Result<List<SysDictItem>> items(@PathVariable Long typeId) {
        return Result.ok(dictService.items(typeId));
    }

    @PostMapping("/items")
    @RequirePermission("dict:edit")
    @OperationLog(module = "数据字典", action = "新增字典项")
    public Result<SysDictItem> createItem(@RequestBody DictItemRequest request) {
        return Result.ok(dictService.createItem(request));
    }

    @PutMapping("/items/{id}")
    @RequirePermission("dict:edit")
    @OperationLog(module = "数据字典", action = "编辑字典项")
    public Result<SysDictItem> updateItem(@PathVariable Long id, @RequestBody DictItemRequest request) {
        return Result.ok(dictService.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    @RequirePermission("dict:edit")
    @OperationLog(module = "数据字典", action = "删除字典项")
    public Result<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return Result.ok();
    }

    /**
     * 按编码读取字典项：任意登录用户都可用，方便业务页面渲染下拉框。
     */
    @GetMapping("/code/{code}/items")
    public Result<List<SysDictItem>> itemsByCode(@PathVariable String code) {
        return Result.ok(dictService.itemsByCode(code));
    }
}
