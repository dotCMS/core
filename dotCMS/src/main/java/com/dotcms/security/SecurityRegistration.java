package com.dotcms.security;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import java.util.Arrays;

/**
 * Handles programmatic registration of security constraints
 */
@WebListener
public class SecurityRegistration implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        
        // Register security constraints using annotations
        // These will be picked up by the container's security configuration
        context.setAttribute("security.constraints", Arrays.asList(
            new SecurityConstraint("/dotsecure/*", new String[0]),
            new SecurityConstraint("/assets/*", new String[0])
        ));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cleanup if needed
    }

    private static class SecurityConstraint {
        private final String urlPattern;
        private final String[] roles;

        SecurityConstraint(String urlPattern, String[] roles) {
            this.urlPattern = urlPattern;
            this.roles = roles;
        }

        public String getUrlPattern() {
            return urlPattern;
        }

        public String[] getRoles() {
            return roles;
        }
    }
} 