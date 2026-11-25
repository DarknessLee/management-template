package com.mgmt.module.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mgmt.common.BizException;
import com.mgmt.common.I18n;
import com.mgmt.common.PageResult;
import com.mgmt.module.message.dto.MessageSendRequest;
import com.mgmt.module.message.entity.SysMessage;
import com.mgmt.module.message.mapper.SysMessageMapper;
import com.mgmt.module.role.entity.SysRolePermission;
import com.mgmt.module.role.mapper.SysRolePermissionMapper;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 站内消息：写入数据库后通过 WebSocket 实时推给在线用户，
 * 离线用户下次登录可在顶栏铃铛中看到未读消息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    public static final String TYPE_NOTICE = "notice";
    public static final String TYPE_JOB = "job";
    public static final String TYPE_KICK = "kick";
    public static final String TYPE_SYSTEM = "system";

    private static final int READ = 1;
    private static final int UNREAD = 0;

    private final SysMessageMapper messageMapper;
    private final SysUserMapper userMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final MessageSocketRegistry socketRegistry;

    public PageResult<SysMessage> page(Long userId, long page, long size, Integer readFlag, String type) {
        IPage<SysMessage> result = messageMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getUserId, userId)
                        .eq(readFlag != null, SysMessage::getReadFlag, readFlag)
                        .eq(type != null && !type.isBlank(), SysMessage::getType, type)
                        .orderByDesc(SysMessage::getCreateTime)
                        .orderByDesc(SysMessage::getId));
        return PageResult.of(result, result.getRecords());
    }

    public long unreadCount(Long userId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getReadFlag, UNREAD));
    }

    public void markRead(Long id, Long userId) {
        SysMessage message = messageMapper.selectById(id);
        if (message == null || !message.getUserId().equals(userId)) {
            throw new BizException(404, I18n.get("message.notFound"));
        }
        if (Integer.valueOf(READ).equals(message.getReadFlag())) {
            return;
        }
        messageMapper.update(null, new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getId, id)
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getReadFlag, UNREAD)
                .set(SysMessage::getReadFlag, READ)
                .set(SysMessage::getReadTime, LocalDateTime.now()));
    }

    public void markAllRead(Long userId) {
        messageMapper.update(null, new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getReadFlag, UNREAD)
                .set(SysMessage::getReadFlag, READ)
                .set(SysMessage::getReadTime, LocalDateTime.now()));
    }

    public void delete(Long id, Long userId) {
        SysMessage message = messageMapper.selectById(id);
        if (message == null || !message.getUserId().equals(userId)) {
            throw new BizException(404, I18n.get("message.notFound"));
        }
        messageMapper.deleteById(id);
    }

    /** 批量删除只作用于当前用户自己的消息，混入别人的 ID 不会误删 */
    public void deleteBatch(Collection<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        messageMapper.delete(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .in(SysMessage::getId, ids));
    }

    /** 单个用户发消息 */
    public void send(Long userId, String type, String title, String content, String link) {
        sendToUsers(List.of(userId), type, title, content, link);
    }

    /** 全体启用用户发消息（公告发布等） */
    public void sendToAllEnabled(String type, String title, String content, String link) {
        List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getStatus, 1)
                        .select(SysUser::getId))
                .stream()
                .map(SysUser::getId)
                .toList();
        sendToUsers(userIds, type, title, content, link);
    }

    /** 拥有指定权限的用户发消息（定时任务失败提醒等） */
    public void sendToPermission(String permission, String type, String title, String content, String link) {
        List<Long> roleIds = rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getPermission, permission))
                .stream()
                .map(SysRolePermission::getRoleId)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            return;
        }
        List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getRoleId, roleIds)
                        .eq(SysUser::getStatus, 1)
                        .select(SysUser::getId))
                .stream()
                .map(SysUser::getId)
                .toList();
        sendToUsers(userIds, type, title, content, link);
    }

    /**
     * 管理员群发：按全员 / 角色 / 指定用户解析接收人，只发给启用中的用户。
     * 返回实际发送人数，方便前端提示。
     */
    public int send(MessageSendRequest request) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .select(SysUser::getId);

        String scope = request.getScope();
        if ("all".equals(scope)) {
            // 不加其它条件，即全员
        } else if ("role".equals(scope)) {
            if (request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
                throw new BizException(400, I18n.get("message.sendEmptyRecipients"));
            }
            wrapper.in(SysUser::getRoleId, request.getRoleIds());
        } else if ("user".equals(scope)) {
            if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
                throw new BizException(400, I18n.get("message.sendEmptyRecipients"));
            }
            wrapper.in(SysUser::getId, request.getUserIds());
        } else {
            throw new BizException(400, I18n.get("message.sendScopeInvalid"));
        }

        List<Long> userIds = userMapper.selectList(wrapper).stream()
                .map(SysUser::getId)
                .toList();
        if (userIds.isEmpty()) {
            throw new BizException(400, I18n.get("message.sendEmptyRecipients"));
        }
        return sendToUsers(userIds, TYPE_SYSTEM, request.getTitle(), request.getContent(), request.getLink());
    }

    /**
     * 逐人落库并推送。消息只是通知能力，单条失败不影响主业务。
     */
    public int sendToUsers(Collection<Long> userIds, String type, String title, String content, String link) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        int sent = 0;
        for (Long userId : new LinkedHashSet<>(userIds)) {
            try {
                SysMessage message = new SysMessage();
                message.setUserId(userId);
                message.setType(type);
                message.setTitle(truncate(title, 100));
                message.setContent(truncate(content, 500));
                message.setLink(truncate(link, 200));
                message.setReadFlag(UNREAD);
                messageMapper.insert(message);

                Map<String, Object> payload = new HashMap<>();
                payload.put("event", "message");
                payload.put("data", message);
                payload.put("unreadCount", unreadCount(userId));
                socketRegistry.send(userId, payload);
                sent++;
            } catch (Exception e) {
                log.warn("站内消息发送失败，用户 {}：{}", userId, e.getMessage());
            }
        }
        return sent;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
