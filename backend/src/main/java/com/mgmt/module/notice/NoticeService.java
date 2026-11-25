package com.mgmt.module.notice;

import com.mgmt.common.I18n;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mgmt.common.BizException;
import com.mgmt.common.HtmlSanitizer;
import com.mgmt.common.PageResult;
import com.mgmt.module.message.MessageService;
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
    private final MessageService messageService;

    public PageResult<SysNotice> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), SysNotice::getTitle, keyword)
                .eq(status != null, SysNotice::getStatus, status)
                .orderByDesc(SysNotice::getPinned)
                .orderByDesc(SysNotice::getCreateTime)
                .orderByDesc(SysNotice::getId);

        IPage<SysNotice> result = noticeMapper.selectPage(new Page<>(page, size), wrapper);
        // 已设置定时且仍处于下线状态，列表里标记为「待发布」（到点由任务上线）
        result.getRecords().forEach(notice -> notice.setScheduled(
                Integer.valueOf(STATUS_OFFLINE).equals(notice.getStatus())
                        && notice.getScheduledTime() != null));
        attachReadCount(result.getRecords());
        return PageResult.of(result, result.getRecords());
    }

    /**
     * 公告中心：所有登录用户只看已发布公告，并标记当前用户的已读状态。
     */
    public PageResult<SysNotice> centerPage(long page, long size, String keyword, Long userId) {
        IPage<SysNotice> result = noticeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysNotice>()
                        .eq(SysNotice::getStatus, STATUS_PUBLISHED)
                        .like(StringUtils.hasText(keyword), SysNotice::getTitle, keyword)
                        .orderByDesc(SysNotice::getPinned)
                        .orderByDesc(SysNotice::getPublishTime)
                        .orderByDesc(SysNotice::getId));
        attachReadFlag(result.getRecords(), userId);
        return PageResult.of(result, result.getRecords());
    }

    /**
     * 列表页一次性补齐当前用户的已读状态，避免逐条查询。
     */
    private void attachReadFlag(List<SysNotice> notices, Long userId) {
        if (notices.isEmpty()) {
            return;
        }
        Set<Long> readIds = readMapper.selectList(new LambdaQueryWrapper<SysNoticeRead>()
                        .eq(SysNoticeRead::getUserId, userId)
                        .in(SysNoticeRead::getNoticeId, notices.stream().map(SysNotice::getId).toList()))
                .stream()
                .map(SysNoticeRead::getNoticeId)
                .collect(Collectors.toSet());
        notices.forEach(notice -> notice.setReadFlag(readIds.contains(notice.getId()) ? 1 : 0));
    }

    public SysNotice create(NoticeRequest request) {
        validate(request);
        SysNotice notice = new SysNotice();
        notice.setTitle(request.getTitle().trim());
        notice.setContent(sanitizeContent(request.getContent()));
        notice.setPinned(normalizePinned(request.getPinned()));
        notice.setExpireTime(validateExpire(request.getExpireTime()));
        // 新建先保持下线状态，由管理员确认后再发布
        notice.setStatus(STATUS_OFFLINE);
        noticeMapper.insert(notice);
        return notice;
    }

    public SysNotice update(Long id, NoticeRequest request) {
        requireNotice(id);
        validate(request);
        // 用 UpdateWrapper 才能把到期时间真正清空（updateById 会忽略 null 字段）
        noticeMapper.update(null, new LambdaUpdateWrapper<SysNotice>()
                .eq(SysNotice::getId, id)
                .set(SysNotice::getTitle, request.getTitle().trim())
                .set(SysNotice::getContent, sanitizeContent(request.getContent()))
                .set(SysNotice::getPinned, normalizePinned(request.getPinned()))
                .set(SysNotice::getExpireTime, validateExpire(request.getExpireTime()))
                .set(SysNotice::getUpdateTime, LocalDateTime.now()));
        return requireNotice(id);
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
     * 发布或下线。发布立即生效并清掉定时；下线同时取消未到点的定时发布。
     */
    public SysNotice updateStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_OFFLINE && status != STATUS_PUBLISHED)) {
            throw new BizException(I18n.get("notice.statusInvalid"));
        }
        SysNotice notice = requireNotice(id);
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<SysNotice> wrapper = new LambdaUpdateWrapper<SysNotice>()
                .eq(SysNotice::getId, id)
                .set(SysNotice::getStatus, status)
                .set(SysNotice::getScheduledTime, null)
                .set(SysNotice::getUpdateTime, now);
        if (status == STATUS_PUBLISHED) {
            wrapper.set(SysNotice::getPublishTime, now);
        }
        noticeMapper.update(null, wrapper);
        if (status == STATUS_PUBLISHED) {
            notifyPublished(notice);
        }
        return requireNotice(id);
    }

    /**
     * 发布公告：发布时间晚于当前时间则进入待发布，由定时任务到点自动上线。
     */
    @Transactional
    public SysNotice publish(Long id, LocalDateTime scheduledTime, LocalDateTime expireTime) {
        SysNotice notice = requireNotice(id);
        if (scheduledTime == null) {
            throw new BizException(I18n.get("notice.scheduleTimeRequired"));
        }
        LocalDateTime now = LocalDateTime.now();
        boolean future = scheduledTime.isAfter(now);
        LocalDateTime effectiveStart = future ? scheduledTime : now;
        if (expireTime != null && !expireTime.isAfter(effectiveStart)) {
            throw new BizException(I18n.get("notice.expireInvalid"));
        }

        LambdaUpdateWrapper<SysNotice> wrapper = new LambdaUpdateWrapper<SysNotice>()
                .eq(SysNotice::getId, id)
                .set(SysNotice::getExpireTime, expireTime)
                .set(SysNotice::getUpdateTime, now);
        if (future) {
            wrapper.set(SysNotice::getStatus, STATUS_OFFLINE)
                    .set(SysNotice::getScheduledTime, scheduledTime)
                    .set(SysNotice::getPublishTime, null);
        } else {
            wrapper.set(SysNotice::getStatus, STATUS_PUBLISHED)
                    .set(SysNotice::getScheduledTime, null)
                    .set(SysNotice::getPublishTime, now);
        }
        noticeMapper.update(null, wrapper);
        if (!future) {
            notifyPublished(notice);
        }
        return requireNotice(id);
    }

    /**
     * 定时任务入口：到点发布已定时的公告，并下线已到期的公告。
     * 返回本次自动发布的公告数量。
     */
    @Transactional
    public int applySchedule() {
        LocalDateTime now = LocalDateTime.now();
        int published = 0;

        List<SysNotice> due = noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, STATUS_OFFLINE)
                .isNotNull(SysNotice::getScheduledTime)
                .le(SysNotice::getScheduledTime, now));
        for (SysNotice notice : due) {
            if (notice.getExpireTime() != null && !notice.getExpireTime().isAfter(now)) {
                // 还没发布就已经过期，直接取消定时
                noticeMapper.update(null, new LambdaUpdateWrapper<SysNotice>()
                        .eq(SysNotice::getId, notice.getId())
                        .set(SysNotice::getScheduledTime, null)
                        .set(SysNotice::getUpdateTime, now));
                continue;
            }
            noticeMapper.update(null, new LambdaUpdateWrapper<SysNotice>()
                    .eq(SysNotice::getId, notice.getId())
                    .set(SysNotice::getStatus, STATUS_PUBLISHED)
                    .set(SysNotice::getPublishTime, notice.getScheduledTime())
                    .set(SysNotice::getScheduledTime, null)
                    .set(SysNotice::getUpdateTime, now));
            notifyPublished(notice);
            published++;
        }

        // 已发布且到达到期时间：自动下线
        noticeMapper.update(null, new LambdaUpdateWrapper<SysNotice>()
                .eq(SysNotice::getStatus, STATUS_PUBLISHED)
                .isNotNull(SysNotice::getExpireTime)
                .le(SysNotice::getExpireTime, now)
                .set(SysNotice::getStatus, STATUS_OFFLINE)
                .set(SysNotice::getUpdateTime, now));
        return published;
    }

    private void notifyPublished(SysNotice notice) {
        messageService.sendToAllEnabled(MessageService.TYPE_NOTICE,
                I18n.get("message.noticeTitle", notice.getTitle()),
                HtmlSanitizer.plainText(notice.getContent()),
                null);
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

    /**
     * 当前用户未读的已发布公告数量，公告中心顶部展示用。
     */
    public long unreadCount(Long userId) {
        List<Long> publishedIds = noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                        .eq(SysNotice::getStatus, STATUS_PUBLISHED)
                        .select(SysNotice::getId))
                .stream()
                .map(SysNotice::getId)
                .toList();
        if (publishedIds.isEmpty()) {
            return 0;
        }
        long readCount = readMapper.selectCount(new LambdaQueryWrapper<SysNoticeRead>()
                .eq(SysNoticeRead::getUserId, userId)
                .in(SysNoticeRead::getNoticeId, publishedIds));
        return Math.max(0, publishedIds.size() - readCount);
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

    private LocalDateTime validateExpire(LocalDateTime expireTime) {
        if (expireTime != null && !expireTime.isAfter(LocalDateTime.now())) {
            throw new BizException(I18n.get("notice.expireInvalid"));
        }
        return expireTime;
    }

    /**
     * 公告正文存富文本，入口统一做白名单过滤，避免脚本被带到所有用户的阅读弹窗。
     */
    private String sanitizeContent(String raw) {
        String clean = HtmlSanitizer.clean(raw.trim());
        if (!StringUtils.hasText(clean)) {
            throw new BizException(I18n.get("notice.contentRequired"));
        }
        if (clean.length() > 20000) {
            throw new BizException(I18n.get("notice.contentTooLong"));
        }
        return clean;
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
