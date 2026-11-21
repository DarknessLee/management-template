package com.mgmt.module.job;

import com.mgmt.common.PageResult;
import com.mgmt.common.Result;
import com.mgmt.module.job.dto.JobHandlerVO;
import com.mgmt.module.job.dto.JobRequest;
import com.mgmt.module.job.dto.JobVO;
import com.mgmt.module.job.entity.SysJobLog;
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
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobLogService jobLogService;

    @GetMapping
    @RequirePermission("job:view")
    public Result<List<JobVO>> list(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Integer status) {
        return Result.ok(jobService.list(keyword, status));
    }

    @GetMapping("/handlers")
    @RequirePermission("job:view")
    public Result<List<JobHandlerVO>> handlers() {
        return Result.ok(jobService.handlers());
    }

    @PostMapping
    @RequirePermission("job:edit")
    @OperationLog(module = "定时任务", action = "新增任务")
    public Result<JobVO> create(@RequestBody JobRequest request) {
        return Result.ok(jobService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("job:edit")
    @OperationLog(module = "定时任务", action = "编辑任务")
    public Result<JobVO> update(@PathVariable Long id, @RequestBody JobRequest request) {
        return Result.ok(jobService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("job:edit")
    @OperationLog(module = "定时任务", action = "删除任务")
    public Result<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/run")
    @RequirePermission("job:edit")
    @OperationLog(module = "定时任务", action = "执行任务")
    public Result<String> run(@PathVariable Long id) {
        return Result.ok(jobService.runNow(id));
    }

    /**
     * 执行记录：按任务分页查询，定时和手动执行都会记录。
     */
    @GetMapping("/{id}/logs")
    @RequirePermission("job:view")
    public Result<PageResult<SysJobLog>> logs(@PathVariable Long id,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size) {
        return Result.ok(jobLogService.logs(id, page, size));
    }
}
