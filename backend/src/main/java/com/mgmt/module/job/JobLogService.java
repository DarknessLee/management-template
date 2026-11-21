package com.mgmt.module.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mgmt.common.PageResult;
import com.mgmt.module.job.entity.SysJob;
import com.mgmt.module.job.entity.SysJobLog;
import com.mgmt.module.job.mapper.SysJobLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 任务执行记录：写入和分页查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobLogService {

    public static final int TRIGGER_SCHEDULED = 1;
    public static final int TRIGGER_MANUAL = 2;

    private final SysJobLogMapper jobLogMapper;

    /**
     * 记录执行结果。写日志失败不应影响任务本身，所以这里吞掉异常只打警告。
     */
    public void record(SysJob job, int triggerType, boolean success, long durationMs, String result) {
        try {
            SysJobLog record = new SysJobLog();
            record.setJobId(job.getId());
            record.setJobName(job.getName());
            record.setHandlerKey(job.getHandlerKey());
            record.setTriggerType(triggerType);
            record.setStatus(success ? 1 : 0);
            record.setDurationMs(durationMs);
            record.setResult(result != null && result.length() > 500 ? result.substring(0, 500) : result);
            jobLogMapper.insert(record);
        } catch (Exception e) {
            log.warn("任务 {} 执行记录写入失败", job.getName(), e);
        }
    }

    public PageResult<SysJobLog> logs(Long jobId, long page, long size) {
        IPage<SysJobLog> result = jobLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysJobLog>()
                        .eq(SysJobLog::getJobId, jobId)
                        .orderByDesc(SysJobLog::getCreateTime)
                        .orderByDesc(SysJobLog::getId));
        return PageResult.of(result, result.getRecords());
    }
}
