package com.bradmcevoy.http;

import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServlet;

@WebServlet(
    name = "WebDav",
    urlPatterns = {
        "/webdav/autopub/*",
        "/webdav/nonpub/*",
        "/webdav/live/*",
        "/webdav/working/*"
    },
    initParams = {
        @WebInitParam(name = "resource.factory.class", value = "com.dotmarketing.webdav.ResourceFactoryImpl")
    }
)
public class MiltonServlet extends HttpServlet {
    // ... existing code ...
} 