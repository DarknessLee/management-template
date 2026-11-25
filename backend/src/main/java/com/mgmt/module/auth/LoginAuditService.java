package com.mgmt.module.auth;

import com.mgmt.module.auth.entity.LoginLog;
import com.mgmt.module.auth.mapper.LoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录失败记录需要独立事务：登录方法抛异常时主事务会回滚，
 * 若共用事务失败日志也会被一起回滚。
 */
@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final LoginLogMapper loginLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String username, Long userId, String reason) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setLoginTime(LocalDateTime.now());
        log.setSuccess(0);
        log.setFailReason(reason);
        loginLogMapper.insert(log);
    }
}
