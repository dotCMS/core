package com.dotcms.api.client;

import com.starxg.keytar.Keytar;
import com.starxg.keytar.KeytarException;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * KeyTarPasswordStoreImpl implements the SecurePasswordStore interface to securely store passwords using the system keychain.
 * It uses the Keytar library to interface with the native keychain/credential manager of the operating system.
 * The setPassword method stores the password for the given service and account.
 * It wraps any KeytarException in a StoreSecureException.
 * The getPassword method retrieves the password for the given service and account.
 * It wraps any KeytarException in a StoreSecureException.
 * The deletePassword method deletes the password for the given service and account.
 * It wraps any KeytarException in a StoreSecureException.
 */
@ApplicationScoped
public class KeyTarPasswordStoreImpl implements SecurePasswordStore {

    // START-NOSCAN
    private Keytar instance;

    private boolean unsupported = false;

    Optional<Keytar> getInstance(){
       if(null == instance && !unsupported){
          try {
              instance = Keytar.getInstance();
          } catch (Error e) {
               unsupported = true;
          }
       }
       return Optional.ofNullable(instance);
    }

    Keytar getInstanceOrThrow() throws StoreSecureException{
       return getInstance().orElseThrow(() -> new StoreSecureException("Keytar is not supported on this platform"));
    }

    @Override
    public void setPassword(String service, String account, String password) throws StoreSecureException {
        final Keytar keytar = getInstanceOrThrow();
        try {
            keytar.setPassword(service, account, password);
        } catch (Exception | Error  e) {
            throw new StoreSecureException("Failure saving password securely",e);
        }
    }

    @Override
    public String getPassword(String service, String account) throws StoreSecureException {
        final Keytar keytar = getInstanceOrThrow();
        try {
            return keytar.getPassword(service, account);
        } catch (Exception | Error e) {
            throw new StoreSecureException("Failure retrieving password from secure storage",e);
        }
    }

    @Override
    public void deletePassword(String service, String account) throws StoreSecureException {
        final Keytar keytar = getInstanceOrThrow();
        try {
            keytar.deletePassword(service, account);
        } catch (Exception | Error e) {
            throw new StoreSecureException("Failure deleting password from secure storage",e);
        }
    }
    // END-NOSCAN
}
