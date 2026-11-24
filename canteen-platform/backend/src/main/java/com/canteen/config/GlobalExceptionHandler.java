package com.canteen.config;

import com.canteen.dto.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handle(Exception e) {
        log.error("接口异常", e);
        return ApiResult.error(e.getMessage() == null ? "服务器内部错误" : e.getMessage());
    }
}
