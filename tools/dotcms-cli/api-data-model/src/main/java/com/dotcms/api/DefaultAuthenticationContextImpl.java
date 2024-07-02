package com.dotcms.api;

import com.dotcms.api.client.model.AuthenticationParam;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.annotation.SecuredPassword;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.TokenEntity;
import com.dotcms.model.config.CredentialsBean;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.user.User;
import io.quarkus.arc.All;
import io.quarkus.arc.DefaultBean;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@DefaultBean
@ApplicationScoped
public class DefaultAuthenticationContextImpl implements AuthenticationContext {

    @Inject
    Logger logger;

    @Inject
    @All
    List<ServiceManager> serviceManagers;

    ServiceManager serviceManager;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    AuthenticationParam authenticationParam;

    private String user;

    private char[] token;

    @ConfigProperty(name = "com.dotcms.api.token.expiration", defaultValue = "10")
    Integer expirationDays;

    @PostConstruct
    void init(){
        //Always prefer a Service Manager that can securely keep passwords
        Optional<ServiceManager> optional = serviceManagers.stream().filter(manager -> {
            final SecuredPassword[] annotationsByType = manager.getClass().getAnnotationsByType(SecuredPassword.class);
            return annotationsByType.length > 0;
        }).findFirst();
        serviceManager = optional.orElseGet(() -> serviceManagers.get(0));
        //if we fail to read configuration we should fail fast and halt the application
        readConfiguration(serviceManager);
    }

    void readConfiguration(ServiceManager serviceManager){
        try {
            serviceManager.selected().ifPresent(serviceBean -> {
                final CredentialsBean credentials = serviceBean.credentials();
                if (null != credentials) {
                    this.user = credentials.user();
                    final Optional<char[]> chars = credentials.loadToken();
                    if(chars.isPresent()){
                      this.token = chars.get();
                    } else {
                        logger.warn("No token found for user " + user);
                    }
                }
            });
        }catch (IOException e){
            logger.fatal("Error starting up application check your configuration file and make sure it is well formed. See dotcms-cli.log for more details. ");
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<String> getUser() {
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<char[]> getToken() throws IOException {

        //This injects the token from the command line if present
        final Optional<char[]> paramToken = authenticationParam.getToken();
        if(paramToken.isPresent()){
            return paramToken;
        }
        //Otherwise we try to load it from the service manager
        final Optional<String> optionalUser = getUser();
        if (optionalUser.isPresent()) {
            if(null != token  && token.length > 0){
              return Optional.of(token);
            }
            final String userString = optionalUser.get();
            final Optional<ServiceBean> selected = serviceManager.selected();
            if(selected.isEmpty()){
                throw new IllegalStateException("No service selected, unable to load token.");
            }
            final Optional<char[]> optionalToken = loadToken(selected.get().name(), userString);
            optionalToken.ifPresent(s -> token = s);
            return optionalToken;
        }
        return Optional.empty();
    }

    @Override
    public void login(final String user, final char[] password) throws IOException {
        final AuthenticationAPI api = clientFactory.getClient(AuthenticationAPI.class);
        final ResponseEntityView<TokenEntity> responseEntityView = api.getToken(
                APITokenRequest.builder().user(user).password(password)
                        .expirationDays(expirationDays).build());
        saveCredentials(user, responseEntityView.entity().token());
    }

    @Override
    public void login(char[] token) throws IOException {
        authenticationParam.setToken(token);
        final UserAPI userAPI = clientFactory.getClient(UserAPI.class);
        final User current = userAPI.getCurrent();
        saveCredentials(current.userId(), token);
    }

    private void saveCredentials(final String user, final char[] token) {
        try {
            final Optional<ServiceBean> selected = serviceManager.selected();
            if(selected.isEmpty()){
                throw new IllegalStateException("No service selected, unable to save credentials.");
            }
            final ServiceBean serviceBean = ServiceBean.builder().active(true)
                    .name(selected.get().name()).url(selected.get().url())
                    .credentials(CredentialsBean.builder().user(user).token(token).build()).build();
            serviceManager.persist(serviceBean);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        this.user = user;
        this.token = token;
    }

    private Optional<char[]> loadToken(String serviceKey, String user) throws IOException {

        final List<ServiceBean> profiles = serviceManager.services();
        final Optional<ServiceBean> optional = profiles.stream()
                .filter(serviceBean -> serviceKey.equals(serviceBean.name())).findFirst();
        if (optional.isPresent()) {
            final ServiceBean bean = optional.get();
            if (bean.credentials() != null  && user.equals(bean.credentials().user()) ) {
                return bean.credentials().loadToken();
            }
        }
        return Optional.empty();
    }

    public void reset(){
        this.user = null;
        if(null != this.token && this.token.length > 0){
             Arrays.fill(this.token,(char)0);
        }
        this.token = null;
    }

}
