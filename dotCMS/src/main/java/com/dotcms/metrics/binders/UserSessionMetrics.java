package com.dotcms.metrics.binders;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive metric binder for dotCMS user session and authentication metrics.
 * 
 * This binder provides essential user activity metrics including:
 * - Current logged-in user count
 * - Session distribution by user type (admin, frontend, API)
 * - Login attempt tracking (success/failure rates)
 * - Session duration statistics
 * - API token usage patterns
 * 
 * These metrics are critical for monitoring user engagement,
 * detecting authentication issues, and capacity planning.
 */
public class UserSessionMetrics implements MeterBinder {
    
    private static final String METRIC_PREFIX = "dotcms.users";
    private final MBeanServer mBeanServer;
    
    // Thread-safe counters for login tracking
    private final AtomicLong totalLoginAttempts = new AtomicLong(0);
    private final AtomicLong successfulLogins = new AtomicLong(0);
    private final AtomicLong failedLogins = new AtomicLong(0);
    private final AtomicLong apiTokenUsage = new AtomicLong(0);
    
    public UserSessionMetrics() {
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }
    
    @Override
    public void bindTo(MeterRegistry registry) {
        try {
            registerSessionMetrics(registry);
            registerUserCountMetrics(registry);
            registerAuthenticationMetrics(registry);
            registerTomcatSessionMetrics(registry);
            
            Logger.info(this, "User session metrics registered successfully");
            
        } catch (Exception e) {
            Logger.error(this, "Failed to register user session metrics: " + e.getMessage(), e);
        }
    }
    
    /**
     * Register core session metrics.
     */
    private void registerSessionMetrics(MeterRegistry registry) {
        // Active user sessions
        Gauge.builder(METRIC_PREFIX + ".sessions.active", this, metrics -> getActiveSessionCount())
            .description("Number of active user sessions")
            .register(registry);
        
        // Admin sessions
        Gauge.builder(METRIC_PREFIX + ".sessions.admin", this, metrics -> getAdminSessionCount())
            .description("Number of active admin sessions")
            .register(registry);
        
        // Frontend user sessions
        Gauge.builder(METRIC_PREFIX + ".sessions.frontend", this, metrics -> getFrontendSessionCount())
            .description("Number of active frontend user sessions")
            .register(registry);
        
        // Anonymous sessions
        Gauge.builder(METRIC_PREFIX + ".sessions.anonymous", this, metrics -> getAnonymousSessionCount())
            .description("Number of anonymous sessions")
            .register(registry);
        
        // Average session duration
        Gauge.builder(METRIC_PREFIX + ".sessions.duration.avg_minutes", this, metrics -> getAverageSessionDuration())
            .description("Average session duration in minutes")
            .register(registry);
    }
    
    /**
     * Register user count metrics.
     */
    private void registerUserCountMetrics(MeterRegistry registry) {
        // Total active users
        Gauge.builder(METRIC_PREFIX + ".count.active", this, metrics -> getActiveUserCount())
            .description("Total number of active users")
            .register(registry);
        
        // Admin users
        Gauge.builder(METRIC_PREFIX + ".count.admins", this, metrics -> getAdminUserCount())
            .description("Number of admin users")
            .register(registry);
        
        // Frontend users
        Gauge.builder(METRIC_PREFIX + ".count.frontend", this, metrics -> getFrontendUserCount())
            .description("Number of frontend users")
            .register(registry);
        
        // Currently logged in users
        Gauge.builder(METRIC_PREFIX + ".count.logged_in", this, metrics -> getCurrentlyLoggedInCount())
            .description("Number of users currently logged in")
            .register(registry);
    }
    
    /**
     * Register authentication-related metrics.
     */
    private void registerAuthenticationMetrics(MeterRegistry registry) {
        // Total login attempts
        Gauge.builder(METRIC_PREFIX + ".auth.attempts.total", this, metrics -> totalLoginAttempts.get())
            .description("Total number of login attempts")
            .register(registry);
        
        // Successful logins
        Gauge.builder(METRIC_PREFIX + ".auth.attempts.success", this, metrics -> successfulLogins.get())
            .description("Number of successful login attempts")
            .register(registry);
        
        // Failed logins
        Gauge.builder(METRIC_PREFIX + ".auth.attempts.failed", this, metrics -> failedLogins.get())
            .description("Number of failed login attempts")
            .register(registry);
        
        // Login success rate
        Gauge.builder(METRIC_PREFIX + ".auth.success_rate", this, metrics -> getLoginSuccessRate())
            .description("Login success rate percentage")
            .register(registry);
        
        // API token usage
        Gauge.builder(METRIC_PREFIX + ".auth.api_tokens.usage", this, metrics -> apiTokenUsage.get())
            .description("Number of API token authentications")
            .register(registry);
        
        // Recent failed login attempts (security metric)
        Gauge.builder(METRIC_PREFIX + ".auth.recent_failures", this, metrics -> getRecentFailedLogins())
            .description("Number of failed login attempts in the last hour")
            .register(registry);
    }
    
