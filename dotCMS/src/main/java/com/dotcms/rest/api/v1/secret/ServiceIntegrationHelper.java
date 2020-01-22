package com.dotcms.rest.api.v1.secret;

import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationDetailedView;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationSiteView;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationView;
import com.dotcms.rest.api.v1.secret.view.SiteView;
import com.dotcms.security.secret.Param;
import com.dotcms.security.secret.Secret;
import com.dotcms.security.secret.ServiceDescriptor;
import com.dotcms.security.secret.ServiceIntegrationAPI;
import com.dotcms.security.secret.ServiceSecrets;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

class ServiceIntegrationHelper {

    private final ServiceIntegrationAPI serviceIntegrationAPI;
    private final HostAPI hostAPI;

    @VisibleForTesting
    ServiceIntegrationHelper(
            final ServiceIntegrationAPI serviceIntegrationAPI, final HostAPI hostAPI) {
        this.serviceIntegrationAPI = serviceIntegrationAPI;
        this.hostAPI = hostAPI;
    }

    ServiceIntegrationHelper() {
        this(APILocator.getServiceIntegrationAPI(), APILocator.getHostAPI());
    }

    private static Comparator<ServiceIntegrationView> compareByNameAndConfiguration = Comparator
            .comparing(ServiceIntegrationView::getName).thenComparing(ServiceIntegrationView::getConfigurationsCount);

    /**
     * This will give you the whole list of service descriptors.
     * @param user Logged in user
     * @return a list of views.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    List<ServiceIntegrationView> getAvailableDescriptorViews(final User user)
            throws DotSecurityException, DotDataException {
        final List<ServiceIntegrationView> views = new ArrayList<>();
        final List<ServiceDescriptor> serviceDescriptors = serviceIntegrationAPI.getServiceDescriptors(user);
        final List<Host> hosts = getHosts(user);
        for (final ServiceDescriptor serviceDescriptor : serviceDescriptors) {
            final String serviceKey = serviceDescriptor.getKey();
            final long configurationsCount = computeSitesWithConfigurations(serviceKey, hosts, user).size();
            views.add(new ServiceIntegrationView(serviceDescriptor, configurationsCount));
        }
        return views.stream().sorted(compareByNameAndConfiguration).collect(CollectionsUtils.toImmutableList());
    }

    /**
     * This gets you a view composed of the service-key and all the hosts that have configurations
     * @param serviceKey unique service id for the given host.
     * @param user Logged in user
     * @return view
     * @throws DotSecurityException
     * @throws DotDataException
     */
    Optional<ServiceIntegrationSiteView> getServiceIntegrationSiteView(final String serviceKey,
            final User user)
            throws DotSecurityException, DotDataException {

        final Optional<ServiceDescriptor> serviceDescriptorOptional = serviceIntegrationAPI
                .getServiceDescriptor(serviceKey, user);
        if (serviceDescriptorOptional.isPresent()) {
            final ImmutableList.Builder<SiteView> hostViewBuilder = new ImmutableList.Builder<>();

            final ServiceDescriptor serviceDescriptor = serviceDescriptorOptional.get();
            final List<Host> hosts = getHosts(user);
            final List<Host> hostsWithConfigurations = computeSitesWithConfigurations(serviceKey, hosts, user);
            for (final Host host : hostsWithConfigurations) {
                hostViewBuilder.add(new SiteView(host.getIdentifier(), host.getHostname()));
            }
            return Optional.of(
               new ServiceIntegrationSiteView(
                  new ServiceIntegrationView(serviceDescriptor, hostsWithConfigurations.size()),
                  hostViewBuilder.build()
               )
            );
        }
        return Optional.empty();
    }

