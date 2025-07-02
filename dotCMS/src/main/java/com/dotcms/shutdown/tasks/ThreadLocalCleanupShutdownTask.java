package com.dotcms.shutdown.tasks;

import com.dotcms.shutdown.ShutdownOrder;
import com.dotcms.shutdown.ShutdownTask;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.lang.reflect.Field;

@ApplicationScoped
@ShutdownOrder(95)
public class ThreadLocalCleanupShutdownTask implements ShutdownTask {

    @Override
    public String getName() {
        return "ThreadLocal cleanup";
    }

    @Override
    public void run() {
        try {
            Logger.info(this, "Starting ThreadLocal cleanup");
            
            // Clean up known ThreadLocal instances to prevent memory leaks
            cleanupKnownThreadLocals();
            
            Logger.info(this, "ThreadLocal cleanup completed");
        } catch (Exception e) {
            Logger.warn(this, "ThreadLocal cleanup failed: " + e.getMessage(), e);
        }
    }

    private void cleanupKnownThreadLocals() {
        // Clean up ThreadContextUtil ThreadLocal
        cleanupThreadContextUtil();
        
        // Clean up RegEX ThreadLocals 
        cleanupRegEXThreadLocals();
        
        // Clean up HibernateUtil session ThreadLocal
        cleanupHibernateUtilThreadLocal();
        
        // Clean up any other dotCMS-specific ThreadLocals
        cleanupOtherThreadLocals();
    }

    private void cleanupThreadContextUtil() {
        try {
            Class<?> threadContextUtilClass = Class.forName("com.dotcms.util.ThreadContextUtil");
            Field contextLocalField = threadContextUtilClass.getDeclaredField("contextLocal");
            contextLocalField.setAccessible(true);
            ThreadLocal<?> contextLocal = (ThreadLocal<?>) contextLocalField.get(null);
            if (contextLocal != null) {
                contextLocal.remove();
                Logger.debug(this, "Cleaned up ThreadContextUtil ThreadLocal");
            }
        } catch (Exception e) {
            Logger.debug(this, "Could not cleanup ThreadContextUtil ThreadLocal: " + e.getMessage());
        }
    }

    private void cleanupRegEXThreadLocals() {
        try {
            Class<?> regexClass = Class.forName("com.dotmarketing.util.RegEX");
            
            // Get singleton instance
            java.lang.reflect.Method getInstanceMethod = regexClass.getDeclaredMethod("getInstance");
            getInstanceMethod.setAccessible(true);
            Object regexInstance = getInstanceMethod.invoke(null);
            
            // Clean up localP5Matcher ThreadLocal
            Field matcherField = regexClass.getDeclaredField("localP5Matcher");
            matcherField.setAccessible(true);
            ThreadLocal<?> localP5Matcher = (ThreadLocal<?>) matcherField.get(regexInstance);
            if (localP5Matcher != null) {
                localP5Matcher.remove();
                Logger.debug(this, "Cleaned up RegEX localP5Matcher ThreadLocal");
            }
            
            // Clean up localP5Sub ThreadLocal
            Field subField = regexClass.getDeclaredField("localP5Sub");
            subField.setAccessible(true);
            ThreadLocal<?> localP5Sub = (ThreadLocal<?>) subField.get(regexInstance);
            if (localP5Sub != null) {
                localP5Sub.remove();
                Logger.debug(this, "Cleaned up RegEX localP5Sub ThreadLocal");
            }
        } catch (Exception e) {
            Logger.debug(this, "Could not cleanup RegEX ThreadLocals: " + e.getMessage());
        }
    }

    private void cleanupHibernateUtilThreadLocal() {
        try {
            Class<?> hibernateUtilClass = Class.forName("com.dotmarketing.db.HibernateUtil");
            Field sessionHolderField = hibernateUtilClass.getDeclaredField("sessionHolder");
            sessionHolderField.setAccessible(true);
            ThreadLocal<?> sessionHolder = (ThreadLocal<?>) sessionHolderField.get(null);
            if (sessionHolder != null) {
                sessionHolder.remove();
                Logger.debug(this, "Cleaned up HibernateUtil sessionHolder ThreadLocal");
            }
        } catch (Exception e) {
            Logger.debug(this, "Could not cleanup HibernateUtil ThreadLocal: " + e.getMessage());
        }
    }

    private void cleanupOtherThreadLocals() {
        // Clean up any other ThreadLocals that might be in the system
        try {
            // Clean up DbConnectionFactory connections ThreadLocal
            Class<?> dbConnectionFactoryClass = Class.forName("com.dotmarketing.db.DbConnectionFactory");
            Field connectionsHolderField = dbConnectionFactoryClass.getDeclaredField("connectionsHolder");
            connectionsHolderField.setAccessible(true);
            ThreadLocal<?> connectionsHolder = (ThreadLocal<?>) connectionsHolderField.get(null);
            if (connectionsHolder != null) {
                connectionsHolder.remove();
                Logger.debug(this, "Cleaned up DbConnectionFactory connectionsHolder ThreadLocal");
            }
        } catch (Exception e) {
            Logger.debug(this, "Could not cleanup DbConnectionFactory ThreadLocal: " + e.getMessage());
        }
        
        // Add cleanup for any other known ThreadLocal leaks as they are discovered
    }

    @Override
    public int getTimeoutSeconds() {
        return 5; // ThreadLocal cleanup should be fast
    }
}