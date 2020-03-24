package com.dotcms.rest.api.v1.apps;

import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.apps.view.AppView;
import com.dotcms.rest.api.v1.apps.view.SiteView;
import com.dotcms.security.apps.Param;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

/**
 * Bridge class that encapsulates the logic necessary to consume the appsAPI
 * and forward it to AppsResource
 */
class AppsHelper {

    private final AppsAPI appsAPI;
    private final HostAPI hostAPI;
    private final ContentletAPI contentletAPI;

    @VisibleForTesting
    AppsHelper(
            final AppsAPI appsAPI, final HostAPI hostAPI, final ContentletAPI contentletAPI) {
        this.appsAPI = appsAPI;
        this.hostAPI = hostAPI;
        this.contentletAPI = contentletAPI;
    }

    AppsHelper() {
        this(APILocator.getAppsAPI(), APILocator.getHostAPI(), APILocator.getContentletAPI());
    }

    private static Comparator<AppView> compareByCountAndName = (o1, o2) -> {
        final int compare = Long.compare(o2.getConfigurationsCount(), o1.getConfigurationsCount());
        if (compare != 0){
            return compare;
        }
        return o1.getName().compareTo(o2.getName());
    };

    /**
     * This will give you the whole list of app descriptors.
     * @param user Logged in user
     * @return a list of views.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    List<AppView> getAvailableDescriptorViews(final User user, final String filter)
            throws DotSecurityException, DotDataException {
        final List<AppView> views = new ArrayList<>();
        List<AppDescriptor> appDescriptors = appsAPI.getAppDescriptors(user);
        if(UtilMethods.isSet(filter)) {
           final String regexFilter = "(.*)"+filter+"(.*)";
           appDescriptors = appDescriptors.stream().filter(appDescriptor -> appDescriptor.getName().matches(regexFilter)).collect(
                   Collectors.toList());
        }
        final Set<String> hostIdentifiers = appsAPI.appKeysByHost().keySet();
        for (final AppDescriptor appDescriptor : appDescriptors) {
            final String appKey = appDescriptor.getKey();
            final long configurationsCount = appsAPI
                    .filterSitesForAppKey(appKey, hostIdentifiers, user).size();
            views.add(new AppView(appDescriptor, configurationsCount));
        }
        return views.stream().sorted(compareByCountAndName).collect(CollectionsUtils.toImmutableList());
    }

    /**
     * This gets you a view composed of the appKey and all the hosts that have configurations. Wrapped within a Response
     * @param request httpRequest
     * @param key unique app id for the given host.
     * @param paginationContext pagination data
     * @param user user Logged in user
     * @return Response
     * @throws DotSecurityException
     * @throws DotDataException
     */
    Response getAppSiteView(
            final HttpServletRequest request,
            final String key,
            final PaginationContext paginationContext,
            final User user)
            throws DotSecurityException, DotDataException {
        final Optional<AppDescriptor> appDescriptorOptional = appsAPI
                .getAppDescriptor(key, user);
        if (!appDescriptorOptional.isPresent()) {
            //Throw exception and allow the mapper do its thing.
            throw new DoesNotExistException(
                    String.format("No App was found for key `%s`. ", key)
            );
        }

        final OrderDirection orderDirection =
                UtilMethods.isSet(paginationContext.getDirection()) ? OrderDirection
                        .valueOf(paginationContext.getDirection()) : OrderDirection.DESC;

        final AppDescriptor appDescriptor = appDescriptorOptional.get();
        final Map<String,Set<String>> appKeysByHost = appsAPI.appKeysByHost();
        final Set<String> sitesWithConfigurations = appsAPI
                .filterSitesForAppKey(appDescriptor.getKey(), appKeysByHost.keySet(), user);

        final PaginationUtil paginationUtil = new PaginationUtil(new SiteViewPaginator(
                () -> sitesWithConfigurations, hostAPI, contentletAPI));
        return paginationUtil
                .getPage(request, user,
                        paginationContext.getFilter(),
                        paginationContext.getPage(),
                        paginationContext.getPerPage(),
                        paginationContext.getOrderBy(),
                        orderDirection,
                        Collections.emptyMap(),
                        (Function<PaginatedArrayList<SiteView>, AppView>) paginatedArrayList -> {
                            final long count = sitesWithConfigurations.size();
                            return new AppView(appDescriptor, count,
                                    paginatedArrayList);
                        });
    }

