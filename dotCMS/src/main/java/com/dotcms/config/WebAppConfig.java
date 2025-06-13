package com.dotcms.config;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles programmatic registration of web.xml configuration elements
 * This class consolidates all web application configuration including:
 * - Context parameters
 * - JSP properties
 * - Error pages
 * - MIME mappings
 * - Session and encoding settings
 */
@WebListener
public class WebAppConfig implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        
        try {
            // Configure context parameters
            configureContextParams(context);
            
            // Configure JSP properties
            configureJspProperties(context);
            
            // Configure error pages
            configureErrorPages(context);
            
            // Configure MIME mappings
            configureMimeMappings(context);
            
            // Configure session and encoding
            configureSessionAndEncoding(context);
            
            Logger.info(this, "Web application configuration initialized successfully");
        } catch (Exception e) {
            Logger.error(this, "Failed to initialize web application configuration", e);
            throw new RuntimeException("Failed to initialize web application configuration", e);
        }
    }

    private void configureContextParams(ServletContext context) {
        // Essential context parameters
        context.setInitParameter("company_id", Config.getStringProperty("company_id", "dotcms.org"));
        context.setInitParameter("isLog4jAutoInitializationDisabled", 
            Config.getStringProperty("isLog4jAutoInitializationDisabled", "true"));
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

    private void configureErrorPages(ServletContext context) {
        String errorPagePath = Config.getStringProperty("error.page.path", "/html/error/custom-error-page.jsp");
        Map<String, String> errorPages = Map.of(
            "404", errorPagePath,
            "401", errorPagePath,
            "403", errorPagePath,
            "500", errorPagePath,
            "503", errorPagePath
        );
        
        context.setAttribute("error.pages", errorPages);
    }

    private void configureMimeMappings(ServletContext context) {
        Map<String, String> mimeMappings = new HashMap<>();
        mimeMappings.put("vtl", Config.getStringProperty("mime.type.vtl", "text/velocity"));
        mimeMappings.put("xsl", Config.getStringProperty("mime.type.xsl", "application/xml"));
        mimeMappings.put("dotsass", Config.getStringProperty("mime.type.dotsass", "text/css"));
        mimeMappings.put("sass", Config.getStringProperty("mime.type.sass", "text/css"));
        mimeMappings.put("scss", Config.getStringProperty("mime.type.scss", "text/css"));
        mimeMappings.put("less", Config.getStringProperty("mime.type.less", "text/css"));
        
        context.setAttribute("mime.mappings", mimeMappings);
    }

    private void configureSessionAndEncoding(ServletContext context) {
        // Session timeout in minutes
        context.setAttribute("session.timeout", 
            Config.getIntProperty("session.timeout", 30));
        
        // Character encoding
        context.setAttribute("request.character.encoding", 
            Config.getStringProperty("request.character.encoding", "UTF-8"));
        context.setAttribute("response.character.encoding", 
            Config.getStringProperty("response.character.encoding", "UTF-8"));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Logger.info(this, "Web application configuration destroyed");
    }
} 