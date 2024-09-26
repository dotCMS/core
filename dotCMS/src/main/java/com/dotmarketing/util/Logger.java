/*
 * UtilMethods.java
 *
 * Created on March 4, 2002, 2:56 PM
 */
package com.dotmarketing.util;

import com.dotcms.business.expiring.ExpiringMap;
import com.dotcms.business.expiring.ExpiringMapBuilder;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.system.logger.ChangeLoggerLevelEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.common.base.Objects;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.velocity.servlet.VelocityServlet;
import org.apache.velocity.tools.view.tools.ViewTool;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author David Torres
 */
public class Logger {

    /**
     * Caffeine Cache is going to be much more performant concurrently than a hashmap
     */
    private final static Cache<String, org.apache.logging.log4j.Logger> loggerMap =  
                    Caffeine.newBuilder()
                        .maximumSize(10000)
                        .expireAfterAccess(6,TimeUnit.HOURS)
                        .removalListener(new RemovalListener<String, org.apache.logging.log4j.Logger>() {
                            @Override
                            public void onRemoval(String key, org.apache.logging.log4j.Logger value, RemovalCause cause) {
                                System.out.println("removing Logger :" + key + " due to " + cause);
                            }
                        })
                        .build();

    public static void clearLoggers() {
        loggerMap.invalidateAll();
    }

    public static void clearLogger(Class clazz) {
        final String className = Try.of(() -> clazz.getName()).getOrElse(String.valueOf(clazz));
        clearLogger(className);
    }

    public static void clearLogger(String className) {
        loggerMap.invalidate(className);
    }


    private static org.apache.logging.log4j.Logger loadLogger(final String className) {
        if (UtilMethods.isEmpty(className)) {
            return loadLogger("dotCMSLogger");
        }

        return loggerMap.get(className, c ->  LogManager.getLogger(c) );
    }

    private static org.apache.logging.log4j.Logger loadLogger(final Class clazz) {
        final String className = Try.of(() -> clazz.getName()).getOrElse(String.valueOf(clazz));
        return loadLogger(className);
    }

    public static void info(Class clazz, final Supplier<String> message) {
       
            info(clazz, message.get());
        
    }

    public static void info(final Object ob, final Supplier<String> message) {

            info(ob.getClass(), message.get());
        
    }

    public static void info(Object ob, String message) {
        info(ob.getClass(), message);
    }

    public static void info(Class cl, String message) {
        if (isVelocityMessage(cl)) {
            velocityInfo(cl, message);
            return;
        }
        loadLogger(cl).info(message);
    }

    public static void info(String cl, String message) {
        loadLogger(cl).info(message);
    }

    public static void debug(final Object ob, final Supplier<String> message) {
        debug(ob.getClass(), message.get());
        
    }

    public static void debug(final String className, final Supplier<String> message) {
        debug(className, message.get());
        
    }

    public static void debug(final Object ob, final Throwable throwable, final Supplier<String> message) {
        debug(ob.getClass(), message.get(), throwable);

    }

    public static void debug(Object ob, String message) {
        debug(ob.getClass(), message);
    }

    public static void debug(Object ob, String message, Throwable ex) {
        debug(ob.getClass(), message, ex);
    }

    public static void debug(Class cl, String message) {
        if (isVelocityMessage(cl)) {
            velocityDebug(cl, message);
            return;
        }
        loadLogger(cl).debug(message);
    }

    public static void debug(String cl, String message) {
        loadLogger(cl).debug(message);
    }

    public static void debug(Class cl, String message, Throwable ex) {
        if (isVelocityMessage(cl)) {
            velocityDebug(cl, message, ex);
            return;
        }
        loadLogger(cl).debug(message, ex);
    }

    public static void error(Object ob, Throwable ex) {
        error(ob.getClass(), ex.getMessage(), ex);
    }

    public static void error(Object ob, String message) {
        error(ob.getClass(), message);
    }

    public static void error(Object ob, String message, Throwable ex) {
        error(ob.getClass(), message, ex);
    }

