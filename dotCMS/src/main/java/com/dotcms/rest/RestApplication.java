package com.dotcms.rest;

import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.WebInitParam;
import org.glassfish.jersey.servlet.ServletContainer;

@WebServlet(
    name = "RESTAPI",
    loadOnStartup = 1,
    urlPatterns = {"/api/*"},
    initParams = {
        @WebInitParam(name = "jersey.config.server.mediaTypeMappings", 
            value = "txt : text/plain, xml : application/xml, json : application/json, js : application/javascript")
    },
    asyncSupported = true
)
public class RestApplication extends ServletContainer {
    // ... existing code ...
} 