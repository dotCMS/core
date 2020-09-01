package com.dotcms.security.apps;

import static com.dotcms.security.apps.AppsUtil.readJson;
import static com.dotcms.security.apps.AppsUtil.toJsonAsChars;
import static com.dotmarketing.util.UtilMethods.isNotSet;
import static com.dotmarketing.util.UtilMethods.isSet;
import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.emptyMap;

import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.liferay.portal.model.User;
import com.liferay.util.EncryptorException;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
    private final ContentletAPI contentletAPI;
    private final SecretsStore secretsStore;
    private final AppsCache appsCache;
    private final LocalSystemEventsAPI localSystemEventsAPI;

    private final LicenseValiditySupplier licenseValiditySupplier;

    private final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory())
            .enable(Feature.STRICT_DUPLICATE_DETECTION)
            //.enable(SerializationFeature.INDENT_OUTPUT)
            .findAndRegisterModules();

    @VisibleForTesting
    public AppsAPIImpl(final UserAPI userAPI, final LayoutAPI layoutAPI, final HostAPI hostAPI, final ContentletAPI contentletAPI,
            final SecretsStore secretsRepository, final AppsCache appsCache, final LocalSystemEventsAPI localSystemEventsAPI, final LicenseValiditySupplier licenseValiditySupplier) {
        this.userAPI = userAPI;
        this.layoutAPI = layoutAPI;
        this.hostAPI = hostAPI;
        this.contentletAPI = contentletAPI;
        this.secretsStore = secretsRepository;
        this.appsCache = appsCache;
        this.localSystemEventsAPI = localSystemEventsAPI;
        this.licenseValiditySupplier = licenseValiditySupplier;
    }

    public AppsAPIImpl() {
        this(APILocator.getUserAPI(), APILocator.getLayoutAPI(), APILocator.getHostAPI(),
                APILocator.getContentletAPI(), SecretsStore.INSTANCE.get(),
                CacheLocator.getAppsCache(), APILocator.getLocalSystemEventsAPI(),
                new LicenseValiditySupplier() {
                });
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
        final String key = isSet(serviceKey) ? serviceKey : DOT_GLOBAL_SERVICE;
        final String identifier =
                (null == hostIdentifier) ? APILocator.systemHost().getIdentifier() : hostIdentifier;
        return (identifier + HOST_SECRET_KEY_SEPARATOR + key).toLowerCase();
    }

    private boolean userDoesNotHaveAccess(final User user) throws DotDataException {
        return !user.isAdmin() && !layoutAPI
                .doesUserHaveAccessToPortlet(APPS_PORTLET_ID, user);
    }

    @Override
    public List<String> listAppKeys(final Host host, final User user)
            throws DotDataException, DotSecurityException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to get all App keys performed by user with id `%s` and host `%s` ",
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
     * Valid sites are those which are in working state and not marked as deleted (meaning archived)
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Set<String> getValidSites() throws DotSecurityException, DotDataException {
        return contentletAPI
                .searchIndex("+contentType:Host +working:true -deleted:true ", 0, 0, null,
                        APILocator.systemUser(), false).stream()
                .map(ContentletSearch::getIdentifier).collect(Collectors.toSet());
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
            final Set<String> validSites = getValidSites();
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

        if(!licenseValiditySupplier.hasValidLicense()){
            throw new InvalidLicenseException("Apps requires of an enterprise level license.");
        }
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
                    notifySaveEventAndDestroySecret(secrets, host, user);
                } finally {
                    if (null != chars) {
                        Arrays.fill(chars, (char) 0);
                    }
                }
            }
        }
    }

    /***
     * This will broadcast an async AppSecretSavedEvent
     * and will also perform a clean-up (destroy) over the secret once all the event subscribers are done consuming the event.
     * @param secrets
     * @param host
     * @param user
     */
    private void notifySaveEventAndDestroySecret(final AppSecrets secrets, final Host host, final User user) {
        localSystemEventsAPI.asyncNotify(new AppSecretSavedEvent(secrets, host, user.getUserId()),
            event -> {
                final AppSecretSavedEvent appSecretSavedEvent = (AppSecretSavedEvent) event;
                final AppSecrets appSecrets = appSecretSavedEvent.getAppSecrets();
                if (null != appSecrets) {
                    appSecrets.destroy();
                }
            });
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
    public List<AppDescriptor> getAppDescriptors(final User user)
            throws DotDataException, DotSecurityException {

        if(!licenseValiditySupplier.hasValidLicense()){
            throw new InvalidLicenseException("Apps requires of an enterprise level license.");
        }

        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to get all available App descriptors performed by user with id `%s`.",
                    user.getUserId()));
        }

        return getAppDescriptorsMeta();
    }

    private List<AppDescriptor> getAppDescriptorsMeta() {

        synchronized (AppsAPIImpl.class) {
            return appsCache.getAppDescriptorsMeta(() -> {
                try {
                    return loadAppDescriptors();
                } catch (IOException | URISyntaxException e) {
                    Logger.error(AppsAPIImpl.class,
                            "An error occurred while loading the service descriptor yml files. ",
                            e);
                    throw new DotRuntimeException(e);
                }
            });
        }
    }

    private Map<String, AppDescriptor> getAppDescriptorMap(){
       return appsCache.getAppDescriptorsMap(this::getAppDescriptorsMeta);
    }

    @Override
    public Optional<AppDescriptor> getAppDescriptor(final String key,
            final User user)
            throws DotDataException, DotSecurityException {

        if(!licenseValiditySupplier.hasValidLicense()){
           throw new InvalidLicenseException("Apps requires of an enterprise level license.");
        }

        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to get all available App descriptors performed by user with id `%s`.",
                    user.getUserId()));
        }

        final String appKeyLC = key.toLowerCase();
        final AppDescriptor appDescriptorMeta = getAppDescriptorMap()
                .get(appKeyLC);
        return null == appDescriptorMeta ? Optional.empty()
                : Optional.of(appDescriptorMeta);
    }

    @Override
    public AppDescriptor createAppDescriptor(final File file,
            final User user) throws DotDataException, AlreadyExistException, DotSecurityException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to create an app descriptor performed by user with id `%s`.",
                    user.getUserId()));
        }
        final Path ymlFilesPath = getUserAppsDescriptorDirectory();
        final File basePath = ymlFilesPath.toFile();
        if (!basePath.exists()) {
            basePath.mkdirs();
        }
        Logger.debug(AppsAPIImpl.class, () -> " ymlFiles are set under:  " + ymlFilesPath);

            final AppSchema appSchema = readAppFile(file.toPath());
            // Now validate the incoming file.. see if we're rewriting an existing file or attempting to re-use an already in use service-key.
            if (validateAppDescriptor(appSchema)) {
                final File incomingFile = new File(basePath, file.getName());
                if (incomingFile.exists()) {
                    throw new AlreadyExistException(
                            String.format(
                                    "Invalid attempt to override an existing file named '%s'.",
                                    incomingFile.getName()));
                }

                writeAppFile(incomingFile, appSchema);

                invalidateCache();
            }
            return new AppDescriptorImpl(file.getName(), false, appSchema);

    }

    /**
     * There's a version of the method readValue on the ymlMapper which takes a file and internally creates directly a FileInputStream
     * According to https://dzone.com/articles/fileinputstream-fileoutputstream-considered-harmful
     * that's very harmful
     * @param file
     * @return
     * @throws DotDataException
     */
    private AppSchema readAppFile(final Path file) throws DotDataException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            return ymlMapper.readValue(inputStream, AppSchema.class);
        }catch (Exception e){
            throw new DotDataException(e.getMessage(), e);
        }
    }
    /**
     * There's a version of the method writeValue on the ymlMapper which takes a file and internally creates directly a FileOutputStream
     * According to https://dzone.com/articles/fileinputstream-fileoutputstream-considered-harmful
     * that's very harmful
     * @param file
     * @return
     * @throws DotDataException
     */
    private void writeAppFile(final File file, final AppSchema appSchema) throws DotDataException {
        try (OutputStream outputStream = Files.newOutputStream(Paths.get(file.getPath()))) {
             ymlMapper.writeValue(outputStream, appSchema);
        }catch (Exception e){
            throw new DotDataException(e.getMessage(), e);
        }
    }


    @Override
    public void removeApp(final String key, final User user,
            final boolean removeDescriptor)
            throws DotSecurityException, DotDataException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid attempt to delete an App descriptor performed by user with id `%s`.",
                    user.getUserId()));
        }
        final String appKeyLC = key.toLowerCase();
        final AppDescriptor appDescriptorMeta = getAppDescriptorMap().get(appKeyLC);
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
                    deleteSecrets(key, hostId, user);
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
     * @param descriptor
     * @throws DotDataException
     */
    private void removeDescriptor(final AppDescriptor descriptor)
            throws DotDataException, DotSecurityException {
        final AppDescriptorImpl appDescriptor = (AppDescriptorImpl)descriptor;
        if(appDescriptor.isSystemApp()){
            throw new DotSecurityException(" System app files are not allowed to be removed. ");
        }
        final String fileName = appDescriptor.getFileName();
        //Now we need to remove the file it self.
        final Path ymlFilesPath = getUserAppsDescriptorDirectory();
        final Path file = Paths.get(ymlFilesPath + File.separator + fileName).normalize();
        if (!file.toFile().exists()) {
            throw new DoesNotExistException(
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
        for (String siteId : sitesWithConfigurations) {
            //if this is coming directly from the keyStore it's all lowercase.
            siteId = Host.SYSTEM_HOST.equalsIgnoreCase(siteId) ? Host.SYSTEM_HOST : siteId;
            final Host site = hostAPI.find(siteId, user, false);
            if(null != site){
               final Map<String, List<String>> warnings = computeSecretWarnings(appDescriptor, site, user);
               builder.put(site.getIdentifier().toLowerCase(), warnings);
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
                    if (isRequiredWithNoDefaultValue(descriptor, secret)) {
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

    /**
     * Condition check This verifies the descriptor demands the param to be required but.. No default value is provided and the secret neither has a stored value
     * @param descriptor ParamDescriptor
     * @param secret stored secret
     * @return
     */
    private boolean isRequiredWithNoDefaultValue(final ParamDescriptor descriptor, final Secret secret ){
        //Verify we have a param marked required and no default Value
        final boolean isRequiredWithNoDefaultParam = (descriptor.isRequired() && isEmpty(descriptor.getValue()));
        //Verify the secret is empty
        final boolean isSecretWithEmptyValue = (null == secret || isNotSet(secret.getValue()));
        return isRequiredWithNoDefaultParam && isSecretWithEmptyValue;
    }

    /**
     * Verify an object is an empty value
     * @param value
     * @return
     */
    private boolean isEmpty(final Object value){
        if(value == null){
           return true;
        }

        if(value instanceof String){
           return isNotSet((String)value);
        }

        if(value instanceof char[]){
            return isNotSet((char[]) value);
        }

        if(value instanceof List){
           return  ((List)value).isEmpty();
        }

        return false;
    }

    private void invalidateCache() {
        synchronized (AppsAPIImpl.class) {
            appsCache.invalidateDescriptorsCache();
        }
    }

    /**
     * This is the directory intended for customers use
     * @return
     */
    private static Path getUserAppsDescriptorDirectory() {
        final Supplier<String> supplier = () -> APILocator.getFileAssetAPI().getRealAssetsRootPath()
        + File.separator + SERVER_DIR_NAME + File.separator + APPS_DIR_NAME + File.separator;
        final String dirPath = Config
                .getStringProperty(APPS_DIR_PATH_KEY, supplier.get());
        return Paths.get(dirPath).normalize();
    }

    /**
     * This is the Apps-System-Folder which is meant to hold system apps.
     * Those that can not be override and are always available.
     * @return
     */
    static Path getSystemAppsDescriptorDirectory() throws URISyntaxException, IOException {
        final URL res = Thread.currentThread().getContextClassLoader().getResource("apps");
        if(res == null) {
            throw new IOException("Unable to find Apps System folder. It should be at /WEB-INF/classes/apps ");
        } else {
            return Paths.get(res.toURI()).toAbsolutePath();
        }
    }

    /**
     *  This will get you a list with all the available app-yml files registered in the system.
     *
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    private Set<Tuple2<Path, Boolean>> listAvailableYamlFiles() throws IOException, URISyntaxException {
        final Path systemAppsDescriptorDirectory = getSystemAppsDescriptorDirectory();
        final Set<Path> systemFiles = listFiles(systemAppsDescriptorDirectory);

        final Path appsDescriptorDirectory = getUserAppsDescriptorDirectory();
        final File basePath = appsDescriptorDirectory.toFile();
        if (!basePath.exists()) {
            basePath.mkdirs();
        }
        Logger.debug(AppsAPIImpl.class,
                () -> " ymlFiles are set under:  " + basePath.toString());
        final Set<Path> userFiles = listFiles(appsDescriptorDirectory);

        final Set<Path> systemFileNames = systemFiles.stream().map(Path::getFileName)
                .collect(Collectors.toSet());
        final Set<Path> filteredUserFiles = userFiles.stream()
                .filter(path -> systemFileNames.stream().noneMatch(
                        systemPath -> systemPath.toString()
                                .equalsIgnoreCase((path.getFileName().toString().toLowerCase()))))
                .collect(Collectors.toSet());

        return Stream.concat(systemFiles.stream().map(path -> Tuple.of(path, true)),
                filteredUserFiles.stream().map(path -> Tuple.of(path, false)))
                .collect(Collectors.toSet());
    }

    private static DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {

        private static final String ignorePrefix = "_ignore_";
        private static final String yml = "yml";
        private static final String yaml = "yaml";

        @Override
        public boolean accept(final Path path) {
            if (Files.isDirectory(path)) {
              return false;
            }
            final String fileName = path.getFileName().toString();
            return !fileName.startsWith(ignorePrefix) && (fileName.endsWith(yaml) || fileName.endsWith(yml)) ;
        }
    };

    private Set<Path> listFiles(final Path dir) throws IOException {
        final Set<Path> fileList = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
            stream.forEach(fileList::add);
        }
        return fileList;
    }

    private List<AppDescriptor> loadAppDescriptors()
            throws IOException, URISyntaxException {

        final ImmutableList.Builder<AppDescriptor> builder = new ImmutableList.Builder<>();
        final Set<Tuple2<Path, Boolean>> filePaths = listAvailableYamlFiles();
        for (final Tuple2<Path, Boolean> filePath : filePaths) {
            try {
                final Path path = filePath._1;
                final boolean systemApp = filePath._2;
                final AppSchema appSchema = readAppFile(path);
                if (validateAppDescriptor(appSchema)) {
                    builder.add(new AppDescriptorImpl(path.getFileName().toString(), systemApp, appSchema));
                }
            } catch (Exception e) {
                Logger.error(AppsAPIImpl.class,
                        String.format("Error reading yml file `%s`.", filePath), e);
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
   private boolean validateAppDescriptor(final AppSchema appDescriptor)
           throws DotDataValidationException {

       final List<String> errors = new ArrayList<>();

       if(isNotSet(appDescriptor.getName())){
           errors.add("The required field `name` isn't set on the incoming file.");
       }

       if(isNotSet(appDescriptor.getDescription())){
           errors.add("The required field `description` isn't set on the incoming file.");
       }

       if(isNotSet(appDescriptor.getIconUrl())){
           errors.add("The required field `iconUrl` isn't set on the incoming file.");
       }

       if(!isSet(appDescriptor.getAllowExtraParameters())){
           errors.add("The required boolean field `allowExtraParameters` isn't set on the incoming file.");
       }

       if(!isSet(appDescriptor.getParams())){
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
            final ParamDescriptor descriptor) {

        final List<String> errors = new LinkedList<>();

        if (isNotSet(name)) {
            errors.add("Param descriptor is missing required  field `name` .");
        }

        if (DESCRIPTOR_NAME_MAX_LENGTH < name.length()) {
            errors.add(String.format("`%s`: exceeds %d chars length.", name,
                    DESCRIPTOR_NAME_MAX_LENGTH));
        }

        if (null == descriptor.getValue()) {
            errors.add(String.format(
                    "`%s`: is missing required field `value` or a value hasn't been set. Value is mandatory. ",
                    name));
        }

        if (isNotSet(descriptor.getHint())) {
            errors.add(String.format("Param `%s`: is missing required field `hint` .", name));
        }

        if (isNotSet(descriptor.getLabel())) {
            errors.add(String.format("Param `%s`: is missing required field `hint` .", name));
        }

        if (null == descriptor.getType()) {
            errors.add(String.format(
                    "Param `%s`: is missing required field `type` (STRING|BOOL|SELECT) .",
                    name));
        }

        if (!isSet(descriptor.getRequired())) {
            errors.add(
                    String.format("Param `%s`: is missing required field `required` (true|false) .",
                            name));
        }

        if (!isSet(descriptor.getHidden())) {
            errors.add(
                    String.format("Param `%s`: is missing required field `hidden` (true|false) .",
                            name));
        }

        if (isSet(descriptor.getValue()) && StringPool.NULL
                .equalsIgnoreCase(descriptor.getValue().toString()) && descriptor.isRequired()) {
            errors.add(String.format(
                    "Null isn't allowed as the default value on required params see `%s`. ",
                    name)
            );
        }

        if (Type.BOOL.equals(descriptor.getType())) {
            if (isSet(descriptor.getHidden())
                    && descriptor.isHidden()) {
                errors.add(String.format(
                        "Param `%s`: Bool params can not be marked hidden. The combination (Bool + Hidden) isn't allowed.",
                        name));
            }

            if (isSet(descriptor.getValue())
                    && !isBoolString(descriptor.getValue().toString())) {
                errors.add(String.format(
                        "Boolean Param `%s` has a default value `%s` that can not be parsed to bool (true|false).",
                        name, descriptor.getValue()));
            }
        }

        if(Type.STRING.equals(descriptor.getType()) && !(descriptor.getValue() instanceof String)){
                errors.add(String.format(
                        "Value Param `%s` has a default value `%s` that isn't a string .",
                        name, descriptor.getValue()));
        }

        if (Type.SELECT.equals(descriptor.getType())) {

            if (isSet(descriptor.getHidden()) && descriptor.isHidden()) {
                errors.add(String.format(
                        "Param `%s`: List params can not be marked hidden. The combination (List + Hidden) isn't allowed.",
                        name));
            }

            if (!(descriptor.getValue() instanceof List)) {
                errors.add(String.format(
                        " As param `%s`:  is marked as `List` the field value is expected to hold a list of objects. ",
                        name));
            } else {
                final int minSelectedElements = 1;
                int selectedCount = 0;
                final List list = (List) descriptor.getValue();
                for (final Object object : list) {
                    if (!(object instanceof Map)) {
                        errors.add(String.format(
                                "Malformed list. Param: `%s` is marked as `List` therefore field `value` is expected to have a list of objects. ",
                                name));
                    } else {
                        final Map map = (Map) object;
                        if (!map.containsKey("label") || !map.containsKey("value") ) {
                            errors.add(String.format("Malformed list. Param: `%s`. Every entry of the `List` has to have the following fields (`label`,`value`). ", name));
                        }
                         if(map.containsKey("selected")){
                             selectedCount++;
                         }
                    }
                }
                if(selectedCount > minSelectedElements ){
                    errors.add(String.format("Malformed list. Param: `%s`. There must be only 1 item marked as selected ", name));
                }
            }
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
     * On the event of a Key reset. We need to react and handle it as best we can.
     * @param user
     * @throws DotDataException
     */
    @Override
    public void resetSecrets(final User user)
            throws DotDataException, IOException {
       //Since we just regenerated the key Company. Accessing it is near impossible.
       //Best we can do is create a backup and recreate an empty one.
       secretsStore.backupAndRemoveKeyStore();
       //Clear cache forces reloading the yml app descriptors.
       appsCache.clearCache();
    }

    /**
     * {@inheritDoc}
     * @param key
     * @param paramAppKeysBySite
     * @return
     */
    public Path exportSecrets(final Key key, final boolean exportAll,
            final Map<String, Set<String>> paramAppKeysBySite, final User user)
            throws DotDataException, DotSecurityException, IOException {

        if(!user.isAdmin()){
            throw new DotSecurityException("Only Admins are allowed to perform an export operation.");
        }

        final AppsSecretsImportExport exportedSecrets;
        if (exportAll) {
            exportedSecrets = collectSecretsForExport(appKeysByHost(), user);
        } else {
            exportedSecrets = collectSecretsForExport(paramAppKeysBySite, user);
        }

        Logger.info(AppsAPIImpl.class,""+exportedSecrets);

        final File tempFile = File.createTempFile("secretsExport", ".tmp");
        try {
            writeObject(exportedSecrets, tempFile.toPath());
            final byte[] bytes = Files.readAllBytes(tempFile.toPath());
            try {
                final File file = File.createTempFile("secrets", ".export");
                file.deleteOnExit();
                final byte[] encrypted = AppsUtil.encrypt(key, bytes);
                final Path path = file.toPath();
                try (OutputStream outputStream = Files.newOutputStream(path)) {
                    outputStream.write(encrypted);
                    return path;
                }
            } catch (EncryptorException e) {
                throw new DotDataException(e);
            }
        } finally {
            tempFile.delete();
        }
    }

    /**
     *
     * @param paramAppKeysBySite
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private AppsSecretsImportExport collectSecretsForExport(final Map<String, Set<String>> paramAppKeysBySite, final User user)
            throws DotDataException, DotSecurityException {
        final Map<String, List<AppSecrets>> exportedSecrets = new HashMap<>();
        final Map<String, Set<String>> keysByHost = appKeysByHost();
        keysByHost.forEach((siteId, appKeys) -> {
            try {
                final Host site = hostAPI.find(siteId, user, false);
                if (null != site) {

                    final Set<String> appKeysBySiteId = paramAppKeysBySite.get(siteId);
                    if (isSet(appKeysBySiteId)) {
                        for (final String appKey : appKeysBySiteId) {
                            final Optional<AppSecrets> optional = getSecrets(appKey, site, user);
                            if (optional.isPresent()) {
                                final AppSecrets appSecrets = optional.get();
                                exportedSecrets
                                        .computeIfAbsent(siteId, list -> new LinkedList<>())
                                        .add(appSecrets);
                            }
                        }
                    }
                } else {
                    Logger.warn(AppsAPIImpl.class,
                            String.format("Unable to find site `%s` ", siteId));
                }
            } catch (DotDataException | DotSecurityException e) {
                Logger.warn(AppsAPIImpl.class, "An exception occurred collecting the secrets for export", e);
            }
        });
        return new AppsSecretsImportExport(
                exportedSecrets);
    }

    /**
     * Takes a wrapping object that encapsulates all entries an write'em out ino a stream
     * @param bean
     * @param file
     * @throws IOException
     */
    private void writeObject(final AppsSecretsImportExport bean, final Path file)
            throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(file)) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
                objectOutputStream.writeObject(bean);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @param incomingFile
     * @param key
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     * @throws EncryptorException
     */
   public Map<String, List<AppSecrets>> importSecrets(final Path incomingFile, final Key key, final User user)
            throws DotDataException, DotSecurityException, IOException, EncryptorException {
        if(!user.isAdmin()){
            throw new DotSecurityException("Only Admins are allowed to perform an export operation.");
        }

        final byte[] encryptedBytes = Files.readAllBytes(incomingFile);
        final byte[] decryptedBytes = AppsUtil.decrypt(key, encryptedBytes);
        final File importFile = File.createTempFile("secrets", "export");
        try (OutputStream outputStream = Files.newOutputStream(importFile.toPath())) {
            outputStream.write(decryptedBytes);
        }
       final AppsSecretsImportExport importExport;
       try {
           importExport = readObject(importFile.toPath());
           return importExport.getSecrets();
       } catch (ClassNotFoundException e) {
           throw new DotDataException(e);
       }
    }

    /**
     * Reads the exported file stream
     * and returns a wrapper that contains all entries.
     * @param importFile
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private AppsSecretsImportExport readObject(final Path importFile)
            throws IOException, ClassNotFoundException {
        try(InputStream inputStream = Files.newInputStream(importFile)){
            return (AppsSecretsImportExport)new ObjectInputStream(inputStream).readObject();
        }
    }

}
