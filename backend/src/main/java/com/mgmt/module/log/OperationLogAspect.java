package com.mgmt.module.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgmt.common.UserContext;
import com.mgmt.module.log.entity.SysOperationLog;
import com.mgmt.module.user.entity.SysUser;
import com.mgmt.module.user.mapper.SysUserMapper;
import com.mgmt.security.OperationLog;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 操作日志切面。
 * 执行顺序：过滤器 → 拦截器（登录/权限）→ 本切面 → Controller 方法，
 * 所以这里能读到拦截器写入的 UserContext。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int MAX_PARAM_LENGTH = 2000;
    private static final int MAX_ERROR_LENGTH = 500;

    private final OperationLogService logService;
    private final ObjectMapper objectMapper;
    private final SysUserMapper userMapper;

    @Around("@annotation(operationLog)")
    public Object record(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        // 以下信息必须在当前请求线程取好，@Async 之后线程切换就取不到了
        long start = System.currentTimeMillis();
        Long userId = UserContext.getUserId();
        String username = usernameOf(userId);
        HttpServletRequest request = currentRequest();
        String ip = clientIp(request);
        String httpMethod = request == null ? "" : request.getMethod();
        String uri = request == null ? "" : request.getRequestURI();
        String params = serializeArgs(joinPoint.getArgs());

        boolean success = true;
        String errorMsg = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            success = false;
            errorMsg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw e;
        } finally {
            SysOperationLog entity = new SysOperationLog();
            entity.setUserId(userId);
            entity.setUsername(username);
            entity.setModule(operationLog.module());
            entity.setAction(operationLog.action());
            entity.setMethod(httpMethod);
            entity.setUri(uri);
            entity.setParams(params);
            entity.setIp(ip);
            entity.setDurationMs(System.currentTimeMillis() - start);
            entity.setSuccess(success ? 1 : 0);
            entity.setErrorMsg(truncate(errorMsg, MAX_ERROR_LENGTH));
            entity.setCreateTime(LocalDateTime.now());
            logService.saveAsync(entity);
        }
    }

    private String usernameOf(Long userId) {
        if (userId == null) {
            return "";
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? "" : user.getUsername();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 参数转 JSON：过滤 Servlet 对象和上传文件，密码脱敏，超长截断。
     * 脱敏必须发生在入库之前，日志一旦写了明文密码就无法挽回。
     */
    private String serializeArgs(Object[] args) {
        try {
            List<Object> payload = Arrays.stream(args)
                    .filter(arg -> !(arg instanceof ServletRequest)
                            && !(arg instanceof ServletResponse)
                            && !(arg instanceof MultipartFile))
                    .toList();
            String json = objectMapper.writeValueAsString(payload);
            json = json.replaceAll("(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1******$2");
            return truncate(json, MAX_PARAM_LENGTH);
        } catch (Exception e) {
            return "[参数序列化失败]";
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