    /**
     * This gives you a detailed view with all the configuration secrets for a given service-key host pair.
     * @param serviceKey unique service id for the given host.
     * @param siteId Host Id
     * @param user Logged in user
     * @return view
     * @throws DotSecurityException
     * @throws DotDataException
     */
    Optional<ServiceIntegrationDetailedView> getServiceIntegrationSiteDetailedView(
            final String serviceKey, final String siteId,
            final User user)
            throws DotSecurityException, DotDataException {
        final Optional<ServiceDescriptor> serviceDescriptorOptional = serviceIntegrationAPI
                .getServiceDescriptor(serviceKey, user);
        if (serviceDescriptorOptional.isPresent()) {
            final ServiceDescriptor serviceDescriptor = serviceDescriptorOptional.get();
            final Host host = hostAPI.find(siteId, user, false);
            if (null == host) {
                throw new DotDataException(
                        String.format(" Couldn't find any host with identifier `%s` ", siteId));
            }
            final SiteView hostView = new SiteView(host.getIdentifier(), host.getHostname());
            final Optional<ServiceSecrets> optionalServiceSecrets = serviceIntegrationAPI
                    .getSecrets(serviceKey, host, user);
            if (optionalServiceSecrets.isPresent()) {
                final ServiceSecrets serviceSecrets = protectHiddenSecrets(
                        optionalServiceSecrets.get());
                return Optional.of(new ServiceIntegrationDetailedView(
                        new ServiceIntegrationView(serviceDescriptor, 1L),
                        hostView, serviceSecrets));
            }
        }
        return Optional.empty();
    }

    /**
     * This will remove all the secrets under a service-integration for a given host.
     * @param serviceKey unique service id for the given host.
     * @param siteId Host Id
     * @param user Logged in user
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void deleteServiceIntegrationSecrets(
            final String serviceKey, final String siteId,
            final User user)
            throws DotSecurityException, DotDataException {
        final Optional<ServiceDescriptor> serviceDescriptorOptional = serviceIntegrationAPI
                .getServiceDescriptor(serviceKey, user);
        if (!serviceDescriptorOptional.isPresent()) {
            throw new DotDataException(
                    String.format(" Couldn't find a descriptor under key `%s` for host `%s` ",
                            serviceKey, siteId));
        }
        final Host host = hostAPI.find(siteId, user, false);
        if (null == host) {
            throw new DotDataException(
                    String.format(" Couldn't find any site with identifier `%s` ", siteId));
        }
        serviceIntegrationAPI.deleteSecrets(serviceKey, host, user);
    }

    /**
     * This will tell you all the different integrations for a given serviceKey.
     * If the service key is used in a given host the host will come back in the resulting list.
     * Otherwise it means no configurations exist for the given host.
     * @param serviceKey unique service id for the given host.
     * @param sites a list of host
     * @param user Logged in user
     * @return a list where the service-key is present (a Configuration exist for the given host)
     */
    private List<Host> computeSitesWithConfigurations(final String serviceKey, final List<Host> sites, final User user){
        return sites.stream().filter(host -> {
            try {
                return serviceIntegrationAPI.hasAnySecrets(serviceKey, host, user);
            } catch (DotDataException | DotSecurityException e) {
                Logger.error(ServiceIntegrationHelper.class,
                        String.format("Error getting secret from `%s` ", serviceKey), e);
            }
            return false;
        }).collect(Collectors.toList());
    }

    private List<Host> getHosts(final User user) throws DotSecurityException, DotDataException {
        final Builder<Host> builder = ImmutableList.builder();
        final Set<String> hostIds = serviceIntegrationAPI.serviceKeysByHost().keySet();
        for(final String hostId:hostIds) {
            builder.add(hostAPI.find(hostId, user, false));
        }
        return builder.build();
    }

