package com.mgmt.module.job;

import com.mgmt.common.I18n;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mgmt.common.BizException;
import com.mgmt.module.job.dto.JobHandlerVO;
import com.mgmt.module.job.dto.JobRequest;
import com.mgmt.module.job.dto.JobVO;
import com.mgmt.module.job.entity.SysJob;
import com.mgmt.module.job.mapper.SysJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final SysJobMapper jobMapper;
    private final JobScheduler jobScheduler;

    public List<JobVO> list(String keyword, Integer status) {
        LambdaQueryWrapper<SysJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), SysJob::getName, keyword)
                .eq(status != null, SysJob::getStatus, status)
                .orderByAsc(SysJob::getId);
        return jobMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    public List<JobHandlerVO> handlers() {
        return jobScheduler.handlers().values().stream()
                .map(handler -> new JobHandlerVO(handler.key(), handler.name(), handler.description()))
                .toList();
    }

    public JobVO create(JobRequest request) {
        validate(request);
        String handlerKey = request.getHandlerKey().trim();
        ensureHandlerExists(handlerKey);
        long exists = jobMapper.selectCount(new LambdaQueryWrapper<SysJob>()
                .eq(SysJob::getHandlerKey, handlerKey));
        if (exists > 0) {
            throw new BizException(I18n.get("job.handlerTaskExists"));
        }

        SysJob job = new SysJob();
        job.setName(request.getName().trim());
        job.setHandlerKey(handlerKey);
        job.setCronExpression(request.getCronExpression().trim());
        job.setStatus(request.getStatus() == null ? 0 : request.getStatus());
        job.setRemark(trimToNull(request.getRemark()));
        jobMapper.insert(job);

        jobScheduler.reschedule(job);
        return toVO(job);
    }

    public JobVO update(Long id, JobRequest request) {
        SysJob job = requireJob(id);
        validate(request);
        if (!job.getHandlerKey().equals(request.getHandlerKey().trim())) {
            throw new BizException(I18n.get("job.handlerImmutable"));
        }

        job.setName(request.getName().trim());
        job.setCronExpression(request.getCronExpression().trim());
        if (request.getStatus() != null) {
            job.setStatus(request.getStatus());
        }
        job.setRemark(trimToNull(request.getRemark()));
        jobMapper.updateById(job);

        jobScheduler.reschedule(job);
        return toVO(job);
    }

    public void delete(Long id) {
        requireJob(id);
        jobScheduler.cancel(id);
        jobMapper.deleteById(id);
    }

    public String runNow(Long id) {
        SysJob job = requireJob(id);
        return jobScheduler.runNow(job);
    }

    private void validate(JobRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException(I18n.get("job.nameRequired"));
        }
        if (!StringUtils.hasText(request.getHandlerKey())) {
            throw new BizException(I18n.get("job.handlerRequired"));
        }
        if (!StringUtils.hasText(request.getCronExpression())) {
            throw new BizException(I18n.get("job.cronRequired"));
        }
        if (!CronExpression.isValidExpression(request.getCronExpression().trim())) {
            throw new BizException(I18n.get("job.cronInvalid"));
        }
    }

    private void ensureHandlerExists(String handlerKey) {
        if (!jobScheduler.handlers().containsKey(handlerKey)) {
            throw new BizException(I18n.get("job.handlerNotFound"));
        }
    }

    private SysJob requireJob(Long id) {
        SysJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new BizException(404, I18n.get("job.notFound"));
        }
        return job;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private JobVO toVO(SysJob job) {
        JobVO vo = new JobVO();
        vo.setId(job.getId());
        vo.setName(job.getName());
        vo.setHandlerKey(job.getHandlerKey());
        JobHandler handler = jobScheduler.handlers().get(job.getHandlerKey());
        vo.setHandlerName(handler == null ? job.getHandlerKey() : handler.name());
        vo.setCronExpression(job.getCronExpression());
        vo.setStatus(job.getStatus());
        vo.setRemark(job.getRemark());
        vo.setLastRunTime(job.getLastRunTime());
        vo.setLastResult(job.getLastResult());
        vo.setCreateTime(job.getCreateTime());
        return vo;
    }
}
