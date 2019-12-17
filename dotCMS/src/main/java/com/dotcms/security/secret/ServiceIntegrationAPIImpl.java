package com.dotcms.security.secret;

import com.dotmarketing.beans.Host;
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
import com.liferay.util.StringPool;
import io.vavr.Tuple2;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServiceIntegrationAPIImpl implements ServiceIntegrationAPI {
    private static final String HOST_SECRET_KEY_SEPARATOR = ":";
    private static final String DOT_GLOBAL_SERVICE = "dotCMSGlobalService";
    private static final String INTEGRATIONS_PORTLET_ID = "integrationsPortlet";

    private UserAPI userAPI;
    private LayoutAPI layoutAPI;
    private SecretsStore secretsStore;

    private final ObjectMapper objectMapper = new ObjectMapper()
            //.enable(SerializationFeature.INDENT_OUTPUT)
            .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    ;

    private String toJsonString(final ServiceSecrets object) throws DotDataException {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    private ServiceSecrets readJson(final String json) throws DotDataException {
        try {
            return objectMapper.readValue(json, ServiceSecrets.class);
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    private ServiceSecrets readJson(final char [] chars) throws DotDataException {
        return readJson(String.valueOf(chars));
    }

    /**
     *
     * @param serviceKey
     * @param host
     * @return
     */
    private String internalKey(final String serviceKey, final Host host){
        // if Empty ServiceKey is passed everything will be set under systemHostIdentifier:dotCMSGlobalService
        //Otherwise the internal Key will look like:
        // `5e096068-edce-4a7d-afb1-95f30a4fa80e:serviceKeyNameXYZ` where the first portion is the hostId
        final String key = UtilMethods.isSet(serviceKey) ? serviceKey : DOT_GLOBAL_SERVICE;
        final String hostIdentifier = (null == host) ? APILocator.systemHost().getIdentifier() : host.getIdentifier();
        return hostIdentifier + HOST_SECRET_KEY_SEPARATOR + key.toLowerCase();
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
    public List<String> listServiceKeys(final Host host) {
        return secretsStore.listKeys().stream().filter(s -> s.startsWith(host.getIdentifier()))
               .map(s -> s.replace(host.getIdentifier() + HOST_SECRET_KEY_SEPARATOR, StringPool.BLANK))
               .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<String>> serviceKeysByHost() {
        return secretsStore.listKeys().stream()
                .map(s -> s.split(HOST_SECRET_KEY_SEPARATOR))
                .collect(Collectors.groupingBy(strings -> strings[0],
                        Collectors.mapping(strings -> strings[1], Collectors.toList())));
    }

    @Override
    public Optional<ServiceSecrets> getSecretsForService(final String serviceKey,
            final Host host, final User user) throws DotDataException {
        if (userAPI.isCMSAdmin(user) || layoutAPI
                .doesUserHaveAccessToPortlet(INTEGRATIONS_PORTLET_ID, user)) {
             Optional<char[]> optionalChars = secretsStore
                    .getValue(internalKey(serviceKey, host));
            if (optionalChars.isPresent()) {
                    //We really don't want to cache the json object to protect the secrets.
                    //Caching happens on the layers below.
                    return Optional.of(readJson(optionalChars.get()));
            } else {
            //fallback
                optionalChars = secretsStore
                        .getValue(internalKey(serviceKey, APILocator.systemHost()));
                if (optionalChars.isPresent()) {
                    return Optional.of(readJson(optionalChars.get()));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void deleteSecret(final String serviceKey, final String propOrSecretName, final Host host, final User user)
            throws DotDataException, DotSecurityException {
        final Optional<ServiceSecrets> secretsForService = getSecretsForService(serviceKey, host, user);
        if (secretsForService.isPresent()) {
            final ServiceSecrets.Builder builder = new ServiceSecrets.Builder();
            final ServiceSecrets serviceSecrets = secretsForService.get();
            final Map<String, Secret> secrets = serviceSecrets.getSecrets();
            secrets.remove(propOrSecretName); //we simply remove the secret by name and then persist the remaining.
            for (final Entry<String, Secret> entry : secrets.entrySet()) {
                builder.withSecret(entry.getKey(), entry.getValue());
            }
            saveSecrets(builder.withServiceKey(serviceKey).build(), host, user);
        } else {
           throw new DotDataException(String.format("Unable to find secret-property named `%s` for service `%s` .",propOrSecretName, serviceKey));
        }
    }

    @Override
    public void saveSecret(final String serviceKey, final Tuple2<String, Secret> keyAndSecret,
            final Host host, final User user)
            throws DotDataException, DotSecurityException {
        final Optional<ServiceSecrets> secretsForService = getSecretsForService(serviceKey, host, user);
        if (secretsForService.isPresent()) {
        //The secret already exists and wee need to update one specific entry they rest should be copy as they are
            final ServiceSecrets serviceSecrets = secretsForService.get();
            final Map<String, Secret> secrets = serviceSecrets.getSecrets();
            final ServiceSecrets.Builder builder = new ServiceSecrets.Builder();

            for (final Entry<String, Secret> entry : secrets.entrySet()) {
                //Replace all existing secret/property that must be copied as it is.
                builder.withSecret(entry.getKey(), entry.getValue());
            }
            //finally override or add the new value.
            builder.withSecret(keyAndSecret._1, keyAndSecret._2);

            saveSecrets(builder.withServiceKey(serviceKey).build(), host, user);
        } else {
        //The secret is brand new . We need to create a whole new one.
            final ServiceSecrets.Builder builder = new ServiceSecrets.Builder();
            builder.withSecret(keyAndSecret._1, keyAndSecret._2);
            saveSecrets(builder.withServiceKey(serviceKey).build(), host, user);
        }
    }

    @Override
    public void saveSecrets(final ServiceSecrets secrets, final Host host, final User user)
            throws DotDataException, DotSecurityException {
        if (userAPI.isCMSAdmin(user) || layoutAPI
                .doesUserHaveAccessToPortlet(INTEGRATIONS_PORTLET_ID, user)) {
            final String internalKey = internalKey(secrets.getServiceKey(), host);
            secretsStore.saveValue(internalKey, toJsonString(secrets).toCharArray());
        } else {
            throw new DotSecurityException(String.format(
                    "Invalid service registration attempt `%s` by user with id `%s` and host `%s` ",
                    secrets.getServiceKey(), user.getUserId(), host.getIdentifier()));
        }
    }

    @Override
    public void deleteSecrets(final String serviceKey, final Host host, final User user)
            throws DotDataException, DotSecurityException {
        if(userAPI.isCMSAdmin(user) || layoutAPI.doesUserHaveAccessToPortlet(INTEGRATIONS_PORTLET_ID, user) ) {
            secretsStore.deleteValue(internalKey(serviceKey, host));
        } else {
            throw new DotSecurityException(String.format("Invalid service delete attempt performed on `%s` for host `%s` by user with id `%s`", serviceKey, host.getIdentifier() ,user.getUserId()));
        }
    }

    @Override
    public List<?> getAvailableServiceDescriptors(final Host host, final User user) {
        throw new UnsupportedOperationException("Will read yml file for service definitions.");
    }
}
