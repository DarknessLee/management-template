package com.mgmt.common;

import lombok.Data;

/**
 * 统一响应体：{ code, message, data }
 * 约定：200 成功、400 参数/业务错误、401 未登录、404 不存在、500 服务端异常。
 */
@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static Result<Void> ok() {
        return ok(null);
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
