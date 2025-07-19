package com.dotcms.metrics.binders;

import com.dotmarketing.util.Logger;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Set;

/**
 * Metric binder for Tomcat server metrics.
 * 
 * This binder provides metrics about:
 * - Thread pool usage
 * - Connection pool status
 * - Request processing
 * - Memory usage
 * - Session information
 */
public class TomcatMetrics implements MeterBinder {
    
    private static final String METRIC_PREFIX = "dotcms.tomcat";
    private final MBeanServer mBeanServer;
    
    public TomcatMetrics() {
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }
    
    @Override
    public void bindTo(MeterRegistry registry) {
        try {
            registerThreadPoolMetrics(registry);
            registerConnectorMetrics(registry);
            registerRequestProcessorMetrics(registry);
            registerSessionMetrics(registry);
            
            Logger.info(this, "Tomcat metrics registered successfully");
            
        } catch (Exception e) {
            Logger.error(this, "Failed to register Tomcat metrics: " + e.getMessage(), e);
        }
    }
    
    private void registerThreadPoolMetrics(MeterRegistry registry) {
        try {
            Set<ObjectName> threadPools = mBeanServer.queryNames(
                new ObjectName("Catalina:type=ThreadPool,name=*"), null);
            
            for (ObjectName threadPool : threadPools) {
                String poolName = threadPool.getKeyProperty("name");
                
                // Current thread count
                Gauge.builder(METRIC_PREFIX + ".threads.current", this, 
                    metrics -> getThreadPoolAttribute(threadPool, "currentThreadCount"))
                    .description("Current number of threads in the pool")
                    .tag("pool", poolName)
                    .register(registry);
                
                // Active thread count
                Gauge.builder(METRIC_PREFIX + ".threads.active", this,
                    metrics -> getThreadPoolAttribute(threadPool, "currentThreadsBusy"))
                    .description("Number of active threads")
                    .tag("pool", poolName)
                    .register(registry);
                
                // Max thread count
                Gauge.builder(METRIC_PREFIX + ".threads.max", this,
                    metrics -> getThreadPoolAttribute(threadPool, "maxThreads"))
                    .description("Maximum number of threads allowed")
                    .tag("pool", poolName)
                    .register(registry);
            }
            
        } catch (Exception e) {
            Logger.warn(this, "Failed to register thread pool metrics: " + e.getMessage());
        }
    }
    
    private void registerConnectorMetrics(MeterRegistry registry) {
        try {
            Set<ObjectName> connectors = mBeanServer.queryNames(
                new ObjectName("Catalina:type=Connector,port=*"), null);
            
            for (ObjectName connector : connectors) {
                String port = connector.getKeyProperty("port");
                
                // Current connections
                Gauge.builder(METRIC_PREFIX + ".connections.current", this,
                    metrics -> getConnectorAttribute(connector, "connectionCount"))
                    .description("Current number of connections")
                    .tag("port", port)
                    .register(registry);
                
                // Max connections
                Gauge.builder(METRIC_PREFIX + ".connections.max", this,
                    metrics -> getConnectorAttribute(connector, "maxConnections"))
                    .description("Maximum number of connections allowed")
                    .tag("port", port)
                    .register(registry);
            }
            
        } catch (Exception e) {
            Logger.warn(this, "Failed to register connector metrics: " + e.getMessage());
        }
    }
    
    private void registerRequestProcessorMetrics(MeterRegistry registry) {
        try {
            Set<ObjectName> processors = mBeanServer.queryNames(
                new ObjectName("Catalina:type=GlobalRequestProcessor,name=*"), null);
            
            for (ObjectName processor : processors) {
                String processorName = processor.getKeyProperty("name");
                
                // Request count
                Gauge.builder(METRIC_PREFIX + ".requests.total", this,
                    metrics -> getRequestProcessorAttribute(processor, "requestCount"))
                    .description("Total number of requests processed")
                    .tag("processor", processorName)
                    .register(registry);
                
                // Error count
                Gauge.builder(METRIC_PREFIX + ".requests.errors", this,
                    metrics -> getRequestProcessorAttribute(processor, "errorCount"))
                    .description("Total number of request errors")
                    .tag("processor", processorName)
                    .register(registry);
                
                // Processing time
                Gauge.builder(METRIC_PREFIX + ".requests.processing_time", this,
                    metrics -> getRequestProcessorAttribute(processor, "processingTime"))
                    .description("Total request processing time (ms)")
                    .tag("processor", processorName)
                    .register(registry);
                
                // Bytes received
                Gauge.builder(METRIC_PREFIX + ".requests.bytes_received", this,
                    metrics -> getRequestProcessorAttribute(processor, "bytesReceived"))
                    .description("Total bytes received")
                    .tag("processor", processorName)
                    .register(registry);
                
                // Bytes sent
                Gauge.builder(METRIC_PREFIX + ".requests.bytes_sent", this,
                    metrics -> getRequestProcessorAttribute(processor, "bytesSent"))
                    .description("Total bytes sent")
                    .tag("processor", processorName)
                    .register(registry);
            }
            
        } catch (Exception e) {
            Logger.warn(this, "Failed to register request processor metrics: " + e.getMessage());
        }
    }
    
    private void registerSessionMetrics(MeterRegistry registry) {
        try {
            Set<ObjectName> managers = mBeanServer.queryNames(
                new ObjectName("Catalina:type=Manager,host=*,context=*"), null);
            
            for (ObjectName manager : managers) {
                String host = manager.getKeyProperty("host");
                String context = manager.getKeyProperty("context");
                
                // Active sessions
                Gauge.builder(METRIC_PREFIX + ".sessions.active", this,
                    metrics -> getSessionManagerAttribute(manager, "activeSessions"))
                    .description("Number of active sessions")
                    .tag("host", host)
                    .tag("context", context)
                    .register(registry);
                
                // Max active sessions
                Gauge.builder(METRIC_PREFIX + ".sessions.max_active", this,
                    metrics -> getSessionManagerAttribute(manager, "maxActive"))
                    .description("Maximum number of active sessions")
                    .tag("host", host)
                    .tag("context", context)
                    .register(registry);
                
                // Session creation rate
                Gauge.builder(METRIC_PREFIX + ".sessions.created", this,
                    metrics -> getSessionManagerAttribute(manager, "sessionCounter"))
                    .description("Total sessions created")
                    .tag("host", host)
                    .tag("context", context)
                    .register(registry);
            }
            
        } catch (Exception e) {
            Logger.warn(this, "Failed to register session metrics: " + e.getMessage());
        }
    }
    
    private double getThreadPoolAttribute(ObjectName objectName, String attributeName) {
        try {
            Object value = mBeanServer.getAttribute(objectName, attributeName);
            return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get thread pool attribute " + attributeName + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getConnectorAttribute(ObjectName objectName, String attributeName) {
        try {
            Object value = mBeanServer.getAttribute(objectName, attributeName);
            return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get connector attribute " + attributeName + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getRequestProcessorAttribute(ObjectName objectName, String attributeName) {
        try {
            Object value = mBeanServer.getAttribute(objectName, attributeName);
            return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get request processor attribute " + attributeName + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getSessionManagerAttribute(ObjectName objectName, String attributeName) {
        try {
            Object value = mBeanServer.getAttribute(objectName, attributeName);
            return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get session manager attribute " + attributeName + ": " + e.getMessage());
            return 0.0;
        }
    }
}