    public static void error(Class cl, String message) {
        if (isVelocityMessage(cl)) {
            velocityError(cl, message);
            return;
        }
        loadLogger(cl).error(message);
    }

    public static void error(String cl, String message) {
        loadLogger(cl).error(message);
    }

    public static void error(Class cl, String message, Throwable ex) {
        if (isVelocityMessage(cl)) {
            velocityError(cl, message, ex);
            return;
        }
        try {
            loadLogger(cl).error(message, ex);
        } catch (java.lang.IllegalStateException e) {
            ex.printStackTrace();
        }
    }

    public static void error(String cl, String message, Throwable ex) {

        try {
            loadLogger(cl).error(message, ex);
        } catch (java.lang.IllegalStateException e) {
            ex.printStackTrace();
        }
    }

    /**
     * a map with a 6 hrs max lifespan
     */
    static Lazy<ExpiringMap<Long, Long>> logMap =
                    Lazy.of(() -> new ExpiringMapBuilder().ttl(21600 * 1000).size(2000).build());

    /**
     * this method will print the message at WARN level every millis set and print the message plus
     * whole stack trace if at DEGUG level
     * 
     * @param cl
     * @param message
     * @param ex
     * @param warnEveryMillis
     */
    public static void warnEveryAndDebug(final Class cl, final String message, final Throwable ex,
                    final int warnEveryMillis) {
        warnEveryAndDebug(cl.getName(), message, ex, warnEveryMillis);
    }


    /**
     * this method will print the message at WARN level every millis set and print the message plus
     * whole stack trace if at DEGUG level
     * 
     * @param cl
     * @param message
     * @param ex
     * @param warnEveryMillis
     */
    public static void warnEveryAndDebug(final String cl, final String message, final Throwable ex,
                    final int warnEveryMillis) {
        if (ex == null) {
            return;
        }
        final org.apache.logging.log4j.Logger logger = loadLogger(cl);
        final StackTraceElement ste = ex.getStackTrace()[0];

        final Long hash = Long.valueOf(Objects.hashCode(ste, message.substring(0, Math.min(message.length(), 10)), cl));
        final Long expireWhen = logMap.get().get(hash);

        if (expireWhen == null || expireWhen < System.currentTimeMillis()) {
            logMap.get().put(hash, System.currentTimeMillis() + warnEveryMillis, true);
            logger.warn(message + " (every " + warnEveryMillis + " millis)");

        }
        logger.debug(() -> message, ex);
    }

    /**
     * this method will print the message at WARN level every millis set and print the message plus
     * whole stack trace if at DEGUG level
     * 
     * @param cl
     * @param message
     * @param ex
     * @param warnEveryMillis
     */
    public static void infoEvery(final String cl, final String message, final int warnEveryMillis) {

        final org.apache.logging.log4j.Logger logger = loadLogger(cl);


        final Long hash = Long.valueOf(Objects.hashCode(cl, message.substring(0, Math.min(message.length(), 10)), cl));
        final Long expireWhen = logMap.get().get(hash);
        if (expireWhen == null || expireWhen < System.currentTimeMillis()) {
            logMap.get().put(hash, System.currentTimeMillis() + warnEveryMillis, true);
            logger.info(message + " (every " + warnEveryMillis + " millis)");
        }
        logger.debug(() -> message);
    }

    public static void infoEvery(final Class cl, final String message, final int warnEveryMillis) {
        infoEvery(cl.getName(), message, warnEveryMillis);

    }

    public static void warnEveryAndDebug(final Class cl, final Throwable ex, final int ttlMillis) {
        warnEveryAndDebug(cl.getName(), ex.getMessage(), ex, ttlMillis);
    }


    /**
     * this method will print the message if at the WARN level and print the message + whole stack trace
     * if at the DEGUG LEVEL
     * 
     * @param cl
     * @param message
     * @param ex
     */
    public static void warnAndDebug(Class cl, String message, Throwable ex) {
        warnAndDebug(cl.getName(), message, ex);
    }

