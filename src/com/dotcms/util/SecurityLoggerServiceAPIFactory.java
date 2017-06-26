package com.dotcms.util;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;

import java.io.Serializable;

/**
 * Factory for the Security Logger Factory.
 *
 * @author jsanca
 */
public class SecurityLoggerServiceAPIFactory implements Serializable {

    private final SecurityLoggerServiceAPI securityLogger = new SecurityLoggerServiceAPIFactory.SecurityLoggerApiImpl();
    private SecurityLoggerServiceAPI alternativeSecurityLogger = null;

    private SecurityLoggerServiceAPIFactory() {
        // singleton
    }

    private static class SingletonHolder {
        private static final SecurityLoggerServiceAPIFactory INSTANCE = new SecurityLoggerServiceAPIFactory();
    }
    /**
     * Get the instance.
     * @return JsonWebTokenFactory
     */
    public static SecurityLoggerServiceAPIFactory getInstance() {

        return SecurityLoggerServiceAPIFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Note: this method should be use only for testing goals.
     */
    @VisibleForTesting
    public static void setAlternativeSecurityLogger (final SecurityLoggerServiceAPI alternativeSecurityLogger) {

        getInstance().alternativeSecurityLogger = alternativeSecurityLogger;
    }

    public SecurityLoggerServiceAPI getSecurityLoggerAPI() {

        return (null == alternativeSecurityLogger)?this.securityLogger:this.alternativeSecurityLogger;
    }

    private final class SecurityLoggerApiImpl implements SecurityLoggerServiceAPI {

        @Override
        public void logInfo(final Class cl, final String msg) {
            SecurityLoggerServiceAPI.super.logInfo(cl, msg);
        }

        @Override
        public void logDebug(final Class cl, final String msg) {
            SecurityLoggerServiceAPI.super.logDebug(cl, msg);
        }
    } // SecurityLoggerImpl.

} // E:O:F:SecurityLoggerServiceAPIFactory.