    /**
     * This gives you a detailed view with all the configuration secrets for a given appKey host pair.
     * @param key unique app id for the given host.
     * @param siteId Host Id
     * @param user Logged in user
     * @return view
     * @throws DotSecurityException
     * @throws DotDataException
     */
    Optional<AppView> getAppSiteDetailedView(
            final String key, final String siteId,
            final User user)
            throws DotSecurityException, DotDataException {
        final Optional<AppDescriptor> appDescriptorOptional = appsAPI
                .getAppDescriptor(key, user);
        if (appDescriptorOptional.isPresent()) {
            final AppDescriptor appDescriptor = appDescriptorOptional.get();
            final Host host = hostAPI.find(siteId, user, false);
            if (null == host) {
                throw new DoesNotExistException(
                        String.format(" Couldn't find any host with identifier `%s` ", siteId));
            }

            final Optional<AppSecrets> optionalAppSecrets = appsAPI
                    .getSecrets(key, true, host, user);
            if (optionalAppSecrets.isPresent()) {
                final AppSecrets appSecrets = protectHiddenSecrets(
                        optionalAppSecrets.get());
                final SiteView siteView = new SiteView(host.getIdentifier(), host.getHostname(),
                        appSecrets.getSecrets());
                return Optional.of(new AppView(appDescriptor, 1L,
                        ImmutableList.of(siteView)));
            }
        }
        return Optional.empty();
    }

    /**
     * This will remove all the secrets under an app for a given host.
     * @param key unique app id for the given host.
     * @param siteId Host Id
     * @param user Logged in user
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void deleteAppSecrets(final String key, final String siteId, final User user)
            throws DotSecurityException, DotDataException {
        final Optional<AppDescriptor> appDescriptorOptional = appsAPI
                .getAppDescriptor(key, user);
        if (!appDescriptorOptional.isPresent()) {
            throw new DoesNotExistException(
                    String.format(" Couldn't find a descriptor under key `%s` for host `%s` ",
                            key, siteId));
        }
        final Host host = hostAPI.find(siteId, user, false);
        if (null == host) {
            throw new DoesNotExistException(
                    String.format(" Couldn't find any site with identifier `%s` ", siteId));
        }
        appsAPI.deleteSecrets(key, host, user);
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

        final String key = form.getKey();
        if (!UtilMethods.isSet(key)) {
            throw new IllegalArgumentException("Required param Key isn't set.");
        }
        final String siteId = form.getSiteId();
        if (!UtilMethods.isSet(siteId)) {
            throw new IllegalArgumentException("Required Param siteId isn't set.");
        }
        final Host host = hostAPI.find(siteId, user, false);
        if(null == host) {
            throw new IllegalArgumentException(String.format(" Couldn't find any host with identifier `%s` ",siteId));
        }
        final Optional<AppDescriptor> optionalAppDescriptor = appsAPI
                .getAppDescriptor(key, user);
        if (!optionalAppDescriptor.isPresent()) {
            throw new DoesNotExistException(  String.format("Unable to find an app descriptor bound to the  Key `%s`. You must upload a yml descriptor.",key));
        }
        final Map<String, Param> params = form.getParams();
        if(!UtilMethods.isSet(params)){
            throw new IllegalArgumentException("Required Params aren't set.");
        }
        final AppDescriptor appDescriptor = optionalAppDescriptor.get();
        validateIncomingParams(params, appDescriptor);

        final Optional<AppSecrets> appSecretsOptional = appsAPI
                .getSecrets(key, host, user);
        if (!appSecretsOptional.isPresent()) {
            //Create a brand new secret for the present app
            final AppSecrets.Builder builder = new AppSecrets.Builder();
            builder.withKey(key);
            for (final Entry<String, Param> stringParamEntry : params.entrySet()) {
                final String name = stringParamEntry.getKey();
                final Param param = stringParamEntry.getValue();
                final Secret secret = Secret.newSecret(param.getValue().toCharArray(), param.getType(), param.isHidden());
                builder.withSecret(name, secret);
            }
            appsAPI.saveSecrets(builder.build(), host, user);
        } else {
           //Update individual secrets/properties.
            for (final Entry<String, Param> stringParamEntry : params.entrySet()) {
                final String name = stringParamEntry.getKey();
                final Param param = stringParamEntry.getValue();
                final Secret secret = Secret.newSecret(param.getValue().toCharArray(), param.getType(), param.isHidden());

                appsAPI.saveSecret(key, Tuple.of(name, secret), host, user);
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

        final String key = form.getKey();
        if (!UtilMethods.isSet(key)) {
            throw new IllegalArgumentException("Required param Key isn't set.");
        }
        final String siteId = form.getSiteId();
        if (!UtilMethods.isSet(siteId)) {
            throw new IllegalArgumentException("Required Param siteId isn't set.");
        }
        final Host host = hostAPI.find(siteId, user, false);
        if(null == host) {
            throw new IllegalArgumentException(String.format(" Couldn't find any site with identifier `%s` ",siteId));
        }
        final Optional<AppDescriptor> optionalAppDescriptor = appsAPI
                .getAppDescriptor(key, user);
        if (!optionalAppDescriptor.isPresent()) {
            throw new DoesNotExistException(String.format("Unable to find an app descriptor bound to the Key `%s`. You must upload a yml descriptor.",key));
        }
        final Set<String> params = form.getParams();
        if(!UtilMethods.isSet(params)){
            throw new IllegalArgumentException("Required Params aren't set.");
        }
        final AppDescriptor appDescriptor = optionalAppDescriptor.get();
        validateIncomingParams(params, appDescriptor);

        final Optional<AppSecrets> appSecretsOptional = appsAPI
                .getSecrets(key, host, user);
        if (!appSecretsOptional.isPresent()) {
            throw new DoesNotExistException(String.format("Unable to find a secret for app with Key `%s`.",key));
        } else {
            appsAPI.deleteSecret(key, params, host, user);
        }
    }

    /**
     * Validate the incoming params match the params described by an appDescriptor yml.
     * @param incomingParams a set of paramNames
     * @param appDescriptor the app template
     * @throws DotDataException This will give bac an exception if you send an invalid param.
     */
    private void validateIncomingParams(final Map<String, Param> incomingParams, final AppDescriptor appDescriptor)
            throws DotDataException {

        //Param/Property names are case sensitive.
        final Map<String, Param> appDescriptorParams = appDescriptor.getParams();
        for (final Entry<String, Param> incomingParamEntry : incomingParams.entrySet()) {
            final String incomingParamName = incomingParamEntry.getKey();
            final Param describedParam = appDescriptorParams.get(incomingParamName);
            if(appDescriptor.isAllowExtraParameters() && null == describedParam){
               //if the param isn't found in our description but the allow extra params flag is true we're ok
               continue;
            }
            //If the flag isn't true. Then we must reject the unknown param.
            if(null == describedParam) {
                throw new IllegalArgumentException(String.format(
                        "Params named `%s` can not be matched against an app descriptor. ",
                        incomingParamName));
            }

            final Param incomingParam = incomingParamEntry.getValue();
            //We revise the incoming param against the definition loaded from the yml.
            if(describedParam.isRequired() && UtilMethods.isNotSet(incomingParam.getValue())){
               throw new IllegalArgumentException(
               String.format("Params named `%s` is marked as required in the descriptor but does not have any value.", incomingParamName));
            }
        }
    }

