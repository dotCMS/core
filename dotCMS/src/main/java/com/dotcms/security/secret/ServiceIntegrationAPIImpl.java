package com.dotcms.security.secret;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ServiceIntegrationAPIImpl implements ServiceIntegrationAPI {

    private static final String DOT_GLOBAL_SERVICE = "dotCMSGlobalService";
    private static final String INTEGRATIONS_PORTLET_ID = "integrationsPortlet";

    private UserAPI userAPI;
    private LayoutAPI layoutAPI;
    private SecretsStore secretsStore;

    private ObjectMapper objectMapper = new ObjectMapper()
            //.enable(SerializationFeature.INDENT_OUTPUT)
            .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    ;

    private String toJsonString(final ServiceIntegrationBean object) throws DotDataException {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    private ServiceIntegrationBean readJson(final String json) throws DotDataException {
        try {
            return objectMapper.readValue(json, ServiceIntegrationBean.class);
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    private ServiceIntegrationBean readJson(final char [] chars) throws DotDataException {
        return readJson(String.valueOf(chars));
    }

    @VisibleForTesting
    public ServiceIntegrationAPIImpl(final UserAPI userAPI, final LayoutAPI layoutAPI, final SecretsStore secretsRepository) {
        this.userAPI = userAPI;
        this.layoutAPI = layoutAPI;
        this.secretsStore = secretsRepository;
    }

    public ServiceIntegrationAPIImpl() {
        this(APILocator.getUserAPI(), APILocator.getLayoutAPI(), SecretsStore.INSTANCE.get());
    }

    @Override
    public Optional<ServiceIntegrationBean> getIntegrationForService(final String serviceKey,
            final User user) throws DotDataException {

        final Optional<char[]> optionalChars = secretsStore.getValue(serviceKey);
        if (optionalChars.isPresent()) {
            try {
                // We really don't want to cache the json object to protect the secrets.
                return Optional.of(readJson(optionalChars.get()));
            } catch (DotDataException e) {
                throw new DotDataException(String.format(
                        "Error converting to json the integration data for service identified by key `%s`.",
                        serviceKey), e);
            }
        }
        return Optional.empty();
    }


    @Override
    public void registerServiceIntegration(final ServiceIntegrationBean bean, final User user)
            throws DotDataException, DotSecurityException {
        String serviceKey = bean.getServiceKey();
        if(userAPI.isCMSAdmin(user) || layoutAPI.doesUserHaveAccessToPortlet(INTEGRATIONS_PORTLET_ID, user) ){
            serviceKey = UtilMethods.isSet(serviceKey) ? serviceKey : DOT_GLOBAL_SERVICE;
            secretsStore.saveValue(serviceKey, toJsonString(bean).toCharArray());
        } else {
            throw new DotSecurityException(String.format("Invalid service registration attempt `%s` by user with id `%s`", serviceKey, user.getUserId()));
        }
    }

    @Override
    public void deleteServiceIntegration(final String serviceKey, final User user)
            throws DotDataException, DotSecurityException {
        if(userAPI.isCMSAdmin(user) || layoutAPI.doesUserHaveAccessToPortlet(INTEGRATIONS_PORTLET_ID, user) ) {
            secretsStore.deleteValue(serviceKey);
        } else {
            throw new DotSecurityException(String.format("Invalid service delete attempt `%s` by user with id `%s`", serviceKey, user.getUserId()));
        }
    }

    @Override
    public List<ServiceIntegrationBean> getAvailableServiceDescriptors(final User user) {
        throw new UnsupportedOperationException("Will read yml file for service definitions.");
    }
}