    /**
     * this method will print the message if at the WARN level and print the message + whole stack trace
     * if at the DEGUG LEVEL
     * 
     * @param cl
     * @param message
     * @param ex
     */
    public static void warnAndDebug(final String clazz, final String message, final Throwable ex) {
        final org.apache.logging.log4j.Logger logger = loadLogger(clazz);
        logger.warn(message);
        try {
            // Never swallow the original message, EVER! Unless it's the same as the current one
            if (!message.equalsIgnoreCase(ex.getMessage())) {
                logger.warn(ex.getMessage());
            }
            if (UtilMethods.isSet(ex.getStackTrace()) && null != ex.getStackTrace()[0]) {
                logger.warn(ex.getStackTrace()[0]);
            }
            logger.debug(() -> message, ex);
        } catch (final IllegalStateException e) {
            logger.warn(String.format("Failed to log warnAndDebug message: %s",
                    ExceptionUtil.getErrorMessage(e)));
        }
    }

    /**
     * this method will print the message if at the WARN level and print the message + whole stack trace
     * if at the DEGUG LEVEL
     * 
     * @param cl
     * @param message
     * @param ex
     */
    public static void warnAndDebug(Class cl, Throwable ex) {
        warnAndDebug(cl, ex.getMessage(), ex);
    }


    public static void fatal(Object ob, String message) {
        fatal(ob.getClass(), message);
    }

    public static void fatal(Object ob, String message, Throwable ex) {
        fatal(ob.getClass(), message, ex);
    }

    public static void fatal(Class cl, String message) {
        if (isVelocityMessage(cl)) {
            velocityFatal(cl, message);
            return;
        }
        loadLogger(cl).fatal(message);
    }

    public static void fatal(Class cl, String message, Throwable ex) {
        if (isVelocityMessage(cl)) {
            velocityFatal(cl, message, ex);
            return;
        }
        loadLogger(cl).fatal(message, ex);
    }

    public static void warn(final Object ob, final Supplier<String> message) {

            warn(ob.getClass(), message.get());
        
    }

    public static void warn(final String className, final Supplier<String> message) {

            warn(className, message.get());
        
    }

    public static void warn(Class clazz, final Supplier<String> message) {

            warn(clazz, message.get());
        
    }

    public static void warn(Class clazz, final Supplier<String> message, Throwable ex) {
        warn(clazz, message.get(), ex);
    }

    public static void warn(Object ob, String message) {
        warn(ob.getClass(), message);
    }

    public static void warn(Object ob, String message, Throwable ex) {
        warn(ob.getClass(), message, ex);
    }

    public static void warn(Class cl, String message) {
        if (isVelocityMessage(cl)) {
            velocityWarn(cl, message);
            return;
        }
        loadLogger(cl).warn(message);
    }

    public static void warn(String cl, String message) {

        loadLogger(cl).warn(message);
    }

    public static void warn(Class cl, String message, Throwable ex) {
        if (isVelocityMessage(cl)) {
            velocityWarn(cl, message, ex);
            return;
        }
        loadLogger(cl).warn(message, ex);
    }

    public static boolean isDebugEnabled(Class cl) {

        return loadLogger(cl).isDebugEnabled();
    }

    public static boolean isDebugEnabled(String cl) {

        return loadLogger(cl).isDebugEnabled();
    }

    public static boolean isInfoEnabled(Class cl) {
        return loadLogger(cl).isInfoEnabled();

    }

    public static boolean isWarnEnabled(Class cl) {

        return loadLogger(cl).isWarnEnabled();

    }

    public static boolean isWarnEnabled(String cl) {

        return loadLogger(cl).isWarnEnabled();

    }

    public static boolean isErrorEnabled(Class cl) {

        return loadLogger(cl).isErrorEnabled();

    }

    public static org.apache.logging.log4j.Logger getLogger(final String className) {

        return loadLogger(className);
    }

    public static org.apache.logging.log4j.Logger getLogger(Class cl) {

        return loadLogger(cl);
    }


    public static void velocityError(Class cl, String message, Throwable thr) {
        loadLogger(cl).error(message + " @ " + Thread.currentThread().getName(), thr);
    }

