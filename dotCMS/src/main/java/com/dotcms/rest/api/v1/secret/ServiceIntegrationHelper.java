package com.dotcms.rest.api.v1.secret;

import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.rest.api.v1.secret.view.HostView;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationDetailedView;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationHostView;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationView;
import com.dotcms.security.secret.Param;
import com.dotcms.security.secret.Secret;
import com.dotcms.security.secret.ServiceDescriptor;
import com.dotcms.security.secret.ServiceIntegrationAPI;
import com.dotcms.security.secret.ServiceSecrets;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.glassfish.jersey.media.multipart.BodyPart;
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

    List<ServiceIntegrationView> getAvailableDescriptorViews(final User user)
            throws DotSecurityException, DotDataException {
        final ImmutableList.Builder<ServiceIntegrationView> viewsBuilder = new ImmutableList.Builder<>();
        final List<ServiceDescriptor> serviceDescriptors = serviceIntegrationAPI
                .getAvailableServiceDescriptors(user);
        final List<Host> hosts = hostAPI.findAll(user, false);
        for (final ServiceDescriptor serviceDescriptor : serviceDescriptors) {
            final String serviceKey = serviceDescriptor.getServiceKey();
            final long configurationsCount = computeHostsWithConfigurations(serviceKey, hosts, user).size();
            viewsBuilder.add(new ServiceIntegrationView(serviceDescriptor, configurationsCount));
        }
        return viewsBuilder.build();
    }

    Optional<ServiceIntegrationHostView> getServiceIntegrationHostView(final String serviceKey,
            final User user)
            throws DotSecurityException, DotDataException {

        final Optional<ServiceDescriptor> serviceDescriptorOptional = serviceIntegrationAPI
                .getServiceDescriptor(serviceKey, user);
        if (serviceDescriptorOptional.isPresent()) {
            final ImmutableList.Builder<HostView> hostViewBuilder = new ImmutableList.Builder<>();

            final ServiceDescriptor serviceDescriptor = serviceDescriptorOptional.get();
            final List<Host> allAvailableHosts = hostAPI.findAll(user, false);
            final List<Host> hostsWithConfigurations = computeHostsWithConfigurations(serviceKey, allAvailableHosts, user);
            for (final Host host : hostsWithConfigurations) {
                hostViewBuilder.add(new HostView(host.getIdentifier(), host.getHostname()));
            }
            return Optional.of(
               new ServiceIntegrationHostView(
                  new ServiceIntegrationView(serviceDescriptor, hostsWithConfigurations.size()),
                  hostViewBuilder.build()
               )
            );
        }
        return Optional.empty();
    }

    Optional<ServiceIntegrationDetailedView> getServiceIntegrationHostDetailedView(
            final String serviceKey, final String hostId,
            final User user)
            throws DotSecurityException, DotDataException {
        final Optional<ServiceDescriptor> serviceDescriptorOptional = serviceIntegrationAPI
                .getServiceDescriptor(serviceKey, user);
        if (serviceDescriptorOptional.isPresent()) {
            final ServiceDescriptor serviceDescriptor = serviceDescriptorOptional.get();
            final Host host = hostAPI.find(hostId, user, false);
            if(null == host) {
               throw new DotDataException(String.format(" Couldn't find any host with identifier `%s` ",hostId));
            }
            final HostView hostView = new HostView(host.getIdentifier(), host.getHostname());
            final Optional<ServiceSecrets> optionalServiceSecrets = serviceIntegrationAPI.getSecretsForService(serviceKey, host, user);
            if (optionalServiceSecrets.isPresent()) {
                final ServiceSecrets serviceSecrets = optionalServiceSecrets.get();
                return Optional.of(new ServiceIntegrationDetailedView(
                        new ServiceIntegrationView(serviceDescriptor, 1L),
                        hostView, serviceSecrets));
            }
        }
        return Optional.empty();
    }

    private List<Host> computeHostsWithConfigurations(final String serviceKey, final List<Host> hosts, final User user){
        return hosts.stream().filter(host -> {
            try {
                return serviceIntegrationAPI.getSecretsForService(serviceKey, host, user)
                        .isPresent();
            } catch (DotDataException | DotSecurityException e) {
                Logger.error(ServiceIntegrationHelper.class,
                        String.format("Error getting secret from `%s` ", serviceKey), e);
            }
            return false;
        }).collect(Collectors.toList());
    }

    void saveUpdateSecret(final SecretForm form, final User user)
            throws DotSecurityException, DotDataException {

        final String serviceKey = form.getServiceKey();
        if (!UtilMethods.isSet(serviceKey)) {
            throw new DotDataException("Required param serviceKey isn't set.");
        }
        final String hostId = form.getSiteId();
        if (!UtilMethods.isSet(hostId)) {
            throw new DotDataException("Required Param siteId isn't set.");
        }
        final Host host = hostAPI.find(hostId, user, false);
        if(null == host) {
            throw new DotDataException(String.format(" Couldn't find any host with identifier `%s` ",hostId));
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
        validateIncomingParamNames(params.keySet(),serviceDescriptor);

        final Optional<ServiceSecrets> serviceSecretsOptional = serviceIntegrationAPI
                .getSecretsForService(serviceKey, host, user);
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

    private void validateIncomingParamNames(final Set<String> paramNames, final ServiceDescriptor serviceDescriptor)
            throws DotDataException {
        final Map<String, Param> serviceDescriptorParams = serviceDescriptor.getParams();
        if(serviceDescriptorParams.containsKey("*")){
            //If an aster has been specified we can have whatever param name we want added.
           return;
        }
        for (final String paramName : paramNames) {
            if(!serviceDescriptor.getParams().containsKey(paramName)){
                throw new DotDataException(String.format("Params named `%s` can not be matched against service descriptor.",paramName));
            }
        }
    }

    void createServiceIntegration(String serviceKey, final FormDataMultiPart multipart,
            final User user)
            throws IOException, DotSecurityException, DotDataException, JSONException {

        for (final BodyPart bodyPart : multipart.getBodyParts()) {
            final FileInputStream fileInputStream = (FileInputStream)bodyPart.getEntity();
            serviceIntegrationAPI.createServiceDescriptor(serviceKey, fileInputStream, user);
            break;
        }
    }

}
