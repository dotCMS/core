package com.dotcms.security.secret;

import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This API serves as the bridge between the secrets safe repository
 * and the structured of the service defined via
 *
 */
public class ServiceIntegrationAPIImpl implements ServiceIntegrationAPI {

    static final String INTEGRATIONS_PORTLET_ID = "integration-services";
    private static final String HOST_SECRET_KEY_SEPARATOR = ":";
    private static final String DOT_GLOBAL_SERVICE = "dotCMSGlobalService";
    private static final String SERVICE_INTEGRATION_DIR_PATH_KEY = "SERVICE_INTEGRATION_DIR_PATH_KEY";

    private final UserAPI userAPI;
    private final LayoutAPI layoutAPI;
    private final HostAPI hostAPI;
    private final SecretsStore secretsStore;

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
     * One single method takes care of building the internal-key
     */
    private String internalKey(final String serviceKey, final Host host) {
        return internalKey(serviceKey, host == null ? null : host.getIdentifier());
    }

    /**
     *
      * @param serviceKey
     * @param hostIdentifier
     * @return
     */
    private String internalKey(final String serviceKey, final String hostIdentifier) {
        // if Empty ServiceKey is passed everything will be set under systemHostIdentifier:dotCMSGlobalService
        //Otherwise the internal Key will look like:
        // `5e096068-edce-4a7d-afb1-95f30a4fa80e:serviceKeyNameXYZ` where the first portion is the hostId
        final String key = UtilMethods.isSet(serviceKey) ? serviceKey : DOT_GLOBAL_SERVICE;
        final String identifier =
                (null == hostIdentifier) ? APILocator.systemHost().getIdentifier() : hostIdentifier;
        return identifier + HOST_SECRET_KEY_SEPARATOR + key.toLowerCase();
    }

    @VisibleForTesting
    public ServiceIntegrationAPIImpl(final UserAPI userAPI, final LayoutAPI layoutAPI, final HostAPI hostAPI,
            final SecretsStore secretsRepository) {
        this.userAPI = userAPI;
        this.layoutAPI = layoutAPI;
        this.hostAPI = hostAPI;
        this.secretsStore = secretsRepository;
    }

