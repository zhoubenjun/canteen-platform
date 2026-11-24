package com.canteen.dto;

import lombok.Data;

/** 统一 API 返回结构 */
@Data
public class ApiResult<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> error(String message) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 1;
        r.message = message;
        return r;
    }
}
