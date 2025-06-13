package com.dotcms.filters;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import com.dotcms.filters.NormalizationFilter;
import com.dotmarketing.filters.InterceptorFilter;
import com.dotmarketing.filters.CookiesFilter;
import com.dotmarketing.filters.CharsetEncodingFilter;
import com.dotmarketing.filters.ThreadNameFilter;
import com.dotcms.health.filter.HealthCheckFilter;
import com.dotmarketing.filters.TimeMachineFilter;
import com.dotmarketing.filters.DotUrlRewriteFilter;
import com.dotmarketing.filters.AutoLoginFilter;
import com.dotmarketing.filters.LoginRequiredFilter;
import com.dotmarketing.filters.CMSFilter;
import com.dotcms.vanityurl.filters.VanityURLFilter;
import com.dotcms.visitor.filter.servlet.VisitorFilter;
import com.dotcms.repackage.com.liferay.filters.secure.SecureFilter;
import org.apache.catalina.filters.HttpHeaderSecurityFilter;
import java.util.LinkedHashMap;
import java.util.Map;
import com.dotmarketing.util.Logger;

/**
 * Handles programmatic registration and ordering of filters to maintain exact filter chain sequence.
 * 
 * CRITICAL ORDERING: This class registers filters in a specific order that must be maintained
 * for security, performance, and functionality. The ordered filters run before any @WebFilter
 * annotated filters.
 * 
 * Filter Chain Execution Order:
 * 1. Programmatically registered filters (this class) - ORDERED
 * 2. @WebFilter annotated filters - UNORDERED (container managed)
 * 
 * ADDING NEW FILTERS:
 * - Filters needing specific ordering: Add to orderedFilters Map in this class
 * - Filters that can run after ordered chain: Use @WebFilter annotation on the filter class
 * 
 * DO NOT add @WebFilter to filters registered here - it creates duplicate registrations!
 */
@WebListener
public class FilterRegistration implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        
        try {
            // Define filter order using a LinkedHashMap to maintain insertion order
            // Order matches previous web.xml filter-mappings for consistency
            Map<String, FilterConfig> orderedFilters = new LinkedHashMap<>();
            
            // === PHASE 1: Essential security and HTTP compliance filters run first ===
            orderedFilters.put("NormalizationFilter", new FilterConfig(NormalizationFilter.class, "/*"));
            orderedFilters.put("HttpHeaderSecurityFilter", new FilterConfig(HttpHeaderSecurityFilter.class, "/*"));
            orderedFilters.put("CookiesFilter", new FilterConfig(CookiesFilter.class, "/*"));
            
            // === PHASE 2: Health check bypass (critical positioning) ===
            // CRITICAL: HealthCheckFilter runs after essential security filters but before expensive ones
            // This allows health checks to bypass slow/failing downstream filters
            orderedFilters.put("HealthCheckFilter", new FilterConfig(HealthCheckFilter.class, "/*"));
            
            // === PHASE 3: Core infrastructure filters ===
            orderedFilters.put("CharsetEncodingFilter", new FilterConfig(CharsetEncodingFilter.class, "/*"));
            orderedFilters.put("ThreadNameFilter", new FilterConfig(ThreadNameFilter.class, "/*"));
            orderedFilters.put("InterceptorFilter", new FilterConfig(InterceptorFilter.class, "/*"));
            
            // === PHASE 4: Content processing filters ===
            orderedFilters.put("TimeMachineFilter", new FilterConfig(TimeMachineFilter.class, "/*"));
            orderedFilters.put("UrlRewriteFilter", new FilterConfig(DotUrlRewriteFilter.class, "/*"));
            orderedFilters.put("VanityURLFilter", new FilterConfig(VanityURLFilter.class, "/*"));
            
            // === PHASE 5: User/session management filters ===
            orderedFilters.put("VisitorFilter", new FilterConfig(VisitorFilter.class, "/*"));
            orderedFilters.put("AutoLoginFilter", new FilterConfig(AutoLoginFilter.class, "/*"));
            orderedFilters.put("LoginRequiredFilter", new FilterConfig(LoginRequiredFilter.class, "/*"));
            
            // === PHASE 6: Application logic filters ===
            orderedFilters.put("CMSFilter", new FilterConfig(CMSFilter.class, "/*"));
            orderedFilters.put("Secure MainServlet Filter", new FilterConfig(SecureFilter.class, "/c/*"));
            
            // === NOTE: @WebFilter annotated filters will run AFTER all the above filters ===
            
            // Register filters in order
            for (Map.Entry<String, FilterConfig> entry : orderedFilters.entrySet()) {
                try {
                    registerFilter(context, entry.getKey(), entry.getValue());
                } catch (IllegalStateException e) {
                    // Filter is already registered via annotation
                    Logger.debug(this, "Filter " + entry.getKey() + " is already registered via annotation");
                }
            }
            
            Logger.info(this, "Filter registration completed successfully. " + 
                       orderedFilters.size() + " filters registered in order. " +
                       "@WebFilter annotated filters will run after these ordered filters.");
        } catch (Exception e) {
            Logger.error(this, "Failed to initialize filters", e);
            throw new RuntimeException("Failed to initialize filters", e);
        }
    }

    private void registerFilter(ServletContext context, String name, FilterConfig config) {
        javax.servlet.FilterRegistration.Dynamic registration = context.addFilter(name, config.filterClass);
        if (registration != null) {
            registration.addMappingForUrlPatterns(null, false, config.urlPattern);
            
            // Add special init parameters for specific filters
            if ("HttpHeaderSecurityFilter".equals(name)) {
                registration.setInitParameter("hstsMaxAgeSeconds", "3600");
                registration.setInitParameter("hstsIncludeSubDomains", "true");
                registration.setInitParameter("antiClickJackingOption", "SAMEORIGIN");
            } else if ("Secure MainServlet Filter".equals(name)) {
                registration.setInitParameter("portal_property_prefix", "main.servlet.");
            }
            
            // Set async support for all filters except HttpHeaderSecurityFilter and SecureFilter
            if (!"HttpHeaderSecurityFilter".equals(name) && !"Secure MainServlet Filter".equals(name)) {
                registration.setAsyncSupported(true);
            }
            
            Logger.debug(this, "Registered filter: " + name + " with pattern: " + config.urlPattern);
        } else {
            Logger.debug(this, "Filter " + name + " is already registered");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cleanup if needed
    }
    
    private static class FilterConfig {
        final Class<? extends javax.servlet.Filter> filterClass;
        final String urlPattern;
        
        FilterConfig(Class<? extends javax.servlet.Filter> filterClass, String urlPattern) {
            this.filterClass = filterClass;
            this.urlPattern = urlPattern;
        }
    }
} 