package com.dotcms.content.elasticsearch.util;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public class HttpGetWithEntity extends HttpEntityEnclosingRequestBase {

    public final static String METHOD_NAME = "GET";

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }

}
