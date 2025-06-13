package com.dotcms.config;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Handles programmatic registration of remaining web.xml configuration elements
 */
@WebListener
public class WebAppInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        
        try {
            // Configure MIME mappings
            configureMimeMappings(context);
            
            // Configure error pages
            configureErrorPages(context);
            
            // Configure JSP properties
            configureJspProperties(context);
            
            // Configure security constraints
            configureSecurityConstraints(context);
            
            Logger.info(this, "Web application configuration initialized successfully");
        } catch (Exception e) {
            Logger.error(this, "Failed to initialize web application configuration", e);
            throw new RuntimeException("Failed to initialize web application configuration", e);
        }
    }

    private void configureMimeMappings(ServletContext context) {
        Map<String, String> mimeMappings = Map.of(
            "vtl", "text/velocity",
            "xsl", "application/xml",
            "dotsass", "text/css",
            "sass", "text/css",
            "scss", "text/css",
            "less", "text/css"
        );
        
        mimeMappings.forEach((extension, mimeType) -> 
            context.setAttribute("mime." + extension, mimeType)
        );
    }

    private void configureErrorPages(ServletContext context) {
        List<Integer> errorCodes = Arrays.asList(401, 403, 404, 500, 503);
        String errorPage = Config.getStringProperty("error.page.path", "/html/error/custom-error-page.jsp");
        
        errorCodes.forEach(code -> 
            context.setAttribute("error." + code, errorPage)
        );
    }

    private void configureJspProperties(ServletContext context) {
        Map<String, Object> jspProperties = Map.of(
            "el-ignored", Config.getBooleanProperty("jsp.el.ignored", true),
            "scripting-invalid", Config.getBooleanProperty("jsp.scripting.invalid", false),
            "page-encoding", Config.getStringProperty("jsp.page.encoding", "UTF-8"),
            "trim-whitespace", Config.getBooleanProperty("jsp.trim.whitespace", true)
        );
        
        context.setAttribute("jsp.properties", jspProperties);
    }

    private void configureSecurityConstraints(ServletContext context) {
        // Map of path patterns to their property names
        Map<String, String> securePaths = Map.of(
            "/dotsecure/*", "security.constraints.dotsecure",
            "/assets/*", "security.constraints.assets"
        );
        
        securePaths.forEach((path, propertyName) -> {
            Map<String, Object> constraint = Map.of(
                "path", path,
                "authConstraint", Config.getBooleanProperty(propertyName, true)
            );
            context.setAttribute("security.constraint." + path, constraint);
        });
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Logger.info(this, "Web application configuration destroyed");
    }
} 