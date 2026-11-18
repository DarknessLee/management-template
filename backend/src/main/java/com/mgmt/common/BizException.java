package com.mgmt.common;

import lombok.Getter;

/**
 * 可预期的业务异常，由 GlobalExceptionHandler 转成 { code, message }。
 * 这里不抛 HTTP 500，避免前端把"用户名已存在"当成服务故障。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
