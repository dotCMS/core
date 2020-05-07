package com.dotcms.security.apps;

import static com.dotcms.security.apps.AppsUtil.readJson;
import static com.dotcms.security.apps.AppsUtil.toJsonAsChars;
import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.emptyMap;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This API serves as the bridge between the secrets safe repository
 * and the structure of the APP defined via YML file descriptor.
 */
public class AppsAPIImpl implements AppsAPI {

    static final String APPS_PORTLET_ID = "apps";
    private static final String HOST_SECRET_KEY_SEPARATOR = ":";
    private static final String DOT_GLOBAL_SERVICE = "dotCMSGlobalService";
    private static final String SERVER_DIR_NAME = "server";
    private static final String APPS_DIR_NAME = "apps";
    private static final String APPS_DIR_PATH_KEY = "APPS_DIR_PATH_KEY";
    static final int DESCRIPTOR_KEY_MAX_LENGTH = 60;
    static final int DESCRIPTOR_NAME_MAX_LENGTH = 60;

    private final UserAPI userAPI;
    private final LayoutAPI layoutAPI;
    private final HostAPI hostAPI;
    private final SecretsStore secretsStore;
    private final AppsCache appsCache;

    private final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory())
            //.enable(SerializationFeature.INDENT_OUTPUT)
            .findAndRegisterModules();

    @VisibleForTesting
    public AppsAPIImpl(final UserAPI userAPI, final LayoutAPI layoutAPI, final HostAPI hostAPI,
            final SecretsStore secretsRepository, final AppsCache appsCache) {
        this.userAPI = userAPI;
        this.layoutAPI = layoutAPI;
        this.hostAPI = hostAPI;
        this.secretsStore = secretsRepository;
        this.appsCache = appsCache;
    }

    public AppsAPIImpl() {
        this(APILocator.getUserAPI(), APILocator.getLayoutAPI(), APILocator.getHostAPI(), SecretsStore.INSTANCE.get(), CacheLocator.getAppsCache());
    }

    /**
     * One single method takes care of building the internal-key
     */
    private String internalKey(final String serviceKey, final Host host) {
        return internalKey(serviceKey, host == null ? null : host.getIdentifier());
    }

    /**
     * Given a service key and an identifier this builds an internal key composed by the two values concatenated
     * And lowercased.
     * Like `5e096068-edce-4a7d-afb1-95f30a4fa80e:serviceKeyNameXYZ`
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
        return (identifier + HOST_SECRET_KEY_SEPARATOR + key).toLowerCase();
    }

    private boolean userDoesNotHaveAccess(final User user) throws DotDataException {
        return !userAPI.isCMSAdmin(user) && !layoutAPI
                .doesUserHaveAccessToPortlet(APPS_PORTLET_ID, user);
    }

    @Override
    public List<String> listAppKeys(final Host host, final User user)
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

    /**
     * {@inheritDoc}
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Override
    public Map<String, Set<String>> appKeysByHost() throws DotSecurityException, DotDataException{
      return appKeysByHost(true);
    }

    /**
     * This private version basically adds the possibility to filter sites that exist in our db
     * This becomes handy when people has secrets associated to a site and then for some reason decides to remove the site.
     * @param filterNonExisting
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Map<String, Set<String>> appKeysByHost(final boolean filterNonExisting)
            throws DotSecurityException, DotDataException {
        Stream<String[]> stream = secretsStore.listKeys().stream()
                .filter(s -> s.contains(HOST_SECRET_KEY_SEPARATOR))
                .map(s -> s.split(HOST_SECRET_KEY_SEPARATOR))
                .filter(strings -> strings.length == 2);
        if (filterNonExisting) {
            final Set<String> validSites = hostAPI.findAll(APILocator.systemUser(), false).stream()
                    .map(Contentlet::getIdentifier).map(String::toLowerCase).collect(Collectors.toSet());
            stream = stream.filter(strings -> validSites.contains(strings[0]));
        }
        return stream.collect(Collectors.groupingBy(strings -> strings[0],
                Collectors.mapping(strings -> strings[1], Collectors.toSet())));
    }

    @Override
    public Optional<AppSecrets> getSecrets(final String key,
            final Host host, final User user) throws DotDataException, DotSecurityException {
            return getSecrets(key, false, host, user);
    }

    @Override
    public Optional<AppSecrets> getSecrets(final String key,
            final boolean fallbackOnSystemHost,
            final Host host, final User user) throws DotDataException, DotSecurityException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid secret access attempt on `%s` performed by user with id `%s` and host `%s` ",
                    key, user.getUserId(), host.getIdentifier()));
        }
        Optional<char[]> optionalChars = secretsStore
                .getValue(internalKey(key, host));
        if (optionalChars.isPresent()) {
            //We really don't want to cache the json object to protect the secrets.
            //Caching happens on the layers below.
            return Optional.of(readJson(optionalChars.get()));
        } else {
            //fallback
            if (fallbackOnSystemHost) {
                optionalChars = secretsStore
                        .getValue(internalKey(key, APILocator.systemHost()));
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
    public Set<String> filterSitesForAppKey(final String key, final Collection<String> siteIdentifiers, final User user){
        return siteIdentifiers.stream().filter(id -> {
            try {
                return hasAnySecrets(key, id, user);
            } catch (DotDataException | DotSecurityException e) {
                Logger.error(AppsAPIImpl.class,
                        String.format("Error getting secret from `%s` ", key), e);
            }
            return false;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSecret(final String key, final Set<String> propOrSecretName,
            final Host host, final User user)
            throws DotDataException, DotSecurityException {
        final Optional<AppSecrets> secretsForService = getSecrets(key, host, user);
        if (secretsForService.isPresent()) {
            final AppSecrets.Builder builder = new AppSecrets.Builder();
            final AppSecrets serviceSecrets = secretsForService.get();
            final Map<String, Secret> secrets = serviceSecrets.getSecrets();

            secrets.keySet().removeAll(propOrSecretName); //we simply remove the secret by name and then persist the remaining.

            for (final Entry<String, Secret> entry : secrets.entrySet()) {
                builder.withSecret(entry.getKey(), entry.getValue());
            }
            saveSecrets(builder.withKey(key).build(), host, user);
        } else {
            throw new DotDataException(
                    String.format("Unable to find secret-property named `%s` for service `%s` .",
                            propOrSecretName, key));
        }
    }

    @Override
    public void saveSecret(final String key, final Tuple2<String, Secret> keyAndSecret,
            final Host host, final User user)
            throws DotDataException, DotSecurityException {

        final Optional<AppSecrets> secretsForService = getSecrets(key, host, user);
        if (secretsForService.isPresent()) {
            //The secret already exists and wee need to update one specific entry they rest should be copy as they are
            final AppSecrets serviceSecrets = secretsForService.get();
            final Map<String, Secret> secrets = serviceSecrets.getSecrets();
            final AppSecrets.Builder builder = new AppSecrets.Builder();

            for (final Entry<String, Secret> entry : secrets.entrySet()) {
                //Replace all existing secret/property that must be copied as it is.
                builder.withSecret(entry.getKey(), entry.getValue());
            }
            //finally override or add the new value.
            builder.withSecret(keyAndSecret._1, keyAndSecret._2);

            saveSecrets(builder.withKey(key).build(), host, user);
        } else {
            //The secret is brand new . We need to create a whole new one.
            final AppSecrets.Builder builder = new AppSecrets.Builder();
            builder.withSecret(keyAndSecret._1, keyAndSecret._2);
            saveSecrets(builder.withKey(key).build(), host, user);
        }
    }

    @Override
    public void saveSecrets(final AppSecrets secrets, final Host host, final User user)
            throws DotDataException, DotSecurityException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid secret update attempt on `%s` performed by user with id `%s` and host `%s` ",
                    secrets.getKey(), user.getUserId(), host.getIdentifier()));
        } else {
            final String internalKey = internalKey(secrets.getKey(), host);
            if (secrets.getSecrets().isEmpty()) {
                //if everything has been removed from the json entry we need to kick it off from cache.
                secretsStore.deleteValue(internalKey);
            } else {
                char[] chars = null;
                try {
                    chars = toJsonAsChars(secrets);
                    secretsStore.saveValue(internalKey, chars);
                } finally {
                    secrets.destroy();
                    if (null != chars) {
                        Arrays.fill(chars, (char) 0);
                    }
                }
            }
        }
    }

    @Override
    public void deleteSecrets(final String key, final Host host, final User user)
            throws DotDataException, DotSecurityException {
        deleteSecrets(key, host.getIdentifier(), user);
    }


    private void deleteSecrets(final String key, final String siteIdentifier, final User user)
            throws DotDataException, DotSecurityException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid service delete attempt on `%s` for host `%s` performed by user with id `%s`",
                    key, siteIdentifier, user.getUserId()));
        } else {
            secretsStore.deleteValue(internalKey(key, siteIdentifier));
        }
    }

    @Override
    public List<AppDescriptor> getAppDescriptors(User user)
            throws DotDataException, DotSecurityException {

        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to get all available service descriptors performed by user with id `%s`.",
                    user.getUserId()));
        }

        return getAppDescriptorsMeta().stream()
                .map(AppDescriptorMeta::getAppDescriptor)
                .collect(Collectors.toList());
    }

    private List<AppDescriptorMeta> getAppDescriptorsMeta() {

        synchronized (AppsAPIImpl.class) {
            return appsCache.getAppDescriptorsMeta(() -> {
                try {
                    return loadAppDescriptors();
                } catch (IOException | DotDataException e) {
                    Logger.error(AppsAPIImpl.class,
                            "An error occurred while loading the service descriptor yml files. ",
                            e);
                    throw new DotRuntimeException(e);
                }
            });
        }
    }

    private Map<String, AppDescriptorMeta> getAppDescriptorMap(){
       return appsCache.getAppDescriptorsMap(this::getAppDescriptorsMeta);
    }

    @Override
    public Optional<AppDescriptor> getAppDescriptor(final String key,
            final User user)
            throws DotDataException, DotSecurityException {

        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to get all available service descriptors performed by user with id `%s`.",
                    user.getUserId()));
        }

        final String appKeyLC = key.toLowerCase();
        final AppDescriptorMeta appDescriptorMeta = getAppDescriptorMap()
                .get(appKeyLC);
        return null == appDescriptorMeta ? Optional.empty()
                : Optional.of(appDescriptorMeta.getAppDescriptor());
    }

    @Override
    public AppDescriptor createAppDescriptor(final InputStream inputStream,
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
        Logger.debug(AppsAPIImpl.class,
                () -> " ymlFiles are set under:  " + ymlFilesPath);

        // Now validate the incoming file.. see if we're rewriting an existing file or attempting to re-use an already in use service-key.
        final AppDescriptor serviceDescriptor = ymlMapper
                .readValue(inputStream, AppDescriptor.class);

        if (validateServiceDescriptor(serviceDescriptor)
                && validateAppDescriptorUniqueName(serviceDescriptor)) {

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
        return serviceDescriptor;
    }

    @Override
    public void removeApp(final String key, final User user,
            final boolean removeDescriptor)
            throws DotSecurityException, DotDataException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to delete a service descriptors performed by user with id `%s`.",
                    user.getUserId()));
        }
        final String appKeyLC = key.toLowerCase();
        final AppDescriptorMeta appDescriptorMeta = getAppDescriptorMap().get(appKeyLC);
        if (null == appDescriptorMeta) {
            throw new DoesNotExistException(String.format("The requested descriptor `%s` does not exist.",key));
        }else{
            // if we succeed trying to find the descriptor.
            // Lets get all the service-keys and organized by host-id
            final Map<String, Set<String>> appKeysByHost = appKeysByHost(true);
            for (final Entry<String, Set<String>> entry : appKeysByHost.entrySet()) {
                final String hostId = entry.getKey();
                final Set<String> appKeys = entry.getValue();
                //Now we need to verify the service-key has a configuration for this host.
                if(appKeys.contains(appKeyLC)){
                    //And if it does we attempt to delete all the secrets.
                    final Host host = hostAPI.find(hostId, user, false);
                    deleteSecrets(key, host, user);
                }
            }
            if(removeDescriptor) {
                removeDescriptor(appDescriptorMeta);
            }
            invalidateCache();
        }
    }

    /**
     * Removes the yml file itself.
     * @param serviceDescriptorMeta
     * @throws DotDataException
     */
    private void removeDescriptor(final AppDescriptorMeta serviceDescriptorMeta) throws DotDataException{
        final String fileName = serviceDescriptorMeta.getFileName();
        //Now we need to remove the file it self.
        final String ymlFilesPath = getServiceDescriptorDirectory();
        final Path file = Paths.get(ymlFilesPath + File.separator + fileName).normalize();
        if (!file.toFile().exists()) {
            throw new DotDataException(
                    String.format(" File with path `%s` does not exist. ", file));
        }
        try {
            Logger.warn(AppsAPIImpl.class, () -> String
                    .format(" Failed attempt to delete file with path `%s` ", file));
            Files.delete(file);
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    /**
     * This gives a Map organized by host id that contains a Map of secrets and their warnings asa list.
     * @param appDescriptor
     * @param sitesWithConfigurations
     * @param user
     * @return A Map organized by host id that contains a Map of secrets and their warnings
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public Map<String, Map<String, List<String>>> computeWarningsBySite(final AppDescriptor appDescriptor,
            final Set<String> sitesWithConfigurations, final User user)
            throws DotSecurityException, DotDataException {
        final Builder<String, Map<String, List<String>>> builder = ImmutableMap.builder();
        for (String hostId : sitesWithConfigurations) {
            final Host site = hostAPI.find(hostId, user, false);
            if(null != site){
               final Map<String, List<String>> warnings = computeSecretWarnings(appDescriptor, site, user);
               builder.put(site.getIdentifier(), warnings);
            }
        }
        return builder.build();
    }

    /**
     * The returned list for now will only have one entry. However the structure is a Map of list to allow several warnings in future implementations.
     * @param appDescriptor
     * @param site
     * @param user
     * @return Map of params and a List of warnings.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public Map<String, List<String>> computeSecretWarnings(final AppDescriptor appDescriptor,
            final Host site, final User user)
            throws DotSecurityException, DotDataException {
        final String appKey = appDescriptor.getKey();
        final boolean hasConfigurations = !filterSitesForAppKey(appKey, of(site.getIdentifier()),
                user).isEmpty();
        if (hasConfigurations) {
            final Optional<AppSecrets> secretsOptional = getSecrets(appKey, site, user);
            if (secretsOptional.isPresent()) {
                final Map<String, List<String>> warnings = new HashMap<>();
                final AppSecrets appSecrets = secretsOptional.get();
                for (final Entry<String, ParamDescriptor> entry : appDescriptor.getParams().entrySet()) {
                    final String paramName = entry.getKey();
                    final ParamDescriptor descriptor = entry.getValue();
                    final Secret secret = appSecrets.getSecrets().get(paramName);
                    if (descriptor.isRequired() && UtilMethods.isNotSet(descriptor.getValue()) && (
                            null == secret || UtilMethods.isNotSet(secret.getValue()))) {
                        warnings.put(paramName, of(String
                                .format("`%s` is required. It is missing a value and no default is provided.",
                                        paramName)));
                    }
                }
                return warnings;
            }
        }
        return emptyMap();
    }

    private void invalidateCache() {
        synchronized (AppsAPIImpl.class) {
            appsCache.invalidateDescriptorsCache();
        }
    }

    private static String getServiceDescriptorDirectory() {
        final Supplier<String> supplier = () -> APILocator.getFileAssetAPI().getRealAssetsRootPath()
        + File.separator + SERVER_DIR_NAME + File.separator + APPS_DIR_NAME + File.separator;
        final String dirPath = Config
                .getStringProperty(APPS_DIR_PATH_KEY, supplier.get());
        return Paths.get(dirPath).normalize().toString();
    }

    private Set<String> listAvailableYamlFiles() throws IOException {
        final String ymlFilesPath = getServiceDescriptorDirectory();
        final File basePath = new File(ymlFilesPath);
        if (!basePath.exists()) {
            basePath.mkdir();
        }
        Logger.debug(AppsAPIImpl.class,
                () -> " ymlFiles are set under:  " + ymlFilesPath);
        final Set<String> files = listFiles(ymlFilesPath);
        if (!UtilMethods.isSet(files)) {
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

    private List<AppDescriptorMeta> loadAppDescriptors()
            throws IOException, DotDataException {
        final Set<String> loadedServiceKeys = new HashSet<>();
        final ImmutableList.Builder<AppDescriptorMeta> builder = new ImmutableList.Builder<>();
        final Set<String> fileNames = listAvailableYamlFiles();
        for (final String fileName : fileNames) {
            try {
                final File file = new File(fileName);
                final AppDescriptor serviceDescriptor = ymlMapper
                        .readValue(file, AppDescriptor.class);
                if (validateServiceDescriptor(serviceDescriptor)) {
                    if (loadedServiceKeys.contains(serviceDescriptor.getKey())) {
                        throw new DotDataException(
                                String.format(
                                        "There's another App already registered under key `%s`.",
                                        serviceDescriptor.getKey())
                                );
                    }
                    builder.add(new AppDescriptorMeta(serviceDescriptor, file.getName()));
                    loadedServiceKeys.add(serviceDescriptor.getKey());
                }
            } catch (IOException | DotDataValidationException e) {
                Logger.error(AppsAPIImpl.class,
                        String.format("Error reading yml file `%s`.", fileName), e);
            }
        }

        return builder.build();
    }

    /**
     * internal descriptor validator
     * @param appDescriptor
     * @return
     * @throws DotDataValidationException
     */
   private boolean validateServiceDescriptor(final AppDescriptor appDescriptor)
           throws DotDataValidationException {

       final List<String> errors = new ArrayList<>();

       if(UtilMethods.isNotSet(appDescriptor.getKey())){
          errors.add("The required field `key` isn't set on the incoming file.");
       }

       if(DESCRIPTOR_KEY_MAX_LENGTH < appDescriptor.getKey().length()){
           errors.add(String.format("The required field `key` exceeds %d chars length.", DESCRIPTOR_KEY_MAX_LENGTH));
       }

       if(UtilMethods.isNotSet(appDescriptor.getName())){
           errors.add("The required field `name` isn't set on the incoming file.");
       }

       if(UtilMethods.isNotSet(appDescriptor.getDescription())){
           errors.add("The required field `description` isn't set on the incoming file.");
       }

       if(UtilMethods.isNotSet(appDescriptor.getIconUrl())){
           errors.add("The required field `iconUrl` isn't set on the incoming file.");
       }

       if(!UtilMethods.isSet(appDescriptor.getAllowExtraParameters())){
           errors.add("The required boolean field `allowExtraParameters` isn't set on the incoming file.");
       }

       if(!UtilMethods.isSet(appDescriptor.getParams())){
           errors.add("The required field `params` isn't set on the incoming file.");
       }

       for (final Map.Entry<String, ParamDescriptor> entry : appDescriptor.getParams().entrySet()) {
           errors.addAll(validateParamDescriptor(entry.getKey(), entry.getValue()));
       }

       if(!errors.isEmpty()){
           throw new DotDataValidationException(String.join(" \n", errors));
       }

       return true;

   }

    /**
     * internal param validator
     * @param name
     * @param descriptor
     * @return
     * @throws DotDataValidationException
     */
    private List<String> validateParamDescriptor(final String name,
            final ParamDescriptor descriptor)  {

        final List<String> errors = new ArrayList<>();

        if (UtilMethods.isNotSet(name)) {
            errors.add("Param descriptor is missing required  field `name` .");
        }

        if (DESCRIPTOR_NAME_MAX_LENGTH < name.length()) {
            errors.add(String.format("`%s`: exceeds %d chars length.", name,
                    DESCRIPTOR_NAME_MAX_LENGTH));
        }

        if (null == descriptor.getValue()) {
            errors.add(String.format("`%s`: is missing required field `value`. It is mandatory that the param exist. ", name));
        }

        if (UtilMethods.isNotSet(descriptor.getHint())) {
            errors.add(String.format("Param `%s`: is missing required field `hint` .", name));
        }

        if (UtilMethods.isNotSet(descriptor.getLabel())) {
            errors.add(String.format("Param `%s`: is missing required field `hint` .", name));
        }

        if (null == descriptor.getType()) {
            errors.add(String.format("Param `%s`: is missing required field `type` (STRING|BOOL|FILE) .",
                    name));
        }

        if (!UtilMethods.isSet(descriptor.getRequired())) {
            errors.add(String.format("Param `%s`: is missing required field `required` (true|false) .",
                    name));
        }

        if (!UtilMethods.isSet(descriptor.getHidden())) {
            errors.add(
                    String.format("Param `%s`: is missing required field `hidden` (true|false) .", name));
        }

        if (Type.BOOL.equals(descriptor.getType()) && UtilMethods.isSet(descriptor.getHidden()) && descriptor.isHidden()) {
            errors.add(String.format(
                    "Param `%s`: Bool params can not be marked hidden. The combination (Bool + Hidden) isn't allowed.",
                    name));
        }

        if (Type.BOOL.equals(descriptor.getType()) && UtilMethods.isSet(descriptor.getValue())
                && !isBoolString(descriptor.getValue())) {
            errors.add(String.format(
                    "Boolean Param `%s` has a default value `%s` that can not be parsed to bool (true|false).",
                    name, descriptor.getValue()));
        }

        if (StringPool.NULL.equalsIgnoreCase(descriptor.getValue()) && descriptor.isRequired()) {
            errors.add(String.format(
                    "Null isn't allowed as the default value on required params see `%s`. ",
                    name)
            );
        }

        return errors;
    }

    /**
     * Verifies if a string can be parsed to boolean safely.
     * @param value
     * @return
     */
   private boolean isBoolString(final String value){
      return Boolean.TRUE.toString().equalsIgnoreCase(value) || Boolean.FALSE.toString().equalsIgnoreCase(value);
   }

    private boolean validateAppDescriptorUniqueName(final AppDescriptor serviceDescriptor)
            throws DotDataException {

        if (getAppDescriptorMap().containsKey(serviceDescriptor.getKey())) {
            throw new DotDataException(
                    String.format("There's a service already registered under key `%s`.",
                            serviceDescriptor.getKey()));
        }
        return true;
    }

    /**
     * Method meant to to be consumed from a delete site event.
     * @param host
     * @param user
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public void removeSecretsForSite(final Host host, final User user)
            throws DotDataException, DotSecurityException {
        Logger.info(AppsAPIImpl.class, () -> String.format(" Removing secrets under site `%s` ", host.getName()));
        //This must be called with param filterNonExisting in false since the first thing that delete-site does is clear cache.
        final Map<String, Set<String>> keysByHost = appKeysByHost(false);
        final Set<String> secretKeys = keysByHost.get(host.getIdentifier().toLowerCase());
        if (null != secretKeys) {
            for (final String secretKey : secretKeys) {
                deleteSecrets(secretKey, host.getIdentifier(), user);
                Logger.info(AppsAPIImpl.class, () -> String.format(" Secret with `%s` has been removed. ", secretKey));
            }
        }
    }

    /**
     * On the event of a Company Key reset. We need to react and handle it as best we can
     * @param user
     * @throws DotDataException
     */
    @Override
    public void resetSecrets(final User user)
            throws DotDataException, IOException {
       secretsStore.backupAndRemoveKeyStore();
       appsCache.clearCache();
    }

}
