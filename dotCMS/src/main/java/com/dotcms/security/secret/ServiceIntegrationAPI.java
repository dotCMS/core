package com.dotcms.security.secret;

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
 * Entry Point to manage secrets and third party service integrations
 */
public interface ServiceIntegrationAPI {

    /**
     * Given an individual host a list with the associated services is returned.
     * @param host
     * @param user
     * @return
     */
    List<String> listServiceKeys(Host host, User user) throws DotDataException, DotSecurityException;

    /**
     * Conforms a map where the Elements are lists of service unique names, organized by host as key.
     * @return
     */
    Map<String, Set<String>> serviceKeysByHost();

    /**
     * This returns a json object read from the secret store that contains the service integration configuration and secret.
     * @param serviceKey the unique service identifier
     * @param host the host for the respective service key
     * @param user logged in user
     * @return
     */
     Optional<ServiceSecrets> getSecrets(String serviceKey,
             Host host, User user) throws DotDataException, DotSecurityException;

    /**
     * This returns a json object read from the secret store that contains the service integration configuration and secret
     * This method allows an additional hit against systemHost in case the secret isn't found under the given host
     * The key is a combination of service + host.
     * @param serviceKey the unique service identifier
     * @param fallbackOnSystemHost this param allows  an additional try against system host
     * @param host the host for the respective service key
     * @param user logged in user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<ServiceSecrets> getSecrets(String serviceKey,
            boolean fallbackOnSystemHost,
            Host host, User user) throws DotDataException, DotSecurityException;

   /**
    * This will tell you all the different integrations for a given serviceKey.
    * If the service key is used in a given host the host will come back in the resulting list.
    * Otherwise it means no configurations exist for the given host.
    * @param serviceKey unique service id for the given host.
    * @param siteIdentifiers a list of host identifiers
    * @param user Logged in user
    * @return a list where the service-key is present (a Configuration exist for the given host)
    */
    Set<String> filterSitesForServiceKey(final String serviceKey, final Collection<String> siteIdentifiers, final User user);

    /**
     *
     * @param user
     * @return
     */
    List<Host> getSitesWithIntegrations(final User user);

    /**
     * Lookup for an individual secret/property then updates the single entry.
     * @param serviceKey Service unique id.
     * @param keyAndSecret Tuple value Pair with the definition of the secret and name.
     * @param host The host owning the secret.
     * @param user logged-in user
     */
     void saveSecret(String serviceKey, Tuple2<String,Secret> keyAndSecret, Host host, User user)
             throws DotDataException, DotSecurityException;

    /**
     * Creates or replaces an existing service set of secrets.
     * When calling this the Whole secret gets replaced.
     * @param secrets Secrets info bean.
     * @param host The host owning the secret.
     */
    void saveSecrets(ServiceSecrets secrets, Host host, User user)
            throws DotDataException, DotSecurityException;

    /**
     * Lookup for an individual secret/property then removes the single entry.
     * @param serviceKey Service unique id.
     * @param propSecretNames Individual secret or property name.
     * @param host The host owning the secret.
     * @param user logged-in user
     */
    void deleteSecret(String serviceKey, Set<String> propSecretNames, Host host, User user)
    throws DotDataException, DotSecurityException;

    /**
     * Deletes all secretes associated with the serviceKey and Host.
     * @param serviceKey service unique id.
     * @param host The host owning the secret.
     */
    void deleteSecrets(String serviceKey, Host host, User user)
            throws DotDataException, DotSecurityException;

    /**
     * This method should read the yml file service definition
     * @return
     */
    List<ServiceDescriptor> getServiceDescriptors(User user)
            throws DotDataException, DotSecurityException;

    /**
     *
     * @param serviceKey
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<ServiceDescriptor> getServiceDescriptor(final String serviceKey, final User user)
            throws DotDataException, DotSecurityException;

    /**
     *
     * @param inputStream
     * @param user
     * @throws IOException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void createServiceDescriptor(final InputStream inputStream,
            User user) throws IOException, DotDataException, DotSecurityException;

    /**
     *
     * @param serviceKey
     * @param user
     * @param removeDescriptor
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void removeServiceIntegration(final String serviceKey, final User user,
            final boolean removeDescriptor)
            throws DotSecurityException, DotDataException;

    enum INSTANCE {
        INSTANCE;
        private final ServiceIntegrationAPI integrationAPI = loadSecretsApi();

        public static ServiceIntegrationAPI get() {
            return INSTANCE.integrationAPI;
        }

        private static ServiceIntegrationAPI loadSecretsApi() {
            return (ServiceIntegrationAPI) Try.of(() -> Class
                    .forName(Config.getStringProperty("SERVICE_INTEGRATION_API_IMPL",
                            ServiceIntegrationAPIImpl.class.getCanonicalName()))
                    .newInstance()).getOrNull();

        }
    }

}
