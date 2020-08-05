package com.dotcms.saml;

/**
 * This interface will be implemented in order to provide the bridge between the message on the bundle and the client.
 * This can be handle by just logging or something else when an issue happen.
 * By now the interface is most oriented to loggers
 * @author jsanca
 */
public interface MessageObserver {

    /**
     * Updates to the observers when there is an error message
     * @param clazz
     * @param message
     */
    void updateError(Class clazz, String message);

    /**
     * Updates to the observers when there is an error.
     * @param clazz
     * @param message
     * @param throwable
     */
    void updateError(Class clazz, String message, Throwable throwable);

    void updateError(Class clazz, String message, Object... arguments);

    /**
     * Updates to the observers when there is an debug.
     * @param clazz
     * @param message
     */
    void updateDebug(Class clazz, String message);

    /**
     * Updates to the observers when there is an info.
     * @param clazz
     * @param message
     */
    void updateInfo(Class clazz, String message);

    /**
     * Updates to the observers when there is a warning
     * @param clazz
     * @param message
     */
    void updateWarning(Class clazz, String message);
}
