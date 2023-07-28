package com.dotcms.api.client;

import io.quarkus.arc.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

@Alternative
@Priority(1)
@ApplicationScoped
public class MockUnsupportedSecurePasswordStoreImpl implements SecurePasswordStore{

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
