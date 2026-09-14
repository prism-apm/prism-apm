package org.prism.apm.ui.server.exceptions;

import org.prism.apm.ui.server.entity.response.ViewResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ViewResponse.Result<?> bizExceptionHandle(BizException e) {
        log.error("BizException: ", e);
        return ViewResponse.Result.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ViewResponse.Result<?> exceptionHandle(Exception e) {
        log.error("Unknown exception", e);
        return ViewResponse.Result.fail("Internal server error");
    }

}