    /**
     * Validate the incoming param names match the params described by an appDescriptor yml.
     * This is mostly useful to validate a delete param request
     * @param incomingParamNames
     * @param appDescriptor
     * @throws DotDataException
     */
    private void validateIncomingParams(final Set<String> incomingParamNames, final AppDescriptor appDescriptor)
            throws DotDataException {

        //Param/Property names are case sensitive.
        final Map<String, Param> appDescriptorParams = appDescriptor.getParams();
        for (final String incomingParamName : incomingParamNames) {

            final Param describedParam = appDescriptorParams.get(incomingParamName);
            if(appDescriptor.isAllowExtraParameters() && null == describedParam){
                //if the param isn't found in our description but the allow extra params flag is true we're ok
                continue;
            }
            //If the flag isn't true. Then we must reject the unknown param.
            if(null == describedParam) {
                throw new IllegalArgumentException(String.format(
                        "Params named `%s` can not be matched against an app descriptor. ",
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
     * @throws DotDataException
     */
    List<AppView> createApp(final FormDataMultiPart multipart, final User user)
            throws IOException, DotDataException {
        final List<File> files = new MultiPartUtils().getBinariesFromMultipart(multipart);
        if(!UtilMethods.isSet(files)){
            throw new DotDataException("Unable to extract any files from multi-part request.");
        }
        List<AppView> appViews = new ArrayList<>(files.size());
        for (final File file : files) {
            try(final InputStream inputStream = Files.newInputStream(Paths.get(file.getPath()))){
                final AppDescriptor appDescriptor = appsAPI
                        .createAppDescriptor(inputStream, user);
                appViews.add(new AppView(appDescriptor,0L));
            }catch (Exception e){
               Logger.error(AppsHelper.class, e);
               throw new DotDataException(e);
            }
        }
        return appViews;
    }

    /**
     *
     * @param key
     * @param user
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void removeServiceIntegration(final String key, final User user, final boolean removeDescriptor)
            throws DotSecurityException, DotDataException {
        appsAPI.removeApp(key, user, removeDescriptor);
    }

    @VisibleForTesting
    static final String PROTECTED_HIDDEN_SECRET = "*****";

    /**
     * Hidden secrets should never be exposed so this will replace the secret values with anything
     * @param appSecrets
     * @return
     */
    private AppSecrets protectHiddenSecrets(final AppSecrets appSecrets){
        final AppSecrets.Builder builder = new AppSecrets.Builder();
        builder.withKey(appSecrets.getKey());
        final Map<String,Secret> sourceSecrets = appSecrets.getSecrets();
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
