package com.dotcms.security.apps;

import static com.dotcms.security.apps.AppDescriptorHelper.getUserAppsDescriptorDirectory;
import static com.dotcms.security.apps.AppsUtil.exportSecret;
import static com.dotcms.security.apps.AppsUtil.importSecrets;
import static com.dotcms.security.apps.AppsUtil.internalKey;
import static com.dotcms.security.apps.AppsUtil.mapForValidation;
import static com.dotcms.security.apps.AppsUtil.readJson;
import static com.dotcms.security.apps.AppsUtil.toJsonAsChars;
import static com.dotcms.security.apps.AppsUtil.validateForSave;
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
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This API serves as the bridge between the secrets safe repository
 * and the structure of the APP defined via YML file descriptor.
 */
public class AppsAPIImpl implements AppsAPI {

    private final LayoutAPI layoutAPI;
    private final HostAPI hostAPI;
    private final SecretsStore secretsStore;
    private final AppsCache appsCache;
    private final LocalSystemEventsAPI localSystemEventsAPI;
    private final LicenseValiditySupplier licenseValiditySupplier;
    private final AppDescriptorHelper appDescriptorHelper;

    @VisibleForTesting
    public AppsAPIImpl(final LayoutAPI layoutAPI, final HostAPI hostAPI,
            final SecretsStore secretsRepository, final AppsCache appsCache,
            final LocalSystemEventsAPI localSystemEventsAPI,
            final AppDescriptorHelper appDescriptorHelper,
            final LicenseValiditySupplier licenseValiditySupplier) {
        this.layoutAPI = layoutAPI;
        this.hostAPI = hostAPI;
        this.secretsStore = secretsRepository;
        this.appsCache = appsCache;
        this.localSystemEventsAPI = localSystemEventsAPI;
        this.appDescriptorHelper = appDescriptorHelper;
        this.licenseValiditySupplier = licenseValiditySupplier;
    }