    public ServiceIntegrationAPIImpl() {
        this(APILocator.getUserAPI(), APILocator.getLayoutAPI(), APILocator.getHostAPI(), SecretsStore.INSTANCE.get());
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
    public Map<String, Set<String>> serviceKeysByHost() {
        return secretsStore.listKeys().stream()
                .filter(s -> s.contains(HOST_SECRET_KEY_SEPARATOR))
                .map(s -> s.split(HOST_SECRET_KEY_SEPARATOR))
                .filter(strings -> strings.length == 2)
                .collect(Collectors.groupingBy(strings -> strings[0],
                        Collectors.mapping(strings -> strings[1], Collectors.toSet())));
    }

    @Override
    public Optional<ServiceSecrets> getSecrets(final String serviceKey,
            final Host host, final User user) throws DotDataException, DotSecurityException {
            return getSecrets(serviceKey, false, host, user);
    }

    @Override
    public Optional<ServiceSecrets> getSecrets(final String serviceKey,
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

    /**
     * In s similar fashion as `getSecrets` does this method hits the secrets repo but it does not deal or convert the entry into a json object
     * This only tells you if the service-key exists for a specific host.
     * @param serviceKey
     * @param hostIdentifier
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private boolean hasAnySecrets(final String serviceKey,
            final String hostIdentifier, final User user) throws DotDataException, DotSecurityException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid secret access attempt on `%s` performed by user with id `%s` and host `%s` ",
                    serviceKey, user.getUserId(), hostIdentifier));
        }
        return secretsStore.containsKey(internalKey(serviceKey, hostIdentifier));
    }

    /**
     * {@inheritDoc}
     * @param user
     * @return
     */
    public Set<String> filterSitesForServiceKey(final String serviceKey, final Collection<String> siteIdentifiers, final User user){
        return siteIdentifiers.stream().filter(id -> {
            try {
                return hasAnySecrets(serviceKey, id, user);
            } catch (DotDataException | DotSecurityException e) {
                Logger.error(ServiceIntegrationAPIImpl.class,
                        String.format("Error getting secret from `%s` ", serviceKey), e);
            }
            return false;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * {@inheritDoc}
     * @param user
     * @return
     */
    public List<Host> getSitesWithIntegrations(final User user) {
        final Set<String> hostIds = serviceKeysByHost().keySet();
        return hostIds.stream().map(hostId -> {
            try {
                return hostAPI.find(hostId, user, false);
            } catch (DotDataException | DotSecurityException e) {
                Logger.warn(ServiceIntegrationAPIImpl.class,
                        String.format("Unable to lookup site for the given id `%s`. The secret config entry is probably no longer valid.",hostId), e);
            }
            return null;
        }).filter(Objects::nonNull).collect(CollectionsUtils.toImmutableList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSecret(final String serviceKey, final Set<String> propOrSecretName,
            final Host host, final User user)
            throws DotDataException, DotSecurityException {
        final Optional<ServiceSecrets> secretsForService = getSecrets(serviceKey, host, user);
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

        final Optional<ServiceSecrets> secretsForService = getSecrets(serviceKey, host, user);
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
        }
    }

    private static final String DESCRIPTORS_CACHE_GROUP = "DESCRIPTORS_CACHE_GROUP";
    private static final String DESCRIPTORS_LIST_KEY = "DESCRIPTORS_LIST_KEY";
    private static final String DESCRIPTORS_MAPPED_BY_SERVICE_KEY = "DESCRIPTORS_MAPPED_BY_SERVICE_KEY";

    @Override
    public List<ServiceDescriptor> getServiceDescriptors(User user)
            throws DotDataException, DotSecurityException {

        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to get all available service descriptors performed by user with id `%s`.",
                    user.getUserId()));
        }

        return getServiceDescriptorsMeta().stream()
                .map(ServiceDescriptorMeta::getServiceDescriptor)
                .collect(Collectors.toList());
    }

    private List<ServiceDescriptorMeta> getServiceDescriptorsMeta()
            throws DotDataException {
            List<ServiceDescriptorMeta> serviceDescriptors = (List<ServiceDescriptorMeta>) CacheLocator
                    .getCacheAdministrator().getNoThrow(
                            DESCRIPTORS_LIST_KEY, DESCRIPTORS_CACHE_GROUP);
            if (!UtilMethods.isSet(serviceDescriptors)) {
                synchronized (ServiceIntegrationAPIImpl.class) {
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
            }
            return serviceDescriptors;
    }

    private Map<String, ServiceDescriptorMeta> getServiceDescriptorMap()
            throws DotDataException {

        Map<String, ServiceDescriptorMeta> descriptorsByServiceKey = (Map<String, ServiceDescriptorMeta>) CacheLocator
                .getCacheAdministrator().getNoThrow(
                        DESCRIPTORS_MAPPED_BY_SERVICE_KEY, DESCRIPTORS_CACHE_GROUP);
        if (!UtilMethods.isSet(descriptorsByServiceKey)) {
            synchronized (ServiceIntegrationAPIImpl.class) {
                descriptorsByServiceKey = getServiceDescriptorsMeta().stream().collect(
                        Collectors.toMap(serviceDescriptorMeta -> serviceDescriptorMeta
                                        .getServiceDescriptor().getKey().toLowerCase(), Function.identity(),
                                (serviceDescriptor, serviceDescriptor2) -> serviceDescriptor));

                CacheLocator.getCacheAdministrator()
                        .put(DESCRIPTORS_MAPPED_BY_SERVICE_KEY, descriptorsByServiceKey,
                                DESCRIPTORS_CACHE_GROUP);
            }
        }
        return descriptorsByServiceKey;
    }

    @Override
    public Optional<ServiceDescriptor> getServiceDescriptor(final String serviceKey,
            final User user)
            throws DotDataException, DotSecurityException {

        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to get all available service descriptors performed by user with id `%s`.",
                    user.getUserId()));
        }

        final String serviceKeyLC = serviceKey.toLowerCase();
        final ServiceDescriptorMeta serviceDescriptorMeta = getServiceDescriptorMap()
                .get(serviceKeyLC);
        return null == serviceDescriptorMeta ? Optional.empty()
                : Optional.of(serviceDescriptorMeta.getServiceDescriptor());
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

        if (validateServiceDescriptor(serviceDescriptor)
                && validateServiceDescriptorUniqueName(serviceDescriptor)) {

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

    @Override
    public void removeServiceIntegration(final String serviceKey, final User user,
            final boolean removeDescriptor)
            throws DotSecurityException, DotDataException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to delete a service descriptors performed by user with id `%s`.",
                    user.getUserId()));
        }
        final String serviceKeyLC = serviceKey.toLowerCase();
        final ServiceDescriptorMeta serviceDescriptorMeta = getServiceDescriptorMap().get(serviceKeyLC);
        if (null == serviceDescriptorMeta) {
            throw new DoesNotExistException(String.format("The requested descriptor `%s` does not exist.",serviceKey));
        }else{
            // if we succeed trying to find the descriptor.
            // Lets get all the service-keys and organized by host-id
            final Map<String, Set<String>> serviceKeysByHost = serviceKeysByHost();
            for (final Entry<String, Set<String>> entry : serviceKeysByHost.entrySet()) {
                final String hostId = entry.getKey();
                final Set<String> serviceKeys = entry.getValue();
                //Now we need to verify the service-key has a configuration for this host.
                if(serviceKeys.contains(serviceKeyLC)){
                    //And if it does we attempt to delete all the secrets.
                    final Host host = hostAPI.find(hostId, user, false);
                    deleteSecrets(serviceKey, host, user);
                }
            }
            if(removeDescriptor) {
                removeDescriptor(serviceDescriptorMeta);
            }
            invalidateCache();
        }
    }

    /**
     * Removes the yml file itself.
     * @param serviceDescriptorMeta
     * @throws DotDataException
     */
    private void removeDescriptor(final ServiceDescriptorMeta serviceDescriptorMeta) throws DotDataException{
        final String fileName = serviceDescriptorMeta.getFileName();
        //Now we need to remove the file it self.
        final String ymlFilesPath = getServiceDescriptorDirectory();
        final Path file = Paths.get(ymlFilesPath + File.separator + fileName).normalize();
        if (!file.toFile().exists()) {
            throw new DotDataException(
                    String.format(" File with path `%s` does not exist. ", file));
        }
        try {
            Logger.warn(ServiceIntegrationAPIImpl.class, () -> String
                    .format(" Failed attempt to delete file with path `%s` ", file));
            Files.delete(file);
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    private synchronized void invalidateCache(){
        CacheLocator
                .getCacheAdministrator()
                .remove(DESCRIPTORS_LIST_KEY, DESCRIPTORS_CACHE_GROUP);
        CacheLocator.getCacheAdministrator()
                .remove(DESCRIPTORS_MAPPED_BY_SERVICE_KEY, DESCRIPTORS_CACHE_GROUP);
    }

    private static String getServiceDescriptorDirectory() {
        final Supplier<String> supplier = () -> APILocator.getFileAssetAPI().getRealAssetsRootPath()
                + File.separator + "services" + File.separator;
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
            // ymlMapper.writeValue(new File(basePath, "sample-service-descriptor.yml"), emptyDescriptor());
            // return listFiles(ymlFilesPath);
           return Collections.emptySet();
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

    private List<ServiceDescriptorMeta> loadServiceDescriptors()
            throws IOException, DotDataException {
        final Set<String> loadedServiceKeys = new HashSet<>();
        final ImmutableList.Builder<ServiceDescriptorMeta> builder = new ImmutableList.Builder<>();
        final Set<String> fileNames = listAvailableYamlFiles();
        for (final String fileName : fileNames) {
            try {
                final File file = new File(fileName);
                final ServiceDescriptor serviceDescriptor = ymlMapper
                        .readValue(file, ServiceDescriptor.class);
                if (validateServiceDescriptor(serviceDescriptor)) {
                    if (loadedServiceKeys.contains(serviceDescriptor.getKey())) {
                        throw new DotDataException(
                                String.format(
                                        "There's a service already registered under key `%s`.",
                                        serviceDescriptor.getKey())
                                );
                    }
                    builder.add(new ServiceDescriptorMeta(serviceDescriptor, file.getName()));
                    loadedServiceKeys.add(serviceDescriptor.getKey());
                }
            } catch (IOException e) {
                Logger.error(ServiceIntegrationAPIImpl.class,
                        String.format("Error reading yml file `%s`.", fileName), e);
            }
        }

        return builder.build();
    }

   private boolean validateServiceDescriptor(final ServiceDescriptor serviceDescriptor)
           throws DotDataException {
       if(UtilMethods.isNotSet(serviceDescriptor.getKey())){
          throw new DotDataException("The required field `key` isn't set on the incoming file.");
       }

       if(serviceDescriptor.getKey().length() > 100){
           throw new DotDataException("The required field `key` is too large.");
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

    private boolean validateServiceDescriptorUniqueName(final ServiceDescriptor serviceDescriptor)
            throws DotDataException {

        if (getServiceDescriptorMap().containsKey(serviceDescriptor.getKey())) {
            throw new DotDataException(
                    String.format("There's a service already registered under key `%s`.",
                            serviceDescriptor.getKey()));
        }
        return true;
    }

    /*
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
    }*/

}