    /**
     * Register Tomcat session manager metrics.
     */
    private void registerTomcatSessionMetrics(MeterRegistry registry) {
        try {
            Set<ObjectName> managers = mBeanServer.queryNames(
                new ObjectName("Catalina:type=Manager,host=*,context=*"), null);
            
            for (ObjectName manager : managers) {
                String host = manager.getKeyProperty("host");
                String context = manager.getKeyProperty("context");
                
                if (host == null) host = "default";
                if (context == null) context = "default";
                
                // Active sessions from Tomcat
                Gauge.builder(METRIC_PREFIX + ".tomcat.sessions.active", this,
                    metrics -> getTomcatSessionAttribute(manager, "activeSessions"))
                    .description("Number of active Tomcat sessions")
                    .tag("host", host)
                    .tag("context", context)
                    .register(registry);
                
                // Max active sessions
                Gauge.builder(METRIC_PREFIX + ".tomcat.sessions.max_active", this,
                    metrics -> getTomcatSessionAttribute(manager, "maxActive"))
                    .description("Maximum number of active sessions")
                    .tag("host", host)
                    .tag("context", context)
                    .register(registry);
                
                // Session creation rate
                Gauge.builder(METRIC_PREFIX + ".tomcat.sessions.created", this,
                    metrics -> getTomcatSessionAttribute(manager, "sessionCounter"))
                    .description("Total sessions created")
                    .tag("host", host)
                    .tag("context", context)
                    .register(registry);
                
                // Expired sessions
                Gauge.builder(METRIC_PREFIX + ".tomcat.sessions.expired", this,
                    metrics -> getTomcatSessionAttribute(manager, "expiredSessions"))
                    .description("Number of expired sessions")
                    .tag("host", host)
                    .tag("context", context)
                    .register(registry);
            }
            
        } catch (Exception e) {
            Logger.warn(this, "Failed to register Tomcat session metrics: " + e.getMessage());
        }
    }
    
    // ====================================================================
    // HELPER METHODS FOR SESSION COUNTING
    // ====================================================================
    
    /**
     * Get active session count from database.
     */
    private double getActiveSessionCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM user_session WHERE active = true AND last_access > NOW() - INTERVAL '1 hour'");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get active session count: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get admin session count.
     */
    private double getAdminSessionCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM user_session us JOIN user_ u ON us.user_id = u.userid " +
                 "WHERE us.active = true AND us.last_access > NOW() - INTERVAL '1 hour' AND u.admin = true");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get admin session count: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get frontend user session count.
     */
    private double getFrontendSessionCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM user_session us JOIN user_ u ON us.user_id = u.userid " +
                 "WHERE us.active = true AND us.last_access > NOW() - INTERVAL '1 hour' AND u.admin = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get frontend session count: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get anonymous session count (estimated from Tomcat - logged in sessions).
     */
    private double getAnonymousSessionCount() {
        double totalTomcatSessions = getTotalTomcatSessions();
        double loggedInSessions = getActiveSessionCount();
        return Math.max(0, totalTomcatSessions - loggedInSessions);
    }
    
    /**
     * Get average session duration in minutes.
     */
    private double getAverageSessionDuration() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT AVG(EXTRACT(EPOCH FROM (last_access - created_at))/60) " +
                 "FROM user_session WHERE active = true AND last_access > NOW() - INTERVAL '1 hour'");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get average session duration: " + e.getMessage());
            return 0.0;
        }
    }
    
    // ====================================================================
    // HELPER METHODS FOR USER COUNTING
    // ====================================================================
    
    /**
     * Get active user count.
     */
    private double getActiveUserCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM user_ WHERE active_ = true AND delete_in_progress = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get active user count: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get admin user count.
     */
    private double getAdminUserCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM user_ WHERE active_ = true AND admin = true AND delete_in_progress = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get admin user count: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get frontend user count.
     */
    private double getFrontendUserCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM user_ WHERE active_ = true AND admin = false AND delete_in_progress = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get frontend user count: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get currently logged in user count.
     */
    private double getCurrentlyLoggedInCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(DISTINCT user_id) FROM user_session " +
                 "WHERE active = true AND last_access > NOW() - INTERVAL '1 hour'");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get currently logged in count: " + e.getMessage());
            return 0.0;
        }
    }
    
    // ====================================================================
    // HELPER METHODS FOR AUTHENTICATION METRICS
    // ====================================================================
    
    /**
     * Get login success rate percentage.
     */
    private double getLoginSuccessRate() {
        long total = totalLoginAttempts.get();
        long success = successfulLogins.get();
        return total > 0 ? ((double) success / total) * 100 : 0.0;
    }
    
    /**
     * Get recent failed login attempts (last hour).
     */
    private double getRecentFailedLogins() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM login_audit " +
                 "WHERE login_result = 'FAILURE' AND login_date > NOW() - INTERVAL '1 hour'");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get recent failed logins: " + e.getMessage());
            return 0.0;
        }
    }
    
    // ====================================================================
    // HELPER METHODS FOR TOMCAT SESSION METRICS
    // ====================================================================
    
    private double getTomcatSessionAttribute(ObjectName objectName, String attributeName) {
        try {
            Object value = mBeanServer.getAttribute(objectName, attributeName);
            return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get Tomcat session attribute " + attributeName + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getTotalTomcatSessions() {
        try {
            Set<ObjectName> managers = mBeanServer.queryNames(
                new ObjectName("Catalina:type=Manager,host=*,context=*"), null);
            
            double total = 0;
            for (ObjectName manager : managers) {
                total += getTomcatSessionAttribute(manager, "activeSessions");
            }
            return total;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get total Tomcat sessions: " + e.getMessage());
            return 0.0;
        }
    }
    
    // ====================================================================
    // PUBLIC METHODS FOR LOGIN TRACKING (to be called by authentication filters)
    // ====================================================================
    
    /**
     * Record a login attempt (to be called by authentication system).
     */
    public void recordLoginAttempt(boolean successful) {
        totalLoginAttempts.incrementAndGet();
        if (successful) {
            successfulLogins.incrementAndGet();
        } else {
            failedLogins.incrementAndGet();
        }
    }
    
    /**
     * Record API token usage (to be called by API authentication).
     */
    public void recordApiTokenUsage() {
        apiTokenUsage.incrementAndGet();
    }
} 