    /**
     * default constructor
     */
    public AppsAPIImpl() {
        this(
                APILocator.getLayoutAPI(),
                APILocator.getHostAPI(),
                SecretsStore.INSTANCE.get(),
                CacheLocator.getAppsCache(),
                APILocator.getLocalSystemEventsAPI(),
                new AppDescriptorHelper(),
                new LicenseValiditySupplier() {});
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
        return Stream.concat(
                Stream.of(APILocator.systemHost()),
                hostAPI.findAllFromCache(APILocator.systemUser(), false).stream()
        ).filter(host -> Try.of(() -> !host.isArchived()).getOrElse(false)).map(Host::getIdentifier)
                .map(String::toLowerCase).collect(Collectors.toSet());
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
    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<AppSecrets> getSecrets(final String key,
            final Host host, final User user) throws DotDataException, DotSecurityException {
            return getSecrets(key, false, host, user);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
            saveSecrets(secrets, host.getIdentifier(), user);
    }

    @Override
    public void saveSecrets(final AppSecrets secrets, final String hostIdentifier, final User user)
            throws DotDataException, DotSecurityException {
        if (userDoesNotHaveAccess(user)) {
            throw new DotSecurityException(String.format(
                    "Invalid secret update attempt on `%s` performed by user with id `%s` and host `%s` ",
                    secrets.getKey(), user.getUserId(), hostIdentifier));
        } else {
            final String internalKey = internalKey(secrets.getKey(), hostIdentifier);
            if (secrets.getSecrets().isEmpty()) {
                //if everything has been removed from the json entry we need to kick it off from cache.
                secretsStore.deleteValue(internalKey);
            } else {
                char[] chars = null;
                try {
                    chars = toJsonAsChars(secrets);
                    secretsStore.saveValue(internalKey, chars);
                    notifySaveEventAndDestroySecret(secrets, hostIdentifier, user);
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
     * @param hostIdentifier
     * @param user
     */
    private void notifySaveEventAndDestroySecret(final AppSecrets secrets, final String hostIdentifier, final User user) {
        localSystemEventsAPI.asyncNotify(new AppSecretSavedEvent(secrets, hostIdentifier, user.getUserId()),
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

    /**
     * AppDescriptor mapped as alist
     * @return
     */
    private List<AppDescriptor> getAppDescriptorsMeta() {

        synchronized (AppsAPIImpl.class) {
            return appsCache.getAppDescriptorsMeta(() -> {
                try {
                    return appDescriptorHelper.loadAppDescriptors();
                } catch (IOException | URISyntaxException e) {
                    Logger.error(AppsAPIImpl.class,
                            "An error occurred while loading the service descriptor yml files. ",
                            e);
                    throw new DotRuntimeException(e);
                }
            });
        }
    }

    /**
     * AppDescriptor mapped by appKey
     * @return
     */
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

        final AppSchema appSchema = appDescriptorHelper.readAppFile(file.toPath());
        // Now validate the incoming file.. see if we're rewriting an existing file or attempting to re-use an already in use service-key.
        if (appDescriptorHelper.validateAppDescriptor(appSchema)) {
            final File incomingFile = new File(basePath, file.getName());
            if (incomingFile.exists()) {
                throw new AlreadyExistException(
                        String.format(
                                "Invalid attempt to override an existing file named '%s'.",
                                incomingFile.getName()));
            }

            appDescriptorHelper.writeAppFile(incomingFile, appSchema);

            invalidateCache();
        }
        return new AppDescriptorImpl(file.getName(), false, appSchema);

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
        appsCache.invalidateDescriptorsCache();
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

        if(!licenseValiditySupplier.hasValidLicense()){
            throw new InvalidLicenseException("Apps requires of an enterprise level license.");
        }

        final AppsSecretsImportExport exportedSecrets;
        if (exportAll) {
            exportedSecrets = collectSecretsForExport(appKeysByHost(), user);
        } else {
           if(null == paramAppKeysBySite || paramAppKeysBySite.isEmpty()){
              throw new IllegalArgumentException("No `AppKeysBySite` param was specified.");
           }
           exportedSecrets = collectSecretsForExport(paramAppKeysBySite, user);
        }

        Logger.info(AppsAPIImpl.class," exporting : "+exportedSecrets);

        return exportSecret(exportedSecrets, key);
    }


    /**
     * constructs the Import export object
     * @param paramAppKeysBySite selection
     * @param user user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private AppsSecretsImportExport collectSecretsForExport(final Map<String, Set<String>> paramAppKeysBySite, final User user)
            throws DotDataException, DotSecurityException {
        final Map<String, List<AppSecrets>> exportedSecrets = new HashMap<>();
        final Map<String, Set<String>> keysByHost = appKeysByHost();

        if(keysByHost.isEmpty()){
           throw new IllegalArgumentException("There are no secrets in storage to export. File would be empty. ");
        }

        for (final Entry<String, Set<String>> entry : paramAppKeysBySite.entrySet()) {
            final String siteId = entry.getKey();
            final Host site = Host.SYSTEM_HOST.equalsIgnoreCase(siteId) ? hostAPI.findSystemHost() : hostAPI.find(siteId, user, false);
            if (null != site ) {
                final Set<String> appKeysBySiteId = paramAppKeysBySite.get(siteId);
                if (isSet(appKeysBySiteId)) {
                    for (final String appKey : appKeysBySiteId) {
                        final Optional<AppSecrets> optional = getSecrets(appKey, site, user);
                        if (optional.isPresent()) {
                            final AppSecrets appSecrets = optional.get();
                            exportedSecrets
                                    .computeIfAbsent(siteId, list -> new LinkedList<>())
                                    .add(appSecrets);
                        } else {
                            throw new IllegalArgumentException(String.format("Unable to find secret identified by key `%s` under site `%s` ",appKey, site.getIdentifier()));
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException(String.format("Unable to find site `%s` ", siteId));
            }
        }
        if(exportedSecrets.isEmpty()){
            throw new IllegalArgumentException("Unable to collect any secrets for the given params. The result would be an empty file.");
        }
        return new AppsSecretsImportExport(exportedSecrets);
    }



    /**
     * {@inheritDoc}
     * @param incomingFile encrypted file
     * @param key security key
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    public int importSecretsAndSave(final Path incomingFile, final Key key, final User user)
            throws DotDataException, DotSecurityException, IOException {

        if(!user.isAdmin()){
            throw new DotSecurityException("Only Admins are allowed to perform an export operation.");
        }

        if(!licenseValiditySupplier.hasValidLicense()){
            throw new InvalidLicenseException("Apps requires of an enterprise level license.");
        }

        final String failSilentlyMessage = "These exceptions can be ignored by setting the property `APPS_IMPORT_FAIL_SILENTLY` to true.";
        final boolean failSilently = Config.getBooleanProperty(APPS_IMPORT_FAIL_SILENTLY, false);
        int count = 0;
        final Map<String, List<AppSecrets>> importedSecretsBySiteId = importSecrets(incomingFile, key);
        Logger.info(AppsAPIImpl.class,
                "Number of secrets found: " + importedSecretsBySiteId.size());
        for (final Entry<String, List<AppSecrets>> importEntry : importedSecretsBySiteId
                .entrySet()) {
            final String siteId = importEntry.getKey();
            final Host site = Host.SYSTEM_HOST.equalsIgnoreCase(siteId) ? hostAPI.findSystemHost() : hostAPI.find(siteId, user, false);
            if (null == site) {
                if (failSilently) {
                    Logger.warn(AppsAPIImpl.class, () -> String
                            .format("No site identified by `%s` was found locally.", siteId));
                    continue;
                }
                throw new IllegalArgumentException(
                        String.format("No site identified by `%s` was found locally.\n %s", siteId,
                                failSilentlyMessage));
            }

            for (final AppSecrets appSecrets : importEntry.getValue()) {
                final Optional<AppDescriptor> appDescriptor = getAppDescriptor(appSecrets.getKey(),
                        user);
                if (appDescriptor.isEmpty()) {
                    if (failSilently) {
                        Logger.warn(AppsAPIImpl.class, () -> String
                                .format("No App Descriptor `%s` was found locally.",
                                        appSecrets.getKey()));
                        continue;
                    }
                    throw new IllegalArgumentException(
                            String.format("No App Descriptor `%s` was found locally.\n %s",
                                    appSecrets.getKey(), failSilentlyMessage));
                }

                if (appSecrets.getSecrets().isEmpty()) {
                    if (failSilently) {
                        Logger.warn(AppsAPIImpl.class, () -> String
                                .format("Incoming empty secret `%s` will be skipped.",
                                        appSecrets.getKey()));
                        continue;
                    }
                    throw new IllegalArgumentException(
                            String.format("Incoming empty secret `%s` could replace local copy.\n %s ",
                                    appSecrets.getKey(), failSilentlyMessage));
                }
                try {
                    validateForSave(mapForValidation(appSecrets), appDescriptor.get(), Optional.empty());
                } catch (IllegalArgumentException ae) {
                    if (failSilently) {
                        Logger.warn(AppsAPIImpl.class, () -> String
                                .format("Incoming secret `%s` has validation issues with local descriptor will be skipped.",
                                        appSecrets.getKey()));
                        continue;
                    }
                    throw new IllegalArgumentException(
                            String.format(
                                    "Incoming secret `%s` has validation issues with local descriptor.\n %s ",
                                    appSecrets.getKey(), failSilentlyMessage),
                            ae);
                }
                Logger.info(AppsAPIImpl.class, String.format("Imported secret `%s` ", appSecrets));
                saveSecrets(appSecrets, site, user);
                count++;
            }
        }
        if(count >= 1){
          appsCache.flushSecret();
        }
        return count;
    }

}
