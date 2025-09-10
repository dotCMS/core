package com.dotcms.shutdown.tasks;

import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ShutdownOrder(80)
public class ThreadPoolCleanupShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "Thread pool cleanup";
    }

    @Override
    public void run() {
        try {
            Logger.debug(this, "Attempting to shutdown remaining thread pools");

            // Try to shutdown ForkJoinPool common pool
            try {
                java.util.concurrent.ForkJoinPool.commonPool().shutdown();
                if (!java.util.concurrent.ForkJoinPool.commonPool().awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    java.util.concurrent.ForkJoinPool.commonPool().shutdownNow();
                }
            } catch (Exception e) {
                Logger.debug(this, "ForkJoinPool shutdown attempt failed: " + e.getMessage());
            }

            // Try to find and shutdown any remaining ExecutorServices via JMX (with safety checks)
            try {
                javax.management.MBeanServer server = java.lang.management.ManagementFactory.getPlatformMBeanServer();
                java.util.Set<javax.management.ObjectName> mbeans = server.queryNames(
                    new javax.management.ObjectName("java.util.concurrent:type=*"), null);
                for (javax.management.ObjectName mbean : mbeans) {
                    try {
                        // First check if the MBean supports the Shutdown attribute
                        javax.management.MBeanInfo mbeanInfo = server.getMBeanInfo(mbean);
                        boolean hasShutdownAttribute = false;
                        boolean hasShutdownOperation = false;

                        // Check for Shutdown attribute
                        for (javax.management.MBeanAttributeInfo attr : mbeanInfo.getAttributes()) {
                            if ("Shutdown".equals(attr.getName()) && attr.isReadable()) {
                                hasShutdownAttribute = true;
                                break;
                            }
                        }

                        // Check for shutdown operation
                        for (javax.management.MBeanOperationInfo op : mbeanInfo.getOperations()) {
                            if ("shutdown".equals(op.getName()) && op.getSignature().length == 0) {
                                hasShutdownOperation = true;
                                break;
                            }
                        }

                        // Only proceed if both attribute and operation exist
                        if (hasShutdownAttribute && hasShutdownOperation) {
                            Object result = server.getAttribute(mbean, "Shutdown");
                            if (result instanceof Boolean && !(Boolean)result) {
                                Logger.debug(this, "Shutting down ExecutorService MBean: " + mbean);
                                server.invoke(mbean, "shutdown", new Object[0], new String[0]);
                            }
                        }
                    } catch (Exception e) {
                        // Log specific failures for debugging, but continue with other MBeans
                        Logger.debug(this, "Failed to shutdown MBean " + mbean + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                Logger.debug(this, "JMX thread pool cleanup attempt failed: " + e.getMessage());
            }

            Logger.debug(this, "Thread pool cleanup completed");
        } catch (Exception e) {
            Logger.warn(this, "Thread pool cleanup failed: " + e.getMessage());
        }
    }
}