    /**
     * Save/Create a secret for the given info on the from
     * @param form Secret specific-form
     * @param user Logged in user.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void saveUpdateSecret(final SecretForm form, final User user)
            throws DotSecurityException, DotDataException {

        final String serviceKey = form.getServiceKey();
        if (!UtilMethods.isSet(serviceKey)) {
            throw new DotDataException("Required param serviceKey isn't set.");
        }
        final String siteId = form.getSiteId();
        if (!UtilMethods.isSet(siteId)) {
            throw new DotDataException("Required Param siteId isn't set.");
        }
        final Host host = hostAPI.find(siteId, user, false);
        if(null == host) {
            throw new DotDataException(String.format(" Couldn't find any host with identifier `%s` ",siteId));
        }
        final Optional<ServiceDescriptor> optionalServiceDescriptor = serviceIntegrationAPI
                .getServiceDescriptor(serviceKey, user);
        if (!optionalServiceDescriptor.isPresent()) {
            throw new DotDataException(  String.format("Unable to find a service descriptor bound to the  serviceKey `%s`. You must upload a yml descriptor.",serviceKey));
        }
        final Map<String, Param> params = form.getParams();
        if(!UtilMethods.isSet(params)){
            throw new DotDataException("Required Params aren't set.");
        }
        final ServiceDescriptor serviceDescriptor = optionalServiceDescriptor.get();
        validateIncomingParams(params, serviceDescriptor);

        final Optional<ServiceSecrets> serviceSecretsOptional = serviceIntegrationAPI
                .getSecrets(serviceKey, host, user);
        if (!serviceSecretsOptional.isPresent()) {
            //Create a brand new secret for the present service
            final ServiceSecrets.Builder builder = new ServiceSecrets.Builder();
            builder.withServiceKey(serviceKey);
            for (final Entry<String, Param> stringParamEntry : params.entrySet()) {
                final String name = stringParamEntry.getKey();
                final Param param = stringParamEntry.getValue();
                final Secret secret = Secret.newSecret(param.getValue().toCharArray(), param.getType(), param.isHidden());
                builder.withSecret(name, secret);
            }
            serviceIntegrationAPI.saveSecrets(builder.build(), host, user);
        } else {
           //Update individual secrets/properties.
            for (final Entry<String, Param> stringParamEntry : params.entrySet()) {
                final String name = stringParamEntry.getKey();
                final Param param = stringParamEntry.getValue();
                final Secret secret = Secret.newSecret(param.getValue().toCharArray(), param.getType(), param.isHidden());
                serviceIntegrationAPI.saveSecret(serviceKey, new Tuple2<>(name, secret), host, user);
            }
        }
    }

    /**
     * This method allows deleting a single secret/property from a stored integration.
     * @param form Secret specific-form
     * @param user Logged in user.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void deleteSecret(final DeleteSecretForm form, final User user)
            throws DotSecurityException, DotDataException {

        final String serviceKey = form.getServiceKey();
        if (!UtilMethods.isSet(serviceKey)) {
            throw new DotDataException("Required param serviceKey isn't set.");
        }
        final String siteId = form.getSiteId();
        if (!UtilMethods.isSet(siteId)) {
            throw new DotDataException("Required Param siteId isn't set.");
        }
        final Host host = hostAPI.find(siteId, user, false);
        if(null == host) {
            throw new DotDataException(String.format(" Couldn't find any site with identifier `%s` ",siteId));
        }
        final Optional<ServiceDescriptor> optionalServiceDescriptor = serviceIntegrationAPI
                .getServiceDescriptor(serviceKey, user);
        if (!optionalServiceDescriptor.isPresent()) {
            throw new DotDataException(String.format("Unable to find a service descriptor bound to the  serviceKey `%s`. You must upload a yml descriptor.",serviceKey));
        }
        final Set<String> params = form.getParams();
        if(!UtilMethods.isSet(params)){
            throw new DotDataException("Required Params aren't set.");
        }
        final ServiceDescriptor serviceDescriptor = optionalServiceDescriptor.get();
        validateIncomingParams(params, serviceDescriptor);

        final Optional<ServiceSecrets> serviceSecretsOptional = serviceIntegrationAPI
                .getSecrets(serviceKey, host, user);
        if (!serviceSecretsOptional.isPresent()) {
            throw new DotDataException(String.format("Unable to find a secret for serviceKey `%s`.",serviceKey));
        } else {
            serviceIntegrationAPI.deleteSecret(serviceKey, params, host, user);
        }
    }

    /**
     * Validate the incoming params match the params described by a serviceDescriptor yml.
     * @param incomingParams a set of paramNames
     * @param serviceDescriptor the service template
     * @throws DotDataException This will give bac an exception if you send an invalid param.
     */
    private void validateIncomingParams(final Map<String, Param> incomingParams, final ServiceDescriptor serviceDescriptor)
            throws DotDataException {

        //Param/Property names are case sensitive.
        final Map<String, Param> serviceDescriptorParams = serviceDescriptor.getParams();
        for (final Entry<String, Param> incomingParamEntry : incomingParams.entrySet()) {
            final String incomingParamName = incomingParamEntry.getKey();
            final Param describedParam = serviceDescriptorParams.get(incomingParamName);
            if(serviceDescriptor.isAllowExtraParameters() && null == describedParam){
               //if the param isn't found in our description but the allow extra params flag is true we're ok
               continue;
            }
            //If the flag isn't true. Then we must reject the unknown param.
            if(null == describedParam) {
                throw new DotDataException(String.format(
                        "Params named `%s` can not be matched against service descriptor. ",
                        incomingParamName));
            }

            final Param incomingParam = incomingParamEntry.getValue();
            //We revise the incoming param against the definition loaded from the yml.
            if(describedParam.isRequired() && UtilMethods.isNotSet(incomingParam.getValue())){
               throw new DotDataException(String.format("Params named `%s` is marked as required in the descriptor but does not have any value.", incomingParamName));
            }
        }
    }

