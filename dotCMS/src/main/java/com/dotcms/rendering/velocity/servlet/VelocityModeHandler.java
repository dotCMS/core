package com.dotcms.rendering.velocity.servlet;

import com.dotmarketing.exception.DotRuntimeException;

import java.io.StringWriter;
import java.io.Writer;

public interface VelocityModeHandler {
    void serve() throws Exception ;

    void serve(Writer out) throws Exception ;
    
    default String eval() {
        StringWriter out = new StringWriter(4096);
        try {
            serve(out);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
        return out.toString();
    }
}
