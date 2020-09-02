package com.dotcms.saml;

import com.dotmarketing.util.Logger;

import java.text.MessageFormat;

/**
 * Implementation of the {@link MessageObserver} as a dot logger
 * @author jsanca
 */
public class DotLoggerMessageObserver implements MessageObserver {
    @Override
    public void updateError(final String aClass, final String message) {

        Logger.error(aClass, message);
    }

    @Override
    public void updateError(final String aClass, final String message, final Throwable throwable) {

        Logger.error(aClass, message, throwable);
    }

    @Override
    public void updateError(final String aClass, final String message, final Object... objects) {

        final String finalMessage = MessageFormat.format(message, objects);
        Logger.error(aClass, finalMessage);
    }

    @Override
    public void updateDebug(final String aClass, final String message) {

        Logger.debug(aClass, ()->message);
    }

    @Override
    public void updateInfo(final String aClass, final String message) {

        Logger.info(aClass, message);
    }

    @Override
    public void updateWarning(final String aClass, final String message) {

        Logger.warn(aClass, ()->message);
    }
}
