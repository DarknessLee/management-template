package com.mgmt.module.setting;

import com.mgmt.common.I18n;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgmt.common.BizException;
import com.mgmt.module.setting.entity.SysSetting;
import com.mgmt.module.setting.mapper.SysSettingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingService {

    private final SysSettingMapper settingMapper;
    private final ObjectMapper objectMapper;

    /** 登录页等未登录场景可以读取的品牌配置 */
    private static final List<String> PUBLIC_BASIC_KEYS =
            List.of("siteTitle", "siteLogo", "defaultTheme", "language");

    /**
     * 前端"重置"按钮使用的默认值，与种子数据保持一致。
     */
    public static Map<String, Object> defaults() {
        Map<String, Object> security = Map.of(
                "loginTimeout", 30,
                "passwordPolicyEnabled", false,
                "passwordMinLength", 8,
                "passwordRequireComplexity", true,
                "passwordExpire", 90,
                "lockOnFail", true,
                "maxFailCount", 5,
                "watermark", false);
        Map<String, Object> basic = Map.of(
                "siteTitle", "后台管理系统",
                "siteLogo", "",
                "defaultTheme", "light",
                "language", "zh-CN");
        Map<String, Object> notification = Map.of(
                "emailNotify", true,
                "smsNotify", false,
                "systemNotify", true,
                "pushNotify", false);

        Map<String, Object> all = new LinkedHashMap<>();
        all.put("security", security);
        all.put("basic", basic);
        all.put("notification", notification);
        return all;
    }

    public Map<String, Object> get() {
        Map<String, Object> result = defaults();
        for (SysSetting setting : settingMapper.selectList(null)) {
            try {
                Map<String, Object> value = objectMapper.readValue(
                        setting.getSettingJson(), new TypeReference<>() {
                        });
                // 与默认值合并，老库保存过的分组缺少新增键时仍能读到默认配置
                Map<String, Object> merged = new LinkedHashMap<>();
                Object defaultValue = result.get(setting.getSettingKey());
                if (defaultValue instanceof Map<?, ?> defaultsMap) {
                    defaultsMap.forEach((k, v) -> merged.put(String.valueOf(k), v));
                }
                merged.putAll(value);
                result.put(setting.getSettingKey(), merged);
            } catch (Exception e) {
                // 单组配置损坏不影响其他组返回，降级为默认值
                log.warn("配置 {} 解析失败，使用默认值", setting.getSettingKey(), e);
            }
        }
        return result;
    }

    /**
     * 安全分组配置，登录逻辑需要读取失败锁定与会话超时设置。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> security() {
        Object value = get().get("security");
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    /**
     * 只暴露品牌相关字段，避免未登录场景读到安全配置。
     */
    public Map<String, Object> publicBasic() {
        Map<String, Object> all = get();
        Map<String, Object> result = new LinkedHashMap<>();
        Object value = all.get("basic");
        if (value instanceof Map<?, ?> basic) {
            for (String key : PUBLIC_BASIC_KEYS) {
                if (basic.containsKey(key)) {
                    result.put(key, basic.get(key));
                }
            }
        }
        // 页面水印开关需要所有登录用户可见，但不暴露其他安全配置
        Object security = all.get("security");
        if (security instanceof Map<?, ?> securityMap) {
            result.put("watermark", Boolean.TRUE.equals(securityMap.get("watermark")));
        }
        return result;
    }

    @Transactional
    public void update(Map<String, Object> body) {
        if (body == null) {
            throw new BizException(I18n.get("setting.contentRequired"));
        }
        for (String key : defaults().keySet()) {
            if (!body.containsKey(key)) {
                continue;
            }
            String json;
            try {
                json = objectMapper.writeValueAsString(body.get(key));
            } catch (Exception e) {
                throw new BizException(I18n.get("setting.formatInvalid", key));
            }
            save(key, json);
        }
    }

    @Transactional
    public void initDefaultsIfEmpty() {
        if (settingMapper.selectCount(null) > 0) {
            return;
        }
        defaults().forEach((key, value) -> {
            try {
                save(key, objectMapper.writeValueAsString(value));
            } catch (Exception e) {
                throw new IllegalStateException("初始化配置失败：" + key, e);
            }
        });
    }

    private void save(String key, String json) {
        SysSetting setting = settingMapper.selectById(key);
        if (setting == null) {
            setting = new SysSetting();
            setting.setSettingKey(key);
            setting.setSettingJson(json);
            settingMapper.insert(setting);
        } else {
            setting.setSettingJson(json);
            settingMapper.updateById(setting);
        }
    }
}
