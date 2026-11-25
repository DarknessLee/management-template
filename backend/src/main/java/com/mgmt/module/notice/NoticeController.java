package com.mgmt.module.notice;

import com.mgmt.common.PageResult;
import com.mgmt.common.Result;
import com.mgmt.common.UserContext;
import com.mgmt.module.notice.dto.NoticeReadStatsVO;
import com.mgmt.module.notice.dto.NoticeReadVO;
import com.mgmt.module.notice.dto.NoticeRequest;
import com.mgmt.module.notice.dto.NoticeScheduleRequest;
import com.mgmt.module.notice.entity.SysNotice;
import com.mgmt.security.OperationLog;
import com.mgmt.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/notices")
@Tag(name = "通知公告", description = "公告的发布、维护与阅读状态")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "分页查询公告列表")
    @GetMapping
    @RequirePermission("notice:view")
    public Result<PageResult<SysNotice>> page(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(noticeService.page(page, size, keyword, status));
    }

    /**
     * 公告中心对所有登录用户开放，只返回已发布公告。
     */
    @Operation(summary = "公告中心：分页查询已发布公告")
    @GetMapping("/center")
    public Result<PageResult<SysNotice>> center(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword) {
        return Result.ok(noticeService.centerPage(page, size, keyword, UserContext.getUserId()));
    }

    @Operation(summary = "新增公告")
    @PostMapping
    @RequirePermission("notice:edit")
    @OperationLog(module = "通知公告", action = "新增公告")
    public Result<SysNotice> create(@RequestBody NoticeRequest request) {
        return Result.ok(noticeService.create(request));
    }

    @Operation(summary = "编辑公告")
    @PutMapping("/{id}")
    @RequirePermission("notice:edit")
    @OperationLog(module = "通知公告", action = "编辑公告")
    public Result<SysNotice> update(@PathVariable Long id, @RequestBody NoticeRequest request) {
        return Result.ok(noticeService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @RequirePermission("notice:edit")
    @OperationLog(module = "通知公告", action = "发布/下线公告")
    public Result<SysNotice> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return Result.ok(noticeService.updateStatus(id, status));
    }

    @Operation(summary = "发布公告（支持定时发布与到期自动下线）")
    @PutMapping("/{id}/schedule")
    @RequirePermission("notice:edit")
    @OperationLog(module = "通知公告", action = "发布公告")
    public Result<SysNotice> schedule(@PathVariable Long id, @RequestBody NoticeScheduleRequest request) {
        return Result.ok(noticeService.publish(id, request.getScheduledTime(), request.getExpireTime()));
    }

    @PutMapping("/{id}/pin")
    @RequirePermission("notice:edit")
    @OperationLog(module = "通知公告", action = "置顶/取消置顶公告")
    public Result<SysNotice> updatePinned(@PathVariable Long id, @RequestParam Integer pinned) {
        return Result.ok(noticeService.updatePinned(id, pinned));
    }

    @GetMapping("/{id}/read-stats")
    @RequirePermission("notice:view")
    public Result<NoticeReadStatsVO> readStats(@PathVariable Long id) {
        return Result.ok(noticeService.readStats(id));
    }

    @GetMapping("/{id}/reads")
    @RequirePermission("notice:view")
    public Result<PageResult<NoticeReadVO>> reads(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        return Result.ok(noticeService.reads(id, page, size));
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @RequirePermission("notice:edit")
    @OperationLog(module = "通知公告", action = "删除公告")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }

    /**
     * 未读公告对所有登录用户开放，不额外校验 notice:view，
     * 否则普通用户会收不到系统通知。
     */
    @GetMapping("/unread")
    public Result<List<SysNotice>> unread() {
        return Result.ok(noticeService.unread(UserContext.getUserId()));
    }

    @Operation(summary = "当前用户未读公告数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(noticeService.unreadCount(UserContext.getUserId()));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        noticeService.markRead(id, UserContext.getUserId());
        return Result.ok();
    }
}
