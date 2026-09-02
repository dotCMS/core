package com.dotmarketing.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.logConsole.model.LogMapper;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

import io.vavr.control.Try;

import java.util.function.Supplier;

public class SecurityLogger {

    private static final String filename = "dotcms-security.log";

    public static void logInfo(final Class<?> clazz, final Supplier<String> message) {
        logInfo(clazz, message.get());
    }

    public static void logInfo(final Class<?> clazz, final String msg) {

        Logger.info(SecurityLogger.class, clazz.toString() + " : " + annointMessage(msg));
        
    }

    public static void logWarn(final Class<?> clazz, final Supplier<String> message) {
        logWarn(clazz, message.get());
    }

    public static void logWarn(final Class<?> clazz, final String msg) {
        Logger.warn(SecurityLogger.class, clazz.toString() + " : " + annointMessage(msg));
    }

    public static void logDebug(final Class<?> clazz, final Supplier<String> message) {
        logDebug(clazz, message.get());
    }

    public static void logDebug(final Class<?> clazz, final String msg) {
        Logger.debug(SecurityLogger.class, clazz.toString() + " : " + annointMessage(msg));
    }
    
    private static String annointMessage(String msg) {
      
      final String ipAddress=Try.of(()->HttpServletRequestThreadLocal.INSTANCE.getRequest().getRemoteAddr()).getOrElse("ukn");
      final User user=Try.of(()->PortalUtil.getUser(HttpServletRequestThreadLocal.INSTANCE.getRequest())).getOrNull();
      return msg + " -- ip:" + ipAddress + ",user:" + user;
    }

}