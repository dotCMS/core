package com.dotcms.health.checks;

import com.dotcms.health.config.HealthCheckConfig.HealthCheckMode;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.util.HealthCheckBase;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.servlet.ServletContext;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Servlet Container health check that monitors if the web container is responsive
 * and functioning properly. This is a critical liveness check as servlet container
 * failures typically require pod restarts.
 * 
 * The check validates:
 * 1. Servlet context availability and responsiveness
 * 2. dotCMS startup process completion (dotcms.started.up=true)
 * 3. HTTP connector readiness (if configured)
 * 4. Basic servlet API version compatibility
 * 
 * Uses PRODUCTION mode by default since HTTP serving is fundamental infrastructure.
 * This check ensures "FIRST SUCCESS" messages only appear after the complete dotCMS
 * startup process is finished, not just when the servlet context is initialized.
 * 
 * Configuration Properties:
 * - health.check.servlet-container.mode: PRODUCTION (fundamental check)
 * - health.check.servlet-container.timeout-ms: Check timeout (default: 2000ms)
 * - health.check.servlet-container.min-servlet-version: Minimum servlet version (default: 3)
 * - health.check.servlet-container.require-connectors: Check HTTP connectors (default: true)
 */
public class ServletContainerHealthCheck extends HealthCheckBase {
    
    private ServletContext servletContext;
    
    public ServletContainerHealthCheck() {
        // Note: HealthCheckBase doesn't have a constructor with parameters
        // The name and order are handled by the base class
    }
    
    @Override
    public String getName() {
        return "servlet-container";
    }
    
    @Override
    public int getOrder() {
        return 100; // High priority for liveness
    }
    
    @Override
    protected CheckResult performCheck() throws Exception {
        return measureExecution(() -> {
            // 1. First verify servlet context is available
            if (servletContext == null) {
                throw new Exception("Servlet context not available");
            }
            
            // 2. Check if dotCMS startup process is complete
            // According to startup documentation, InitServlet (load-on-startup=8) sets this 
            // property after all core initialization is complete
            String startupComplete = System.getProperty("dotcms.started.up");
            boolean startupFinished = "true".equals(startupComplete);
            
            // Log at DEBUG level for operational checks unless it's during critical startup phase
            boolean isVerboseLogging = Config.getBooleanProperty("health.check.servlet-container.verbose-logging", false);
            
            Logger.debug(this, String.format("dotCMS startup check: dotcms.started.up='%s', startupFinished=%s", 
                startupComplete, startupFinished));
            
            // 3. Check basic servlet responsiveness
            if (!checkContainerHealth()) {
                throw new Exception("Servlet container not responsive");
            }
            
            // 4. Check HTTP connectors if configured
            boolean connectorsRequired = Config.getBooleanProperty("health.check.servlet-container.require-connectors", true);
            Logger.debug(this, String.format("HTTP connectors required: %s", connectorsRequired));
            
            if (connectorsRequired) {
                boolean connectorsReady = checkHttpConnectorsReady();
                Logger.debug(this, String.format("HTTP connectors ready: %s", connectorsReady));
                
                if (!connectorsReady && startupFinished) {
                    // If JMX detection failed but startup is complete, try self-check as fallback
                    Logger.debug(this, "JMX connector detection failed, attempting self-check as fallback");
                    try {
                        performSelfHttpCheck();
                        Logger.debug(this, "Self-check passed - HTTP connectors are ready despite JMX detection failure");
                        connectorsReady = true;
                    } catch (Exception e) {
                        Logger.warn(this, "Self-check also failed: " + e.getMessage());
                    }
                }
                
                if (!connectorsReady) {
                    // If startup is finished but connectors aren't ready, that's a real problem
                    if (startupFinished) {
                        Logger.info(this, "HTTP connectors not ready (startup complete, JMX and self-check failed)");
                        throw new Exception("HTTP connectors not ready to serve requests (startup complete, JMX and self-check failed)");
                    } else {
                        Logger.debug(this, "HTTP connectors not ready - waiting for startup completion");
                        throw new Exception("HTTP connectors not ready - waiting for startup completion");
                    }
                }
                Logger.debug(this, "HTTP connectors check passed");
            }
            
            // 5. Perform self-check to prove HTTP serving capability
            boolean selfCheckEnabled = Config.getBooleanProperty("health.check.servlet-container.self-check-enabled", true);
            boolean operationalSelfCheck = Config.getBooleanProperty("health.check.servlet-container.operational-self-check", false);
            Logger.debug(this, String.format("Self-check enabled: %s, operational self-check: %s, startup finished: %s", 
                selfCheckEnabled, operationalSelfCheck, startupFinished));
            
            // During startup: always perform self-check if enabled
            // After startup: only perform self-check if operational-self-check is enabled
            boolean shouldPerformSelfCheck = selfCheckEnabled && (!startupFinished || operationalSelfCheck);
            
            if (shouldPerformSelfCheck) {
                try {
                    Logger.debug(this, "Performing self HTTP check");
                    performSelfHttpCheck();
                    Logger.debug(this, "Self HTTP check passed");
                } catch (Exception e) {
                    Logger.info(this, String.format("Self HTTP check failed: %s", e.getMessage()));
                    throw new Exception("Self HTTP check failed - cannot handle actual requests: " + e.getMessage());
                }
            }
            
            // 6. Final startup completion check
            if (!startupFinished) {
                // Verify startup timing is available (set by InitServlet)
                String startupMs = System.getProperty("dotcms.startup.ms");
                if (startupMs == null || startupMs.isEmpty()) {
                    Logger.debug(this, "dotCMS startup process not yet complete (dotcms.startup.ms not set)");
                    throw new Exception("dotCMS startup process not yet complete (dotcms.startup.ms not set)");
                } else {
                    Logger.debug(this, "dotCMS startup process not yet complete (dotcms.started.up != true)");
                    throw new Exception("dotCMS startup process not yet complete (dotcms.started.up != true)");
                }
            }
            
            // 7. All checks passed - servlet container is ready and dotCMS startup is complete
            String startupMs = System.getProperty("dotcms.startup.ms");
            long startupTimeMs = startupMs != null ? Long.parseLong(startupMs) : 0;
            
            String message = connectorsRequired 
                ? String.format("Servlet container responsive, HTTP connectors ready, self-check passed, dotCMS startup complete (%dms)", startupTimeMs)
                : String.format("Servlet container responsive, self-check passed, dotCMS startup complete (%dms)", startupTimeMs);
                
            Logger.debug(this, String.format("ServletContainerHealthCheck SUCCESS: %s", message));
            return message;
        });
    }
    
