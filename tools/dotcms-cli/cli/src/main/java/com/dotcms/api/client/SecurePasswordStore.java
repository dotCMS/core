package com.dotcms.api.client;

/**
 * SecurePasswordStore provides an interface for securely storing and retrieving passwords.
 * The setPassword method stores a password for the given service and account.
 * The getPassword method retrieves the password for the given service and account.
 * The deletePassword method deletes the stored password for the given service and account.
 * StoreSecureException is thrown if there are any errors storing, retrieving or deleting passwords.
 * It extends Exception and provides constructors to set the error message and optional cause Throwable.
 */
public interface SecurePasswordStore {

    void setPassword(String service, String account, String password) throws StoreSecureException;

    String getPassword(String service, String account) throws StoreSecureException;

    void deletePassword(String service, String account) throws StoreSecureException;

    class StoreSecureException extends java.lang.Exception {
            public StoreSecureException(String message) {
                super(message);
            }

            public StoreSecureException(String message, Throwable cause) {
                super(message, cause);
            }
    }

}
