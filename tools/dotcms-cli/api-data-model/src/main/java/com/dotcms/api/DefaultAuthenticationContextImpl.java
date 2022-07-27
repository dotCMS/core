package com.dotcms.api;


import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.TokenEntity;
import com.dotcms.model.config.CredentialsBean;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.StartupEvent;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;


@DefaultBean
@ApplicationScoped
public class DefaultAuthenticationContextImpl implements AuthenticationContext {

    @Inject
    ServiceManager serviceManager;

    @Inject
    RestClientFactory clientFactory;

    private String user;

    private String token;

    @ConfigProperty(name = "com.dotcms.api.token.expiration", defaultValue = "10")
    Integer expirationDays;

    @Override
    public Optional<String> getUser() {
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<String> getToken() {
        final Optional<String> optionalUser = getUser();
        if (optionalUser.isPresent()) {
            if(null != token){
              return Optional.of(token);
            }
            final String userString = optionalUser.get();
            final Optional<String> optionalToken = loadToken(getServiceKey(), userString);
            optionalToken.ifPresent(s -> {
                token = s;
            });
            return optionalToken;
        }
        return Optional.empty();
    }

    @Override
    public void login(final String user, final char[] password) {
        final AuthenticationAPI api = clientFactory.getClient(AuthenticationAPI.class);
        final ResponseEntityView<TokenEntity> responseEntityView = api.getToken(
                APITokenRequest.builder().user(user).password(password)
                        .expirationDays(expirationDays).build());
        saveCredentials(user, responseEntityView.entity().token());
    }

    private void saveCredentials(final String user, final String token) {
        try {
            final ServiceBean serviceBean = ServiceBean.builder().active(true).name(getServiceKey())
                    .credentials(CredentialsBean.builder().user(user).token(token).build()).build();
            serviceManager.persist(serviceBean);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.user = user;
        this.token = token;
    }

    private Optional<String> loadToken(String serviceKey, String user) {

        final List<ServiceBean> profiles = serviceManager.services();
        final Optional<ServiceBean> optional = profiles.stream()
                .filter(serviceBean -> serviceKey.equals(serviceBean.name())).findFirst();
        if (optional.isPresent()) {
            final ServiceBean bean = optional.get();
            if (user.equals(bean.credentials().user())) {
                return Optional.of(bean.credentials().token());
            }
        }
        return Optional.empty();
    }

    String getServiceKey() {
        final Optional<ServiceBean> selected = serviceManager.selected();
        if(selected.isEmpty()){
           throw new RuntimeException("No dotCMS instance has been activated.");
        }
        return selected.get().name();
    }


    void onStart(@Observes StartupEvent ev) {
        serviceManager.selected().ifPresent(serviceBean -> {
            final CredentialsBean credentials = serviceBean.credentials();
            if(null != credentials){
              this.user = credentials.user();
              this.token = credentials.token();
            }
        });
    }

}