    public static void velocityWarn(Class cl, String message, Throwable thr) {
        loadLogger(cl).warn(message + " @ " + Thread.currentThread().getName(), thr);
    }

    public static void velocityInfo(Class cl, String message, Throwable thr) {
        loadLogger(cl).info(message + " @ " + Thread.currentThread().getName(), thr);
    }

    public static void velocityFatal(Class cl, String message, Throwable thr) {
        loadLogger(cl).fatal(message + " @ " + Thread.currentThread().getName(), thr);
    }

    public static void velocityDebug(Class cl, String message, Throwable thr) {
        loadLogger(cl).debug(message + " @ " + Thread.currentThread().getName(), thr);
    }

    public static void velocityError(Class cl, String message) {
        loadLogger(cl).error(message + " @ " + Thread.currentThread().getName());
    }

    public static void velocityWarn(Class cl, String message) {
        loadLogger(cl).warn(message + " @ " + Thread.currentThread().getName());
    }

    public static void velocityInfo(Class cl, String message) {
        loadLogger(cl).info(message + " @ " + Thread.currentThread().getName());
    }

    public static void velocityFatal(Class cl, String message) {
        loadLogger(cl).fatal(message + " @ " + Thread.currentThread().getName());
    }

    public static void velocityDebug(Class cl, String message) {
        loadLogger(cl).debug(message + " @ " + Thread.currentThread().getName());
    }


    private static String normalizeTemplate(Object t) {
        if (t == null) {
            return null;
        }
        String x = t.toString();
        x = x.replace(File.separatorChar, '/');
        x = (x.indexOf("assets") > -1) ? x.substring(x.lastIndexOf("assets"), x.length()) : x;
        x = (x.startsWith("/")) ? x : "/" + x;

        return x;
    }


    private static boolean isVelocityMessage(Object obj) {
        if (obj == null)
            return false;
        return isVelocityMessage(obj.getClass());
    }

    private static boolean isVelocityMessage(Class clazz) {
        boolean ret = false;
        if (clazz != null && clazz.getName() != null) {
            String name = clazz.getName().toLowerCase();
            ret = name.contains("velocity") || name.contains("viewtool");

            if (!ret) {
                ret = ViewTool.class.isAssignableFrom(clazz);
            }
            if (!ret) {
                ret = VelocityServlet.class.isAssignableFrom(clazz);
            }
        }
        return ret;

    }

    /**
     * Set the Level of the logger; if the logger does not exists or can not set, return null
     * 
     * @param loggerName {@link String} logger name
     * @param level {@link String} logger level
     * @return Object
     */
    public static Object setLevel(final String loggerName, final String level) {

        final Object logger = getLogger(loggerName);
        if (null != logger) {

            if (logger instanceof org.apache.logging.log4j.core.Logger) {

                final Level logLevel = Level.getLevel(level);
                org.apache.logging.log4j.core.Logger.class.cast(logger).setLevel(logLevel);
                return logger;
            }
        }

        return null;
    }

    /**
     * Determinate if the level is a valid one
     * 
     * @param level {@link String}
     * @return boolean
     */
    public static boolean isValidLevel(final String level) {

        return null != Level.getLevel(level);
    }

    /**
     * Get the list of current collection on cache app
     * 
     * @return List
     */
    public static List<Object> getCurrentLoggers() {

        return loggerMap.asMap().values().stream().collect(Collectors.toList());
    }

    /**
     * Handler when the log change the level on another cluster node
     * @param event {@link ChangeLoggerLevelEvent}
     */
    public static void onChangeLoggerLevelEventHandler(final ChangeLoggerLevelEvent event) {

        final String [] loggerNames = null != event.getName()?
                event.getName().split(StringPool.COMMA):new String[]{};
        final String level          = event.getLevel();

        if (Logger.isValidLevel(level) && UtilMethods.isSet(loggerNames)) {

            Stream.of(loggerNames).forEach(loggerName -> Logger.setLevel(loggerName, level));
        }
    }
}
