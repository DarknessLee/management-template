package com.mgmt.module.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录安全概览：锁定阈值与当前被临时锁定的账号。
 */
@Data
public class LoginSecurityVO {

    /** 连续失败达到该次数后锁定 */
    private int maxFailCount;

    private List<LockedAccount> lockedAccounts = new ArrayList<>();

    @Data
    public static class LockedAccount {
        private String username;
        private int failures;
        /** 剩余锁定秒数 */
        private long remainingSeconds;
    }
}
