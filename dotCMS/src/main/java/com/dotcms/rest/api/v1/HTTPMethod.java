package com.dotcms.rest.api.v1;

public enum HTTPMethod {
    GET("get"),
    POST("post"),
    PUT("put"),
    PATCH("patch"),
    DELETE("delete");

    private String fileName;

    HTTPMethod(final String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
