package com.mgmt.module.message;

import com.mgmt.common.PageResult;
import com.mgmt.common.Result;
import com.mgmt.common.UserContext;
import com.mgmt.module.message.dto.MessageSendRequest;
import com.mgmt.module.message.entity.SysMessage;
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

import java.util.List;

@RestController
@RequestMapping("/messages")
@Tag(name = "站内消息", description = "当前登录用户的消息列表、未读数与已读操作")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "管理员群发站内消息")
    @PostMapping("/send")
    @RequirePermission("message:send")
    @OperationLog(module = "站内消息", action = "群发消息")
    public Result<Integer> send(@Valid @RequestBody MessageSendRequest request) {
        return Result.ok(messageService.send(request));
    }

    @Operation(summary = "分页查询当前用户消息")
    @GetMapping
    public Result<PageResult<SysMessage>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Integer readFlag,
                                               @RequestParam(required = false) String type) {
        return Result.ok(messageService.page(UserContext.getUserId(), page, size, readFlag, type));
    }

    @Operation(summary = "当前用户未读消息数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(messageService.unreadCount(UserContext.getUserId()));
    }

    @Operation(summary = "标记单条消息已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        messageService.markAllRead(UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "批量删除消息")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        messageService.deleteBatch(ids, UserContext.getUserId());
        return Result.ok();
    }

    @Operation(summary = "删除消息")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.delete(id, UserContext.getUserId());
        return Result.ok();
    }
}
