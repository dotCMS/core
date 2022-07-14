package com.dotcms.api;


import com.dotcms.api.client.CredentialsManager;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.TokenEntity;
import com.dotcms.model.config.CredentialsBean;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.arc.DefaultBean;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;


@DefaultBean
@ApplicationScoped
public class DefaultAuthenticationContextImpl implements AuthenticationContext {

    private static final Logger logger = Logger.getLogger(AuthSecurityContext.class);

    @Inject
    CredentialsManager credentialsManager;

    @Inject
    RestClientFactory clientFactory;

    private String user;

    private String token;

    @ConfigProperty(name = "expirationDays", defaultValue = "10")
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
            final Optional<String> optionalToken = loadToken(getServiceKey(userString), userString);
            optionalToken.ifPresent(s -> {
                token = s;
            });
            return optionalToken;
        }
        return Optional.empty();
    }

    @Override
    public void login(final String user, final String password) {

        final AuthenticationAPI api = clientFactory.getClient(AuthenticationAPI.class);
        final ResponseEntityView<TokenEntity> responseEntityView = api.getToken(
                APITokenRequest.builder().user(user).password(password)
                        .expirationDays(expirationDays).build());
        saveCredentials(user, responseEntityView.entity().token());
    }

    private void saveCredentials(final String user, final String token) {
        try {
            credentialsManager.persist(
                    getServiceKey(user),
                    CredentialsBean.builder().user(user).token(token).build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.user = user;
        this.token = token;
    }

    private Optional<String> loadToken(String serviceKey, String user) {
        final Map<String,ServiceBean> profiles = credentialsManager.services();
        final ServiceBean bean = profiles.get(serviceKey);
        if(null != bean){
            if(user.equals(bean.credentials().user())){
               return Optional.of(bean.credentials().token());
            }
        }
        return Optional.empty();
    }

    String getServiceKey(String user) {
        return user + "@" + clientFactory.currentSelectedProfile();
    }

}