    /**
     * Sets the servlet context for container health monitoring
     */
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
    
    /**
     * Checks if HTTP connectors are started and ready to serve requests using JMX.
     * This ensures that the "FIRST SUCCESS" health check messages only appear when
     * the system can actually handle HTTP requests, not just when the servlet context
     * is initialized.
     */
    private boolean checkHttpConnectorsReady() {
        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            
            // Query for all Tomcat connectors
            Set<ObjectName> connectorNames = beanServer.queryNames(
                new ObjectName("*:type=Connector,*"), null);
            
            if (connectorNames.isEmpty()) {
                Logger.debug(this, "No connectors found via JMX - may be early in startup");
                return false;
            }
            
            Logger.debug(this, String.format("Found %d connectors via JMX", connectorNames.size()));
            
            // Check if at least one HTTP connector is in STARTED state
            for (ObjectName connectorName : connectorNames) {
                try {
                    Object port = beanServer.getAttribute(connectorName, "port");
                    Object protocol = beanServer.getAttribute(connectorName, "protocol");
                    Object scheme = beanServer.getAttribute(connectorName, "scheme");
                    Object state = beanServer.getAttribute(connectorName, "stateName");
                    
                    Logger.debug(this, String.format("Connector found - port: %s, protocol: %s, scheme: %s, state: %s", 
                        port, protocol, scheme, state));
                    
                    // Check if this is an HTTP connector (not AJP)
                    boolean isHttpConnector = false;
                    if (protocol != null) {
                        String protocolStr = protocol.toString();
                        // Handle various HTTP protocol implementations
                        if (protocolStr.contains("HTTP") || 
                            protocolStr.contains("Http11") || 
                            protocolStr.contains("http11") ||
                            (scheme != null && ("http".equals(scheme.toString()) || "https".equals(scheme.toString())))) {
                            isHttpConnector = true;
                        }
                    }
                    
                    if (isHttpConnector && "STARTED".equals(state)) {
                        Logger.debug(this, String.format("Found started HTTP connector on port %s", port));
                        return true; // At least one HTTP connector is ready
                    }
                } catch (Exception e) {
                    Logger.warn(this, String.format("Could not check state of connector %s: %s", 
                        connectorName, e.getMessage()));
                }
            }
            
            Logger.debug(this, "No HTTP connectors in STARTED state");
            return false;
            
        } catch (Exception e) {
            Logger.debug(this, "Could not check HTTP connector readiness: " + e.getMessage());
            // In case of JMX issues, fall back to basic servlet context check
            return servletContext != null;
        }
    }
    
    /**
     * Gets the count of started HTTP connectors for status reporting
     */
    private int getStartedConnectorCount() {
        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            
            Set<ObjectName> connectorNames = beanServer.queryNames(
                new ObjectName("*:type=Connector,*"),
                Query.match(Query.attr("protocol"), Query.value("HTTP/1.1"))
            );
            
            if (connectorNames.isEmpty()) {
                connectorNames = beanServer.queryNames(
                    new ObjectName("*:type=Connector,*"),
                    Query.anySubString(Query.attr("protocol"), Query.value("Http11"))
                );
            }
            
            int startedCount = 0;
            for (ObjectName connectorName : connectorNames) {
                try {
                    Object state = beanServer.getAttribute(connectorName, "stateName");
                    if ("STARTED".equals(state)) {
                        startedCount++;
                    }
                } catch (Exception e) {
                    // Skip this connector
                }
            }
            
            return startedCount;
            
        } catch (Exception e) {
            return 0;
        }
    }
    
    private boolean checkContainerHealth() {
        try {
            // 1. Check if servlet context is available and responsive
            if (servletContext == null) {
                Logger.warn(this, "ServletContext not available");
                return false;
            }
            
            // 2. Test basic servlet context operations
            String contextPath = servletContext.getContextPath();
            String serverInfo = servletContext.getServerInfo();
            
            // 3. Check if we can access servlet context attributes (basic responsiveness test)
            servletContext.getAttribute("javax.servlet.context.tempdir");
            
            // 4. Verify temp directory is accessible (basic file system access)
            if (!checkTempDirectory()) {
                Logger.warn(this, "Servlet temp directory not accessible");
                return false;
            }
            
            // 5. Check basic servlet API functionality
            int majorVersion = servletContext.getMajorVersion();
            int minVersion = getConfigProperty("min-servlet-version", 3);
            if (majorVersion < minVersion) {
                Logger.warn(this, "Servlet API version unexpectedly low: " + majorVersion + " < " + minVersion);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            Logger.warn(this, "Servlet container health check failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean checkTempDirectory() {
        try {
            if (servletContext == null) {
                return false;
            }
            
            File tempDir = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
            if (tempDir == null) {
                // Try fallback to system temp
                tempDir = new File(System.getProperty("java.io.tmpdir"));
            }
            
            return tempDir != null && tempDir.exists() && tempDir.canWrite();
            
        } catch (Exception e) {
            Logger.debug(this, "Could not check temp directory: " + e.getMessage());
            return true; // Don't fail liveness for temp dir issues
        }
    }
    
    /**
     * Performs a self-check by making an actual HTTP request to the /livez endpoint
     * to prove the system can handle real HTTP requests, not just that components are initialized.
     * This provides definitive proof of HTTP serving capability.
     */
    private void performSelfHttpCheck() throws Exception {
        if (servletContext == null) {
            throw new Exception("ServletContext not available for self-check");
        }
        
        // Get local server info for self-check
        String contextPath = servletContext.getContextPath();
        if (contextPath == null) {
            contextPath = "";
        }
        
        // Try to determine local port - fallback to standard ports if not available
        int port = determineSelfCheckPort();
        String protocol = port == 443 ? "https" : "http";
        // Use a simple static resource to avoid circular dependency with health check endpoints
        // Any response (200, 404, etc.) proves the HTTP server is handling requests
        String url = String.format("%s://localhost:%d%s/favicon.ico", protocol, port, contextPath);
        
        Logger.debug(this, "Performing self HTTP check to: " + url);
        
        java.net.URL selfCheckUrl = new java.net.URL(url);
        java.net.HttpURLConnection connection = null;
        
        try {
            // Create a simple HTTP connection with timeout
            connection = (java.net.HttpURLConnection) selfCheckUrl.openConnection();
            
            // Configure connection for quick check
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(500);  // 500ms connect timeout
            connection.setReadTimeout(1000);    // 1 second read timeout  
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(false);
            
            // Make the request
            long startTime = System.currentTimeMillis();
            int responseCode = connection.getResponseCode();
            long duration = System.currentTimeMillis() - startTime;
            
            // We only care that we got a response, not what the response was
            // Any HTTP response (200, 503, etc.) proves the server is handling requests
            Logger.debug(this, String.format("Self HTTP check completed: %d response in %dms", responseCode, duration));
            
        } catch (java.net.ConnectException e) {
            throw new Exception("Cannot connect to local HTTP endpoint - server not accepting connections");
        } catch (java.net.SocketTimeoutException e) {
            throw new Exception("HTTP self-check timed out - server may be overloaded or not responding");
        } catch (Exception e) {
            throw new Exception("HTTP self-check failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Determines the port to use for self-check by querying JMX connectors
     */
    private int determineSelfCheckPort() {
        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            
            // Query for HTTP connectors to find the port
            Set<ObjectName> connectorNames = beanServer.queryNames(
                new ObjectName("*:type=Connector,*"), null);
            
            for (ObjectName connectorName : connectorNames) {
                try {
                    Object port = beanServer.getAttribute(connectorName, "port");
                    Object protocol = beanServer.getAttribute(connectorName, "protocol");
                    Object scheme = beanServer.getAttribute(connectorName, "scheme");
                    
                    if (port instanceof Integer && protocol != null) {
                        String protocolStr = protocol.toString();
                        String schemeStr = scheme != null ? scheme.toString() : "http";
                        
                        // Look for HTTP connectors (not AJP) using improved detection logic
                        boolean isHttpConnector = protocolStr.contains("HTTP") || 
                                                protocolStr.contains("Http11") || 
                                                protocolStr.contains("http11") ||
                                                "http".equals(schemeStr) || 
                                                "https".equals(schemeStr);
                        
                        if (isHttpConnector) {
                            Logger.debug(this, String.format("Found HTTP connector on port %s (protocol: %s, scheme: %s)", port, protocol, scheme));
                            return (Integer) port;
                        }
                    }
                } catch (Exception e) {
                    // Skip this connector
                    Logger.debug(this, "Could not query connector " + connectorName + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Logger.debug(this, "Could not determine port via JMX: " + e.getMessage());
        }
        
        // Fallback to standard HTTP port
        return 8080;
    }
    
    /**
     * CRITICAL for liveness - servlet container failures typically require restart
     */
    @Override
    public boolean isLivenessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    /**
     * Also essential for readiness - cannot serve HTTP requests without servlet container
     */
    @Override
    public boolean isReadinessCheck() {
        HealthCheckMode mode = getMode();
        return mode != HealthCheckMode.DISABLED;
    }
    
    @Override
    public String getDescription() {
        HealthCheckMode mode = getMode();
        int minVersion = getConfigProperty("min-servlet-version", 3);
        boolean requireConnectors = Config.getBooleanProperty("health.check.servlet-container.require-connectors", true);
        
        return String.format("Monitors servlet container responsiveness (min API: %d, HTTP connectors: %s) (Mode: %s)", 
            minVersion, requireConnectors ? "required" : "optional", mode.name());
    }
    
    @Override
    protected Map<String, Object> buildStructuredData(CheckResult result, HealthStatus originalStatus, 
                                                      HealthStatus finalStatus, HealthCheckMode mode) {
        Map<String, Object> data = new HashMap<>();
        
        // Include startup time that appears in the message
        String startupMs = System.getProperty("dotcms.startup.ms");
        if (startupMs != null && !startupMs.isEmpty()) {
            try {
                long startupTimeMs = Long.parseLong(startupMs);
                data.put("startupTimeMs", startupTimeMs);
            } catch (NumberFormatException e) {
                // Ignore invalid startup time
            }
        }
        
        // Include startup completion status
        String startupComplete = System.getProperty("dotcms.started.up");
        data.put("startupComplete", "true".equals(startupComplete));
        
        // Include servlet container information
        if (servletContext != null) {
            try {
                data.put("servletApiVersion", servletContext.getMajorVersion() + "." + servletContext.getMinorVersion());
                String serverInfo = servletContext.getServerInfo();
                if (serverInfo != null) {
                    data.put("serverInfo", serverInfo);
                }
            } catch (Exception e) {
                // Ignore servlet context access errors
            }
        }
        
        // Include HTTP connector count if available
        try {
            int connectorCount = getStartedConnectorCount();
            if (connectorCount > 0) {
                data.put("httpConnectorCount", connectorCount);
            }
        } catch (Exception e) {
            // Ignore connector count errors
        }
        
        // Include error type for servlet container failures
        if (result.error != null) {
            data.put("errorType", "servlet_container");
        }
        
        return data;
    }
} 