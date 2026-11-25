package com.mgmt.module.user;

import com.mgmt.common.BizException;
import com.mgmt.common.I18n;
import com.mgmt.module.setting.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 密码策略：是否启用、最小长度、是否要求字母+数字、有效天数。
 * 新增用户、重置密码、个人修改密码统一走 {@link #validate(String)}；
 * 登录后由 {@link #isExpired(LocalDateTime)} 判断是否需要强制改密。
 */
@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    private final SettingService settingService;

    public Policy policy() {
        Map<String, Object> security = settingService.security();
        return new Policy(
                boolValue(security, "passwordPolicyEnabled", false),
                intValue(security, "passwordMinLength", 8),
                boolValue(security, "passwordRequireComplexity", true),
                intValue(security, "passwordExpire", 90));
    }

    /**
     * 未启用策略时不做额外限制，仍由接口上的 @Size 兜底基础长度。
     */
    public void validate(String password) {
        Policy policy = policy();
        if (!policy.enabled()) {
            return;
        }
        if (password == null || password.length() < policy.minLength()) {
            throw new BizException(400, I18n.get("password.tooShort", policy.minLength()));
        }
        if (policy.requireComplexity() && !containsLetterAndDigit(password)) {
            throw new BizException(400, I18n.get("password.needLetterAndDigit"));
        }
    }

    /**
     * 未启用策略或有效天数为 0 时永不过期；
     * 没有密码修改时间的用户视为已过期，强制重新设置密码。
     */
    public boolean isExpired(LocalDateTime passwordUpdateTime) {
        Policy policy = policy();
        if (!policy.enabled() || policy.expireDays() <= 0) {
            return false;
        }
        if (passwordUpdateTime == null) {
            return true;
        }
        return passwordUpdateTime.plusDays(policy.expireDays()).isBefore(LocalDateTime.now());
    }

    private boolean containsLetterAndDigit(String password) {
        boolean letter = false;
        boolean digit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                letter = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            }
            if (letter && digit) {
                return true;
            }
        }
        return false;
    }

    private boolean boolValue(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private int intValue(Map<String, Object> map, String key, int fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                // 配置被手工改成非数字时回退默认值
            }
        }
        return fallback;
    }

    public record Policy(boolean enabled, int minLength, boolean requireComplexity, int expireDays) {
    }
}
