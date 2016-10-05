package com.dotcms.util;

import java.io.Serializable;

/**
 * Factory for the Security Logger Factory.
 *
 * @author jsanca
 */
public class SecurityLoggerServiceAPIFactory implements Serializable {

    private final SecurityLoggerServiceAPI securityLogger = new SecurityLoggerServiceAPIFactory.SecurityLoggerApiImpl();

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


    public SecurityLoggerServiceAPI getSecurityLoggerAPI() {

        return this.securityLogger;
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
