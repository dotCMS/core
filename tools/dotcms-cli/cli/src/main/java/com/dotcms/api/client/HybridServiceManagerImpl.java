package com.dotcms.api.client;

import com.dotcms.api.client.SecurePasswordStore.StoreSecureException;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.model.annotation.SecuredPassword;
import com.dotcms.model.config.CredentialsBean;
import com.dotcms.model.config.ServiceBean;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.quarkus.arc.Unremovable;
import org.jboss.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * This class offers a Hybrid strategy storing sensitive info using native system's keychain support when possible
 * Other service related properties are stored in the yml file
 */
@SecuredPassword
@ApplicationScoped
public class HybridServiceManagerImpl implements ServiceManager {
    private static final char[] EMPTY_TOKEN = "".toCharArray();

    @Inject
    ServiceManager defaultManager;

    @Inject
    Logger logger;


    @Inject
    @Unremovable
    SecurePasswordStore passwordStore;

    @Override
    @CanIgnoreReturnValue
    public ServiceManager persist(ServiceBean service) throws IOException {
        CredentialsBean credentialsBean = service.credentials();
        if (null != credentialsBean && null != credentialsBean.user() && credentialsBean.loadToken().isPresent()) {
            final Optional<char[]> token = credentialsBean.loadToken();
            //We need to split the info and save in the KeyChain the authentication token
            if (token.isPresent()){
                try {
                    //First store the sensitive data in the keychain
                    passwordStore.setPassword(service.name(), credentialsBean.user(), new String(token.get()));
                    // then strip any sensitive data from the info that is going to be saved into the yml/text file
                    CredentialsBean strippedTokenBean = CredentialsBean.builder().from(credentialsBean)
                            .token(EMPTY_TOKEN).tokenSupplier(()->EMPTY_TOKEN).build();
                    ServiceBean strippedCredentialsBean = ServiceBean.builder().from(service).credentials(strippedTokenBean).build();
                    defaultManager.persist(strippedCredentialsBean);
                } catch (StoreSecureException  e) {
                    logger.warn("Credentials are stored in plain text!");
                    //Now upon error we need to fall back to YML file for storage
                    defaultManager.persist(service);
                }
            }
            return this;
        }
        //In case of missing info or an error proceed to store the service info as text
        defaultManager.persist(service);
        return this;
    }

    @Override
    public List<ServiceBean> services() throws IOException {
        //Retrieve the beans stored in the yml file
        final List<ServiceBean> services = defaultManager.services();
        final List<ServiceBean> beans = new ArrayList<>(services.size());
        //Now we have to join them with the credentials stored in the keyChain
        for (ServiceBean service : services) {
            CredentialsBean credentialsBean = service.credentials();
            if (null == credentialsBean) {
                logger.info(String.format("Service [%s] is missing credentials.", service.name()));
            } else {
                final String name = service.name();
                final String user = credentialsBean.user();
                CredentialsBean newCredentialsBean = CredentialsBean.builder().from(credentialsBean)
                        .tokenSupplier(() -> {
                            try {
                                return passwordStore.getPassword(name, user).toCharArray();
                            } catch (Exception e) {
                                logger.warn(
                                        "Unable to recover token from key-chain. it probably stored as plain text.");
                                return EMPTY_TOKEN;
                            }
                        }).build();
                ServiceBean bean = ServiceBean.builder().from(service)
                        .credentials(newCredentialsBean).build();
                beans.add(bean);
                continue;
            }
            //This should take off any bean that could have found an error or missing credentials
            beans.add(service);
        }
        //This makes it return an immutable list
        return List.of(beans.toArray(new ServiceBean[]{}));
    }

    @Override
    @CanIgnoreReturnValue
    public ServiceManager removeAll() throws IOException {
        List<ServiceBean> services = defaultManager.services();
        for (ServiceBean service : services) {
            CredentialsBean credentialsBean = service.credentials();
            if (null != credentialsBean && null != credentialsBean.user()) {
                try {
                    passwordStore.deletePassword(service.name(), credentialsBean.user());
                } catch (StoreSecureException e) {
                    logger.warn("Token wasn't deleted from key-chain. it probably stored as plain text.");
                }
            }
        }
        defaultManager.removeAll();
        return this;
    }

    @Override
    public Optional<ServiceBean> selected() throws IOException {
        //It's cheaper if we use base impl
        return defaultManager.services().stream().filter(ServiceBean::active)
                .findFirst();
    }
}
