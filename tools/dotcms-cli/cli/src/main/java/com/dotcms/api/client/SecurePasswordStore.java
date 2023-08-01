package com.dotcms.api.client;

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
