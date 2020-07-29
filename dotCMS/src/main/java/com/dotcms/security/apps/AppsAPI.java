package com.dotcms.security.apps;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.util.EncryptorException;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.Key;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Entry Point to manage secrets and third party App integrations
 */
public interface AppsAPI {

     static final String APPS_API_IMPL = "APPS_API_IMPL";

    /**
     * Given an individual host a list with the associated App is returned.
     * @param host
     * @param user
     * @return
     */
    List<String> listAppKeys(Host host, User user) throws DotDataException, DotSecurityException;

    /**
     * Conforms a map where the Elements are lists of app unique names, organized by host as key.
     * @return
     */
    Map<String, Set<String>> appKeysByHost() throws DotSecurityException, DotDataException;

    /**
     * This returns a json object read from the secret store that contains the apps integration configuration and secret.
     * @param key the unique app identifier
     * @param host the host for the respective app key
     * @param user logged in user
     * @return
     */
     Optional<AppSecrets> getSecrets(String key,
             Host host, User user) throws DotDataException, DotSecurityException;

    /**
     * This returns a json object read from the secret store that contains the app integration configuration and secret
     * This method allows an additional hit against systemHost in case the secret isn't found under the given host
     * The key is a combination of app + host.
     * @param key the unique app identifier
     * @param fallbackOnSystemHost this param allows  an additional try against system host
     * @param host the host for the respective app key
     * @param user logged in user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<AppSecrets> getSecrets(String key,
            boolean fallbackOnSystemHost,
            Host host, User user) throws DotDataException, DotSecurityException;

   /**
    * This will tell you all the different apps for a given appKey.
    * If the app key is used in a given site the site will come back in the resulting list.
    * Otherwise it means no configurations exist for the given host.
    * @param key unique app id for the given host.
    * @param siteIdentifiers a list of host identifiers
    * @param user Logged in user
    * @return a list where the app-key is present (a Configuration exist for the given host)
    */
    Set<String> filterSitesForAppKey(final String key, final Collection<String> siteIdentifiers, final User user);

    /**
     * Lookup for an individual secret/property then updates the single entry.
     * @param key App unique id.
     * @param keyAndSecret Tuple value Pair with the definition of the secret and name.
     * @param host The host owning the secret.
     * @param user logged-in user
     */
     void saveSecret(String key, Tuple2<String,Secret> keyAndSecret, Host host, User user)
             throws DotDataException, DotSecurityException;

    /**
     * Creates or replaces an existing app set of secrets.
     * When calling this the Whole secret gets replaced.
     * @param secrets Secrets info bean.
     * @param host The host owning the secret.
     */
    void saveSecrets(AppSecrets secrets, Host host, User user)
            throws DotDataException, DotSecurityException;

    /**
     * Lookup for an individual secret/property then removes the single entry.
     * @param key App unique id.
     * @param propSecretNames Individual secret or property name.
     * @param host The host owning the secret.
     * @param user logged-in user
     */
    void deleteSecret(String key, Set<String> propSecretNames, Host host, User user)
    throws DotDataException, DotSecurityException;

    /**
     * Deletes all secretes associated with the key and Host.
     * @param key app unique id.
     * @param host The host owning the secret.
     */
    void deleteSecrets(String key, Host host, User user)
            throws DotDataException, DotSecurityException;

    /**
     * This method should read the yml file app definition
     * @return
     */
    List<AppDescriptor> getAppDescriptors(User user)
            throws DotDataException, DotSecurityException;

    /**
     * Given an app key and the current user this will give back the appDescriptor
     * @param key
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<AppDescriptor> getAppDescriptor(final String key, final User user)
            throws DotDataException, DotSecurityException;

    /**
     * Create an App-Descriptor given an InputStream from a yml file read
     * @param file
     * @param user
     * @throws IOException
     * @throws DotDataException
     * @throws DotSecurityException
     * @return
     */

    AppDescriptor createAppDescriptor(final File file,
            User user) throws DotDataException, AlreadyExistException, DotSecurityException;

    /**
     * Remove an App and all the secrets underneath.
     * @param key
     * @param user
     * @param removeDescriptor
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void removeApp(final String key, final User user,
            final boolean removeDescriptor)
            throws DotSecurityException, DotDataException;

    /**
     * Method meant to to be consumed from a site delete event.
     * @param host
     * @param user
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void removeSecretsForSite(Host host, User user)
                    throws DotDataException, DotSecurityException;
    /**
     * Warnings are any secrets missing required values stated on the AppDescriptor
     * @param appDescriptor
     * @param sitesWithConfigurations
     * @param user
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    Map<String, Map<String, List<String>>> computeWarningsBySite(final AppDescriptor appDescriptor,
            final Set<String> sitesWithConfigurations, final User user)
            throws DotSecurityException, DotDataException;

    /**
     * Warnings are any secrets missing required values stated on the AppDescriptor
     * @param appDescriptor
     * @param site
     * @param user
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    Map<String, List<String>> computeSecretWarnings(final AppDescriptor appDescriptor, final Host site, final User user)
            throws DotSecurityException, DotDataException;


    /**
     * The On secrets key reset is handled down here.
     * @param user
     * @throws DotDataException
     * @throws IOException
     */
    void resetSecrets(User user)
                    throws DotDataException, IOException;

    /**
     *
     * @param key
     * @param exportAll
     * @param appKeysBySite
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    File exportSecrets(final Key key, final boolean exportAll,
            final Map<String, Set<String>> appKeysBySite, final User user)
            throws DotDataException, DotSecurityException, IOException;

    /**
     *
     * @param incomingFile
     * @param key
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     * @throws EncryptorException
     * @throws ClassNotFoundException
     */
    Map<String, List<AppSecrets>> importSecrets(final Path incomingFile, final Key key, final User user)
            throws DotDataException, DotSecurityException, IOException, EncryptorException, ClassNotFoundException;

    enum INSTANCE {
        INSTANCE;
        private final AppsAPI integrationAPI = loadSecretsApi();

        public static AppsAPI get() {
            return INSTANCE.integrationAPI;
        }

        private static AppsAPI loadSecretsApi() {
            return (AppsAPI) Try.of(() -> Class
                    .forName(Config.getStringProperty(APPS_API_IMPL,
                            AppsAPIImpl.class.getCanonicalName()))
                    .newInstance()).getOrNull();

        }
    }

}
