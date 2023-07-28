package com.dotcms.api.client;

import com.starxg.keytar.Keytar;
import com.starxg.keytar.KeytarException;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class KeyTarPasswordStoreImpl implements SecurePasswordStore {

    Keytar keytar = Keytar.getInstance();

    @Override
    public void setPassword(String service, String account, String password) throws StoreSecureException {
        try {
            keytar.setPassword(service, account, password);
        } catch (KeytarException e) {
            throw new StoreSecureException("Failure saving password securely",e);
        }
    }

    @Override
    public String getPassword(String service, String account) throws StoreSecureException {
        try {
            return keytar.getPassword(service, account);
        } catch (KeytarException e) {
            throw new StoreSecureException("Failure retrieving password from secure storage",e);
        }
    }

    @Override
    public void deletePassword(String service, String account) throws StoreSecureException {
        try {
            keytar.deletePassword(service, account);
        } catch (KeytarException e) {
            throw new StoreSecureException("Failure deleting password from secure storage",e);
        }
    }
}
