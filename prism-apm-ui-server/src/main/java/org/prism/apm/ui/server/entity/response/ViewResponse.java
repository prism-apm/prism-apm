package org.prism.apm.ui.server.entity.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Unified view response wrapper object
 * Contains two forms: common result (Result) and paginated result (Page)
 */
@Data
public class ViewResponse implements Serializable {

    public static final Integer OK = 200;
    public static final Integer ERR = 500;

    // --- Common fields ---
    protected Integer status;
    protected String msg;
    protected String err;
    protected String traceId;

    /**
     * Form 1: common single-object response
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Result<T> extends ViewResponse {
        private T data;

        /**
         * Success - no data
         */
        public static <T> Result<T> ok() {
            Result<T> r = new Result<>();
            r.setStatus(OK);
            r.setMsg("OK");
            return r;
        }

        /**
         * Success - with data
         */
        public static <T> Result<T> ok(T data) {
            Result<T> r = new Result<>();
            r.setStatus(OK);
            r.setMsg("OK");
            r.setData(data);
            return r;
        }

        /**
         * Failure - with error message
         */
        public static <T> Result<T> fail(String err) {
            Result<T> r = new Result<>();
            r.setStatus(ERR);
            r.setErr(err);
            return r;
        }
    }

    /**
     * Form 2: paginated list response
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Page<T> extends ViewResponse {
        private List<T> data;
        private Long total;

        /**
         * Success - return paginated data
         */
        public static <T> Page<T> ok(long total, List<T> data) {
            Page<T> pageR = new Page<>();
            pageR.setStatus(OK);
            pageR.setMsg("OK");
            pageR.setTotal(total);
            pageR.setData(data);
            return pageR;
        }

        /**
         * Success - return empty page
         */
        public static <T> Page<T> okEmpty() {
            return ok(0L, Collections.emptyList());
        }

    }
}