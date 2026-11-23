package com.mgmt.module.notice;

import com.mgmt.common.I18n;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mgmt.common.BizException;
import com.mgmt.common.PageResult;
import com.mgmt.module.notice.dto.NoticeReadStatsVO;
import com.mgmt.module.notice.dto.NoticeReadVO;
import com.mgmt.module.notice.dto.NoticeRequest;
import com.mgmt.module.notice.entity.SysNotice;
import com.mgmt.module.notice.entity.SysNoticeRead;
import com.mgmt.module.notice.mapper.SysNoticeMapper;
import com.mgmt.module.notice.mapper.SysNoticeReadMapper;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知公告：管理端维护与发布/下线，所有登录用户可读取未读公告并标记已读。
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final int STATUS_OFFLINE = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int PINNED = 1;
    private static final int UNPINNED = 0;

    private final SysNoticeMapper noticeMapper;
    private final SysNoticeReadMapper readMapper;
    private final SysUserMapper userMapper;

    public PageResult<SysNotice> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), SysNotice::getTitle, keyword)
                .eq(status != null, SysNotice::getStatus, status)
                .orderByDesc(SysNotice::getPinned)
                .orderByDesc(SysNotice::getCreateTime)
                .orderByDesc(SysNotice::getId);

        IPage<SysNotice> result = noticeMapper.selectPage(new Page<>(page, size), wrapper);
        attachReadCount(result.getRecords());
        return PageResult.of(result, result.getRecords());
    }

    public SysNotice create(NoticeRequest request) {
        validate(request);
        SysNotice notice = new SysNotice();
        notice.setTitle(request.getTitle().trim());
        notice.setContent(request.getContent().trim());
        notice.setPinned(normalizePinned(request.getPinned()));
        // 新建先保持下线状态，由管理员确认后再发布
        notice.setStatus(STATUS_OFFLINE);
        noticeMapper.insert(notice);
        return notice;
    }

    public SysNotice update(Long id, NoticeRequest request) {
        SysNotice notice = requireNotice(id);
        validate(request);
        notice.setTitle(request.getTitle().trim());
        notice.setContent(request.getContent().trim());
        notice.setPinned(normalizePinned(request.getPinned()));
        noticeMapper.updateById(notice);
        return notice;
    }

    /**
     * 置顶/取消置顶，置顶公告在列表和未读提醒中优先展示。
     */
    public SysNotice updatePinned(Long id, Integer pinned) {
        if (pinned == null || (pinned != PINNED && pinned != UNPINNED)) {
            throw new BizException(I18n.get("notice.pinInvalid"));
        }
        SysNotice notice = requireNotice(id);
        notice.setPinned(pinned);
        noticeMapper.updateById(notice);
        return notice;
    }

    /**
     * 发布或下线。重复发布时刷新发布时间，让公告回到未读列表顶部。
     */
    public SysNotice updateStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_OFFLINE && status != STATUS_PUBLISHED)) {
            throw new BizException(I18n.get("notice.statusInvalid"));
        }
        SysNotice notice = requireNotice(id);
        notice.setStatus(status);
        if (status == STATUS_PUBLISHED) {
            notice.setPublishTime(LocalDateTime.now());
        }
        noticeMapper.updateById(notice);
        return notice;
    }

    @Transactional
    public void delete(Long id) {
        requireNotice(id);
        readMapper.delete(new LambdaQueryWrapper<SysNoticeRead>().eq(SysNoticeRead::getNoticeId, id));
        noticeMapper.deleteById(id);
    }

    /**
     * 当前用户未读的已发布公告，按发布时间倒序，最多返回 20 条。
     * 公告数量少，直接把已读记录取出在内存里过滤，避免写子查询。
     */
    public List<SysNotice> unread(Long userId) {
        List<SysNotice> published = noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, STATUS_PUBLISHED)
                .orderByDesc(SysNotice::getPinned)
                .orderByDesc(SysNotice::getPublishTime)
                .orderByDesc(SysNotice::getId));
        if (published.isEmpty()) {
            return List.of();
        }

        Set<Long> readIds = readMapper.selectList(new LambdaQueryWrapper<SysNoticeRead>()
                        .eq(SysNoticeRead::getUserId, userId))
                .stream()
                .map(SysNoticeRead::getNoticeId)
                .collect(Collectors.toSet());

        return published.stream()
                .filter(notice -> !readIds.contains(notice.getId()))
                .limit(20)
                .toList();
    }

    public void markRead(Long noticeId, Long userId) {
        SysNotice notice = requireNotice(noticeId);
        if (!Integer.valueOf(STATUS_PUBLISHED).equals(notice.getStatus())) {
            throw new BizException(I18n.get("notice.offline"));
        }
        long exists = readMapper.selectCount(new LambdaQueryWrapper<SysNoticeRead>()
                .eq(SysNoticeRead::getNoticeId, noticeId)
                .eq(SysNoticeRead::getUserId, userId));
        if (exists > 0) {
            return;
        }
        SysNoticeRead record = new SysNoticeRead();
        record.setNoticeId(noticeId);
        record.setUserId(userId);
        record.setReadTime(LocalDateTime.now());
        readMapper.insert(record);
    }

    /**
     * 已读明细：分页返回阅读人，用户已删除时保留占位。
     */
    public PageResult<NoticeReadVO> reads(Long noticeId, long page, long size) {
        requireNotice(noticeId);
        IPage<SysNoticeRead> result = readMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysNoticeRead>()
                        .eq(SysNoticeRead::getNoticeId, noticeId)
                        .orderByDesc(SysNoticeRead::getReadTime));

        List<SysNoticeRead> records = result.getRecords();
        Map<Long, SysUser> users = records.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(records.stream().map(SysNoticeRead::getUserId).toList())
                        .stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        List<NoticeReadVO> list = records.stream().map(read -> {
            NoticeReadVO vo = new NoticeReadVO();
            vo.setUserId(read.getUserId());
            SysUser user = users.get(read.getUserId());
            vo.setUsername(user == null ? "已删除用户" : user.getUsername());
            vo.setRealName(user == null ? "—" : user.getRealName());
            vo.setReadTime(read.getReadTime());
            return vo;
        }).toList();
        return PageResult.of(result, list);
    }

    public NoticeReadStatsVO readStats(Long noticeId) {
        requireNotice(noticeId);
        long readCount = readMapper.selectCount(new LambdaQueryWrapper<SysNoticeRead>()
                .eq(SysNoticeRead::getNoticeId, noticeId));
        long totalUsers = userMapper.selectCount(null);
        NoticeReadStatsVO stats = new NoticeReadStatsVO();
        stats.setNoticeId(noticeId);
        stats.setReadCount(readCount);
        stats.setTotalUsers(totalUsers);
        stats.setUnreadCount(Math.max(0, totalUsers - readCount));
        return stats;
    }

    /**
     * 列表页一次性补齐每条公告的已读人数，避免逐条查询。
     */
    private void attachReadCount(List<SysNotice> notices) {
        if (notices.isEmpty()) {
            return;
        }
        List<Long> ids = notices.stream().map(SysNotice::getId).toList();
        List<SysNoticeRead> reads = readMapper.selectList(new LambdaQueryWrapper<SysNoticeRead>()
                .in(SysNoticeRead::getNoticeId, ids)
                .select(SysNoticeRead::getNoticeId));
        Map<Long, Long> counts = reads.stream()
                .collect(Collectors.groupingBy(SysNoticeRead::getNoticeId, Collectors.counting()));
        notices.forEach(notice -> notice.setReadCount(counts.getOrDefault(notice.getId(), 0L)));
    }

    private Integer normalizePinned(Integer pinned) {
        return pinned != null && pinned == PINNED ? PINNED : UNPINNED;
    }

    private void validate(NoticeRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BizException(I18n.get("notice.titleRequired"));
        }
        if (request.getTitle().trim().length() > 100) {
            throw new BizException(I18n.get("notice.titleTooLong"));
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new BizException(I18n.get("notice.contentRequired"));
        }
    }

    private SysNotice requireNotice(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BizException(404, I18n.get("notice.notFound"));
        }
        return notice;
    }
}