    /**
     * Validate the incoming param names match the params described by a serviceDescriptor yml.
     * This is mostly useful to validate a delete param request
     * @param incomingParamNames
     * @param serviceDescriptor
     * @throws DotDataException
     */
    private void validateIncomingParams(final Set<String> incomingParamNames, final ServiceDescriptor serviceDescriptor)
            throws DotDataException {

        //Param/Property names are case sensitive.
        final Map<String, Param> serviceDescriptorParams = serviceDescriptor.getParams();
        for (final String incomingParamName : incomingParamNames) {

            final Param describedParam = serviceDescriptorParams.get(incomingParamName);
            if(serviceDescriptor.isAllowExtraParameters() && null == describedParam){
                //if the param isn't found in our description but the allow extra params flag is true we're ok
                continue;
            }
            //If the flag isn't true. Then we must reject the unknown param.
            if(null == describedParam) {
                throw new DotDataException(String.format(
                        "Params named `%s` can not be matched against service descriptor. ",
                        incomingParamName));
            }
        }
    }

    /**
     * This is used to manipulate a FormDataMultiPart and extract all the files it might contain
     * Internally the file os analyzed and then processed to create new file integrations
     * @param multipart The multi-part form
     * @param user Logged in dude.
     * @throws IOException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void createServiceIntegration(final FormDataMultiPart multipart, final User user)
            throws IOException, DotSecurityException, DotDataException {
        final List<File> files = new MultiPartUtils().getBinariesFromMultipart(multipart);
        if(!UtilMethods.isSet(files)){
            throw new DotDataException("Unable to extract any files from multi-part request.");
        }
        for (final File file : files) {
            //TODO: verify file length and kit it back if exceeds a max
            serviceIntegrationAPI.createServiceDescriptor(Files.newInputStream(Paths.get(file.getPath())), user);
        }
    }

    /**
     *
     * @param serviceKey
     * @param user
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void deleteServiceDescriptor(final String serviceKey, final User user)
            throws DotSecurityException, DotDataException {
        serviceIntegrationAPI.removeServiceDescriptor(serviceKey, user);
    }

    @VisibleForTesting
    static final String PROTECTED_HIDDEN_SECRET = "*****";

    /**
     * Hidden secrets should never be exposed so this will replace the secret values with anything
     * @param serviceSecrets
     * @return
     */
    private ServiceSecrets protectHiddenSecrets(final ServiceSecrets serviceSecrets){
        final ServiceSecrets.Builder builder = new ServiceSecrets.Builder();
        builder.withServiceKey(serviceSecrets.getServiceKey());
        final Map<String,Secret> sourceSecrets = serviceSecrets.getSecrets();
        for (final Entry<String, Secret> secretEntry : sourceSecrets.entrySet()) {
            if(secretEntry.getValue().isHidden()){
                builder.withHiddenSecret(secretEntry.getKey(), PROTECTED_HIDDEN_SECRET);
            } else {
                builder.withSecret(secretEntry.getKey(),secretEntry.getValue());
            }
        }
        return builder.build();
    }

}
