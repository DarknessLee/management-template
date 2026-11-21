package com.mgmt.module.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mgmt.common.PageResult;
import com.mgmt.module.log.entity.SysOperationLog;
import com.mgmt.module.log.mapper.SysOperationLogMapper;
import com.mgmt.module.role.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationLogService {

    private final SysOperationLogMapper logMapper;
    private final DataScopeService dataScopeService;

    /**
     * 异步落库，避免写日志拖慢业务接口的响应时间。
     * 注意：切面必须在调用本方法前把用户、IP、参数都取好，
     * 因为 @Async 之后线程变了，ThreadLocal 和 RequestContext 都拿不到。
     */
    @Async("operationLogExecutor")
    public void saveAsync(SysOperationLog entity) {
        try {
            logMapper.insert(entity);
        } catch (Exception e) {
            // 日志失败不能影响业务，只记录到应用日志
            log.warn("写入操作日志失败：{}", e.getMessage());
        }
    }

    /**
     * 分批删除指定时间之前的日志，返回删除总条数。
     * 每批一个短事务；单次任务最多循环 100 轮，防止积压过多时一直占着调度线程。
     */
    public int deleteBefore(LocalDateTime before, int batchSize) {
        int total = 0;
        int rounds = 0;
        int deleted;
        do {
            deleted = logMapper.deleteBefore(before, batchSize);
            total += deleted;
            rounds++;
        } while (deleted == batchSize && rounds < 100);
        return total;
    }

    public PageResult<SysOperationLog> page(long page, long size, String module, String keyword,
                                            LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SysOperationLog> wrapper = searchWrapper(module, keyword, startTime, endTime);
        wrapper.orderByDesc(SysOperationLog::getId);

        IPage<SysOperationLog> result = logMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result, result.getRecords());
    }

    /**
     * 导出用：不分页，筛选条件与列表页保持一致。
     */
    public List<SysOperationLog> listForExport(String module, String keyword,
                                               LocalDateTime startTime, LocalDateTime endTime) {
        return logMapper.selectList(searchWrapper(module, keyword, startTime, endTime)
                .orderByDesc(SysOperationLog::getId));
    }

    private LambdaQueryWrapper<SysOperationLog> searchWrapper(String module, String keyword,
                                                              LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(module), SysOperationLog::getModule, module)
                .and(StringUtils.hasText(keyword), w -> w.like(SysOperationLog::getUsername, keyword)
                        .or().like(SysOperationLog::getAction, keyword)
                        .or().like(SysOperationLog::getUri, keyword))
                .ge(startTime != null, SysOperationLog::getCreateTime, startTime)
                .le(endTime != null, SysOperationLog::getCreateTime, endTime);

        // 数据范围受限时只看范围内用户的日志；空集合用 -1 兜底，避免生成非法 IN 条件
        List<Long> visibleUserIds = dataScopeService.visibleUserIds();
        if (visibleUserIds != null) {
            if (visibleUserIds.isEmpty()) {
                wrapper.eq(SysOperationLog::getId, -1L);
            } else {
                wrapper.in(SysOperationLog::getUserId, visibleUserIds);
            }
        }
        return wrapper;
    }
}
