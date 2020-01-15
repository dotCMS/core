package com.dotcms.security.secret;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple2;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ServiceIntegrationAPIImpl implements ServiceIntegrationAPI {

    private static final String HOST_SECRET_KEY_SEPARATOR = ":";
    private static final String DOT_GLOBAL_SERVICE = "dotCMSGlobalService";
    private static final String INTEGRATIONS_PORTLET_ID = "integrationsPortlet";
    private static final String SERVICE_INTEGRATION_DIR_PATH_KEY = "SERVICE_INTEGRATION_DIR_PATH_KEY";

    private UserAPI userAPI;
    private LayoutAPI layoutAPI;
    private SecretsStore secretsStore;


    private final ObjectMapper jsonMapper = new ObjectMapper()
            //.enable(SerializationFeature.INDENT_OUTPUT)
            .setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

    private final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory())
            //.enable(SerializationFeature.INDENT_OUTPUT)
            .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
            .findAndRegisterModules();

    private String toJsonString(final ServiceSecrets object) throws DotDataException {
        try {
            return jsonMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    private ServiceSecrets readJson(final String json) throws DotDataException {
        try {
            return jsonMapper.readValue(json, ServiceSecrets.class);
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    private ServiceSecrets readJson(final char[] chars) throws DotDataException {
        return readJson(String.valueOf(chars));
    }

    /**
     *
     */
    private String internalKey(final String serviceKey, final Host host) {
        // if Empty ServiceKey is passed everything will be set under systemHostIdentifier:dotCMSGlobalService
        //Otherwise the internal Key will look like:
        // `5e096068-edce-4a7d-afb1-95f30a4fa80e:serviceKeyNameXYZ` where the first portion is the hostId
        final String key = UtilMethods.isSet(serviceKey) ? serviceKey : DOT_GLOBAL_SERVICE;
        final String hostIdentifier =
                (null == host) ? APILocator.systemHost().getIdentifier() : host.getIdentifier();
        return hostIdentifier + HOST_SECRET_KEY_SEPARATOR + key.toLowerCase();
    }

    @VisibleForTesting
    public ServiceIntegrationAPIImpl(final UserAPI userAPI, final LayoutAPI layoutAPI,
            final SecretsStore secretsRepository) {
        this.userAPI = userAPI;
        this.layoutAPI = layoutAPI;
        this.secretsStore = secretsRepository;
    }

    public ServiceIntegrationAPIImpl() {
        this(APILocator.getUserAPI(), APILocator.getLayoutAPI(), SecretsStore.INSTANCE.get());
    }

    private boolean userDoesNotHaveAccess(final User user) throws DotDataException {
        return !userAPI.isCMSAdmin(user) && !layoutAPI
                .doesUserHaveAccessToPortlet(INTEGRATIONS_PORTLET_ID, user);
    }

    @Override
    public List<String> listServiceKeys(final Host host, final User user)
            throws DotDataException, DotSecurityException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to get all service keys performed by user with id `%s` and host `%s` ",
                    user.getUserId(), host.getIdentifier())
            );
        }
        return secretsStore.listKeys().stream().filter(s -> s.startsWith(host.getIdentifier()))
                .map(s -> s.replace(host.getIdentifier() + HOST_SECRET_KEY_SEPARATOR,
                        StringPool.BLANK))
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
            final Host host, final User user) throws DotDataException, DotSecurityException {
            return getSecretsForService(serviceKey, false, host, user);
    }

    @Override
    public Optional<ServiceSecrets> getSecretsForService(final String serviceKey,
            final boolean fallbackOnSystemHost,
            final Host host, final User user) throws DotDataException, DotSecurityException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid secret access attempt on `%s` performed by user with id `%s` and host `%s` ",
                    serviceKey, user.getUserId(), host.getIdentifier()));
        }
        Optional<char[]> optionalChars = secretsStore
                .getValue(internalKey(serviceKey, host));
        if (optionalChars.isPresent()) {
            //We really don't want to cache the json object to protect the secrets.
            //Caching happens on the layers below.
            return Optional.of(readJson(optionalChars.get()));
        } else {
            //fallback
            if (fallbackOnSystemHost) {
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
    public void deleteSecret(final String serviceKey, final Set<String> propOrSecretName,
            final Host host, final User user)
            throws DotDataException, DotSecurityException {
        final Optional<ServiceSecrets> secretsForService = getSecretsForService(serviceKey, host,
                user);
        if (secretsForService.isPresent()) {
            final ServiceSecrets.Builder builder = new ServiceSecrets.Builder();
            final ServiceSecrets serviceSecrets = secretsForService.get();
            final Map<String, Secret> secrets = serviceSecrets.getSecrets();

            secrets.keySet().removeAll(propOrSecretName); //we simply remove the secret by name and then persist the remaining.

            for (final Entry<String, Secret> entry : secrets.entrySet()) {
                builder.withSecret(entry.getKey(), entry.getValue());
            }
            saveSecrets(builder.withServiceKey(serviceKey).build(), host, user);
        } else {
            throw new DotDataException(
                    String.format("Unable to find secret-property named `%s` for service `%s` .",
                            propOrSecretName, serviceKey));
        }
    }

    @Override
    public void saveSecret(final String serviceKey, final Tuple2<String, Secret> keyAndSecret,
            final Host host, final User user)
            throws DotDataException, DotSecurityException {
        final Optional<ServiceSecrets> secretsForService = getSecretsForService(serviceKey, host,
                user);
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
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid secret update attempt on `%s` performed by user with id `%s` and host `%s` ",
                    secrets.getServiceKey(), user.getUserId(), host.getIdentifier()));
        } else {
            final String internalKey = internalKey(secrets.getServiceKey(), host);
            secretsStore.saveValue(internalKey, toJsonString(secrets).toCharArray());
            invalidateCache();
        }
    }

    @Override
    public void deleteSecrets(final String serviceKey, final Host host, final User user)
            throws DotDataException, DotSecurityException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid service delete attempt on `%s` for host `%s` performed by user with id `%s`",
                    serviceKey, host.getIdentifier(), user.getUserId()));
        } else {
            secretsStore.deleteValue(internalKey(serviceKey, host));
            invalidateCache();
        }
    }

    private static final String DESCRIPTORS_CACHE_GROUP = "DESCRIPTORS_CACHE_GROUP";
    private static final String DESCRIPTORS_LIST_KEY = "DESCRIPTORS_LIST_KEY";
    private static final String DESCRIPTORS_MAPPED_BY_SERVICE_KEY = "DESCRIPTORS_MAPPED_BY_SERVICE_KEY";

    @Override
    public List<ServiceDescriptor> getAvailableServiceDescriptors(final User user)
            throws DotDataException, DotSecurityException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to get all available service descriptors performed by user with id `%s`.",
                    user.getUserId()));
        }

        List<ServiceDescriptor> serviceDescriptors = (List<ServiceDescriptor>) CacheLocator
                .getCacheAdministrator().getNoThrow(
                        DESCRIPTORS_LIST_KEY, DESCRIPTORS_CACHE_GROUP);
        if (!UtilMethods.isSet(serviceDescriptors)) {
            try {
                serviceDescriptors = loadServiceDescriptors();
            } catch (IOException e) {
                Logger.error(ServiceIntegrationAPIImpl.class,
                        "An error occurred while loading the service descriptor yml files. ",
                        e);
                throw new DotDataException(e);
            }
            CacheLocator.getCacheAdministrator()
                    .put(DESCRIPTORS_LIST_KEY, serviceDescriptors, DESCRIPTORS_CACHE_GROUP);
        }
        return serviceDescriptors;

    }

    private Map<String, ServiceDescriptor> getServiceDescriptorMap(User user)
            throws DotSecurityException, DotDataException {

        Map<String, ServiceDescriptor> descriptorsByServiceKey = (Map<String, ServiceDescriptor>) CacheLocator
                .getCacheAdministrator().getNoThrow(
                        DESCRIPTORS_MAPPED_BY_SERVICE_KEY, DESCRIPTORS_CACHE_GROUP);
        if (!UtilMethods.isSet(descriptorsByServiceKey)) {

            descriptorsByServiceKey = getAvailableServiceDescriptors(user).stream().collect(
                    Collectors.toMap(ServiceDescriptor::getKey, Function.identity(),
                            (serviceDescriptor, serviceDescriptor2) -> serviceDescriptor));
            CacheLocator.getCacheAdministrator()
                    .put(DESCRIPTORS_MAPPED_BY_SERVICE_KEY, descriptorsByServiceKey,
                            DESCRIPTORS_CACHE_GROUP);

        }
        return descriptorsByServiceKey;
    }

    @Override
    public Optional<ServiceDescriptor> getServiceDescriptor(final String serviceKey,
            final User user)
            throws DotDataException, DotSecurityException {
        final ServiceDescriptor serviceDescriptor = getServiceDescriptorMap(user).get(serviceKey);
        return null == serviceDescriptor ? Optional.empty() : Optional.of(serviceDescriptor);
    }

    @Override
    public void createServiceDescriptor(final InputStream inputStream,
            final User user) throws IOException, DotDataException, DotSecurityException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to create a service descriptors performed by user with id `%s`.",
                    user.getUserId()));
        }

        final String ymlFilesPath = getServiceDescriptorDirectory();
        final File basePath = new File(ymlFilesPath);
        if (!basePath.exists()) {
            basePath.mkdir();
        }
        Logger.debug(ServiceIntegrationAPIImpl.class,
                () -> " ymlFiles are set under:  " + ymlFilesPath);

        // Now validate the incoming file.. see if we're rewriting an existing file or attempting to re-use an already in use service-key.
        final ServiceDescriptor serviceDescriptor = ymlMapper
                .readValue(inputStream, ServiceDescriptor.class);

        if (validateServiceDescriptor(serviceDescriptor, user)) {

            final String serviceKey = serviceDescriptor.getKey();
            final File incomingFile = new File(basePath, String.format("%s.yml", serviceKey));
            if (incomingFile.exists()) {
                throw new DotDataException(
                        String.format("Invalid attempt to override an existing file named '%s'.",
                                incomingFile.getName()));
            }

            ymlMapper.writeValue(incomingFile, serviceDescriptor);

            invalidateCache();
        }
    }

    private void invalidateCache(){
        CacheLocator
                .getCacheAdministrator()
                .remove(DESCRIPTORS_LIST_KEY, DESCRIPTORS_CACHE_GROUP);
        CacheLocator.getCacheAdministrator()
                .remove(DESCRIPTORS_MAPPED_BY_SERVICE_KEY, DESCRIPTORS_CACHE_GROUP);
    }

    private static String getServiceDescriptorDirectory() {
        final Supplier<String> supplier = () -> APILocator.getFileAssetAPI().getRealAssetsRootPath()
                + File.separator + "server" + File.separator;
        final String dirPath = Config
                .getStringProperty(SERVICE_INTEGRATION_DIR_PATH_KEY, supplier.get());
        return Paths.get(dirPath).normalize().toString();
    }

    private Set<String> listAvailableYamlFiles() throws IOException {
        final String ymlFilesPath = getServiceDescriptorDirectory();
        final File basePath = new File(ymlFilesPath);
        if (!basePath.exists()) {
            basePath.mkdir();
        }
        Logger.debug(ServiceIntegrationAPIImpl.class,
                () -> " ymlFiles are set under:  " + ymlFilesPath);
        final Set<String> files = listFiles(ymlFilesPath);
        if (!UtilMethods.isSet(files)) {
            // No files were found a default empty descriptor will be created.
            ymlMapper.writeValue(new File(basePath, "sample-service-descriptor.yml"),
                    emptyDescriptor());
            return listFiles(ymlFilesPath);
        } else {
            return files;
        }
    }

    private Set<String> listFiles(final String dir) throws IOException {
        final Set<String> fileList = new HashSet<>();
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (final Path path : stream) {
                if (!Files.isDirectory(path)) {
                    final String fileName = path.getFileName().toString();
                    if (fileName.endsWith("yaml") || fileName.endsWith("yml")) {
                        fileList.add(path.toString());
                    }
                }
            }
        }
        return fileList;
    }

    private List<ServiceDescriptor> loadServiceDescriptors() throws IOException {
        final ImmutableList.Builder<ServiceDescriptor> builder = new ImmutableList.Builder<>();
        final Set<String> files = listAvailableYamlFiles();
        for (final String file : files) {
            try {
                final ServiceDescriptor serviceDescriptor = ymlMapper
                        .readValue(new File(file), ServiceDescriptor.class);
                builder.add(serviceDescriptor);
            } catch (IOException e) {
                Logger.error(ServiceIntegrationAPIImpl.class,
                        String.format("Error reading yml file `%s`.", file), e);
            }
        }

        return builder.build();
    }

   private boolean validateServiceDescriptor(final ServiceDescriptor serviceDescriptor, final User user)
           throws DotDataException, DotSecurityException {
       if(UtilMethods.isNotSet(serviceDescriptor.getKey())){
          throw new DotDataException("The required field `serviceKey` isn't set on the incoming file.");
       }

       if (getServiceDescriptorMap(user).containsKey(serviceDescriptor.getKey())) {
           throw new DotDataException(
                   String.format("There's a service already registered under key `%s`.",
                           serviceDescriptor.getKey()));
       }

       if(UtilMethods.isNotSet(serviceDescriptor.getName())){
           throw new DotDataException("The required field `name` isn't set on the incoming file.");
       }

       if(UtilMethods.isNotSet(serviceDescriptor.getDescription())){
           throw new DotDataException("The required field `description` isn't set on the incoming file.");
       }

       if(UtilMethods.isNotSet(serviceDescriptor.getIconUrl())){
           throw new DotDataException("The required field `iconUrl` isn't set on the incoming file.");
       }

       if(!UtilMethods.isSet(serviceDescriptor.getParams())){
           throw new DotDataException("The required field `params` isn't set on the incoming file.");
       }

       return true;

   }

    private ServiceDescriptor emptyDescriptor() {
        final ServiceDescriptor serviceDescriptor = new ServiceDescriptor("sampleDescriptor",
                "Sample Descriptor.",
                "This is an empty descriptor created by the system to show you the expected structure.",
                "/black_18dp.png", true);
        serviceDescriptor.addParam("stringParam", "This is a string.", false, Type.STRING, "This is string param",
                "Test string.");
        serviceDescriptor.addParam("boolParam", "true", false, Type.BOOL, "This is a Bool Param",
                "Test Bool.");
        serviceDescriptor.addParam("myFile", "", false, Type.FILE, "", "");
        return serviceDescriptor;
    }
}
