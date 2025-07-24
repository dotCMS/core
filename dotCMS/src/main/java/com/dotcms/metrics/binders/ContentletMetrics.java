package com.dotcms.metrics.binders;

import com.dotcms.metrics.MetricsConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.util.Logger;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive metric binder for dotCMS content operations and content health.
 * 
 * This binder provides essential content metrics including:
 * - Content counts by status (published, draft, archived)
 * - Content operations performance
 * - Content type distribution
 * - Publishing queue status
 * 
 * Uses efficient database queries rather than loading content into memory.
 */
public class ContentletMetrics implements MeterBinder {
    
    private static final String METRIC_PREFIX = "dotcms.content";
    
    // Performance optimization: Cache expensive database queries
    private final ConcurrentHashMap<String, CachedValue> metricCache = new ConcurrentHashMap<>();
    
    private static class CachedValue {
        final double value;
        final long timestamp;
        
        CachedValue(double value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > (MetricsConfig.METRIC_CACHE_TTL_SECONDS * 1000L);
        }
    }
    
    @Override
    public void bindTo(MeterRegistry registry) {
        try {
            ContentletAPI contentletAPI = APILocator.getContentletAPI();
            
            // API availability
            Gauge.builder(METRIC_PREFIX + ".api.available", this, metrics -> contentletAPI != null ? 1.0 : 0.0)
                .description("Whether the contentlet API is available")
                .register(registry);
            
            // Content counts by status
            registerContentCountMetrics(registry);
            
            // Publishing queue metrics  
            registerPublishingMetrics(registry);
            
            // Content type metrics
            registerContentTypeMetrics(registry);
            
            Logger.info(this, "Enhanced contentlet metrics registered successfully");
            
        } catch (Exception e) {
            Logger.error(this, "Failed to register contentlet metrics: " + e.getMessage(), e);
        }
    }
    
    /**
     * Register metrics for content counts by various dimensions.
     */
    private void registerContentCountMetrics(MeterRegistry registry) {
        // Total published content
        Gauge.builder(METRIC_PREFIX + ".count.published", this, metrics -> getContentCountByStatus("published"))
            .description("Total number of published content items")
            .register(registry);
        
        // Total draft content  
        Gauge.builder(METRIC_PREFIX + ".count.draft", this, metrics -> getContentCountByStatus("draft"))
            .description("Total number of draft content items")
            .register(registry);
        
        // Total archived content
        Gauge.builder(METRIC_PREFIX + ".count.archived", this, metrics -> getContentCountByStatus("archived"))
            .description("Total number of archived content items")
            .register(registry);
        
        // Total content across all statuses
        Gauge.builder(METRIC_PREFIX + ".count.total", this, metrics -> getTotalContentCount())
            .description("Total number of content items (all statuses)")
            .register(registry);
    }
    
    /**
     * Register publishing-related metrics.
     */
    private void registerPublishingMetrics(MeterRegistry registry) {
        // Publishing queue size
        Gauge.builder(METRIC_PREFIX + ".publishing.queue_size", this, metrics -> getPublishingQueueSize())
            .description("Number of items in the publishing queue")
            .register(registry);
        
        // Failed publishing items
        Gauge.builder(METRIC_PREFIX + ".publishing.failed", this, metrics -> getFailedPublishingCount())
            .description("Number of failed publishing attempts")
            .register(registry);
    }
    
    /**
     * Register content type distribution metrics.
     */
    private void registerContentTypeMetrics(MeterRegistry registry) {
        // Number of active content types
        Gauge.builder(METRIC_PREFIX + ".types.active", this, metrics -> getActiveContentTypeCount())
            .description("Number of active content types")
            .register(registry);
        
        // Content type usage (top 10 by count)
        // Note: This could be expensive for high-cardinality metrics, implement carefully
        Gauge.builder(METRIC_PREFIX + ".types.total", this, metrics -> getTotalContentTypeCount())
            .description("Total number of content types")
            .register(registry);
    }
    
    /**
     * Get content count by status using efficient database query.
     */
    private double getContentCountByStatus(String status) {
        try (Connection conn = DbConnectionFactory.getConnection()) {
            String sql = "SELECT COUNT(*) FROM contentlet WHERE deleted = false";
            
            // Add status-specific conditions
            switch (status.toLowerCase()) {
                case "published":
                    sql += " AND live = true";
                    break;
                case "draft":
                    sql += " AND working = true AND live = false";
                    break;
                case "archived":
                    sql += " AND archived = true";
                    break;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (Exception e) {
            Logger.debug(this, "Failed to get content count for status " + status + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get total content count across all statuses.
     */
    private double getTotalContentCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM contentlet WHERE deleted = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get total content count: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get publishing queue size.
     */
    private double getPublishingQueueSize() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM publishing_queue WHERE publish_date <= NOW() AND completed = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get publishing queue size: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get failed publishing count.
     */
    private double getFailedPublishingCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM publishing_queue WHERE publish_date <= NOW() AND completed = false AND num_of_tries >= 3");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get failed publishing count: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get active content type count.
     */
    private double getActiveContentTypeCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM structure WHERE deleted = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get active content type count: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get total content type count including deleted.
     */
    private double getTotalContentTypeCount() {
        return getCachedMetric("content_type_count_total", () -> {
            try (Connection conn = DbConnectionFactory.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM structure")) {
                stmt.setQueryTimeout(MetricsConfig.METRIC_QUERY_TIMEOUT_SECONDS);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? (double) rs.getLong(1) : 0.0;
                }
            } catch (Exception e) {
                Logger.debug(this, "Failed to get total content type count: " + e.getMessage());
                return 0.0;
            }
        });
    }
    
    /**
     * Get cached metric value or calculate if expired.
     * This implements the caching performance optimization pattern.
     */
    private double getCachedMetric(String key, java.util.function.Supplier<Double> calculator) {
        CachedValue cached = metricCache.get(key);
        
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }
        
        // Calculate new value
        double newValue = calculator.get();
        metricCache.put(key, new CachedValue(newValue));
        return newValue;
    }
}