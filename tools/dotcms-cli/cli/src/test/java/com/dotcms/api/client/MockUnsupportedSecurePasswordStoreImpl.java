package com.dotcms.api.client;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

/**
 * When running on a non-OSX system, the KeyChain is not always available, so we need to verify if our code can still survive such scenario
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class MockUnsupportedSecurePasswordStoreImpl implements SecurePasswordStore {

        @Override
        public void setPassword(String service, String account, String password) throws StoreSecureException {
            throw new StoreSecureException("Unsupported operation");
        }

        @Override
        public String getPassword(String service, String account) throws StoreSecureException {
            throw new StoreSecureException("Unsupported operation");
        }

        @Override
        public void deletePassword(String service, String account) throws StoreSecureException {
            throw new StoreSecureException("Unsupported operation");
        }
}
