package com.dotcms.ai.v2.api.aitools;

public final class ToolResult<T> {
    public final boolean ok;
    public final String errorCode; // NOT_FOUND, INVALID_ARG, PERMISSION_DENIED, TIMEOUT, INTERNAL
    public final String message;
    public final T data;

    private ToolResult(boolean ok, String errorCode, String message, T data) {
        this.ok = ok; this.errorCode = errorCode; this.message = message; this.data = data;
    }
    public static <T> ToolResult<T> denied(String msg){ return new ToolResult<T>(false, "PERMISSION_DENIED", msg, null); }
    public static <T> ToolResult<T> success(T data){ return new ToolResult<T>(true, null, null, data); }
    public static <T> ToolResult<T> notFound(String msg){ return new ToolResult<T>(false, "NOT_FOUND", msg, null); }
    public static <T> ToolResult<T> invalid(String msg){ return new ToolResult<T>(false, "INVALID_ARG", msg, null); }
    public static <T> ToolResult<T> internal(String msg){ return new ToolResult<T>(false, "INTERNAL", msg, null); }
    public static <T> ToolResult<T> fail(String msg){ return new ToolResult<T>(false, "INTERNAL", msg, null); }
}
