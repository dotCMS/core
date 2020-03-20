package com.dotcms.security.apps;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.IOException;
import java.io.InputStream;
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
    Map<String, Set<String>> appKeysByHost();

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
    * This will tell you all the different integrations for a given appKey.
    * If the app key is used in a given host the host will come back in the resulting list.
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
     *
     * @param key
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<AppDescriptor> getAppDescriptor(final String key, final User user)
            throws DotDataException, DotSecurityException;

    /**
     *
     * @param inputStream
     * @param user
     * @throws IOException
     * @throws DotDataException
     * @throws DotSecurityException
     * @return
     */
    AppDescriptor createAppDescriptor(final InputStream inputStream,
            User user) throws IOException, DotDataException, DotSecurityException;

    /**
     *
     * @param key
     * @param user
     * @param removeDescriptor
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void removeApp(final String key, final User user,
            final boolean removeDescriptor)
            throws DotSecurityException, DotDataException;

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
