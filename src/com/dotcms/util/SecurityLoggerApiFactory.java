package com.dotcms.util;

import java.io.Serializable;

/**
 * Factory for the Security Logger Factory.
 * 
 * @author jsanca
 */
public class SecurityLoggerAPIFactory implements Serializable {

    private final SecurityLoggerAPI securityLogger = new SecurityLoggerAPIFactory.SecurityLoggerApiImpl();

    private SecurityLoggerAPIFactory() {
        // singleton
    }

    private static class SingletonHolder {
        private static final SecurityLoggerAPIFactory INSTANCE = new SecurityLoggerAPIFactory();
    }
    /**
     * Get the instance.
     * @return JsonWebTokenFactory
     */
    public static SecurityLoggerAPIFactory getInstance() {

        return SecurityLoggerAPIFactory.SingletonHolder.INSTANCE;
    } // getInstance.


    public SecurityLoggerAPI getSecurityLoggerAPI() {

        return this.securityLogger;
    }

    private final class SecurityLoggerApiImpl implements SecurityLoggerAPI {

        @Override
        public void logInfo(final Class cl, final String msg) {
            SecurityLoggerAPI.super.logInfo(cl, msg);
        }

        @Override
        public void logDebug(final Class cl, final String msg) {
            SecurityLoggerAPI.super.logDebug(cl, msg);
        }
    } // SecurityLoggerImpl.

} // E:O:F:SecurityLoggerAPIFactory.
