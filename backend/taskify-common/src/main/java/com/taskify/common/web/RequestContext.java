package com.taskify.common.web;

import org.slf4j.MDC;

/**
 * 请求上下文：从 MDC 读取当前请求 ID（由 RequestIdFilter 写入）。
 */
public final class RequestContext {

    public static final String REQUEST_ID_KEY = "requestId";

    private RequestContext() {
    }

    public static String getRequestId() {
        String id = MDC.get(REQUEST_ID_KEY);
        return id == null || id.isBlank() ? "unknown" : id;
    }
}
