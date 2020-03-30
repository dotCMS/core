package com.dotcms.rest.api.v1.apps;

import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.apps.view.AppView;
import com.dotcms.rest.api.v1.apps.view.SecretView;
import com.dotcms.rest.api.v1.apps.view.SiteView;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.ParamDescriptor;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import jersey.repackaged.com.google.common.collect.Sets;
import jersey.repackaged.com.google.common.collect.Sets.SetView;
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
           final String regexFilter = "(?i:.*)"+filter+"(.*)";
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
                      String.format(" Couldn't find any host with identifier `%s` ", siteId)
                );
            }

            final Optional<AppSecrets> optionalAppSecrets = appsAPI
                    .getSecrets(key, true, host, user);

                //We need to return a view with all the secrets and also descriptors of the remaining parameters merged.
                //So we're gonna need a copy of the params on the yml.
                final Map<String, ParamDescriptor> descriptorParams = new HashMap<>(appDescriptor.getParams());
                //First will process the secrets stored..
                //As we process them we we remove them from the `descriptorParams` map.
                //They're removed from the map as we go on so we know that what's left in the map doesnt have a secret in storage.
                final AppSecrets appSecrets = optionalAppSecrets.isPresent() ? protectHiddenSecrets(optionalAppSecrets.get()) : AppSecrets.empty() ;
                final Set<SecretView> mappedSecrets = appSecrets.getSecrets().entrySet()
                        .stream()
                        .map(e -> new SecretView(e.getKey(),e.getValue(), descriptorParams.remove(e.getKey()))).collect(
                                Collectors.toSet());

                //Now we process the remaining on `descriptorParams`.
                //Transform the DescriptorParams into SecretView
                //What ever is left in there is a param that exist on the yml.
                // that doesnt have a secret in storage.
                final Set<SecretView> mappedDescriptors = appDescriptor.getParams().keySet().stream()
                        .map(
                                paramKey -> new SecretView(paramKey, null,
                                        descriptorParams.remove(paramKey))).collect(
                                Collectors.toSet());

                //At this point `descriptorParams` should be empty.
                assert (descriptorParams.isEmpty());

                //Now we need to present them both.
                //For which we first add the ones from the descriptor.
                final Set<SecretView> merged = new LinkedList<>(mappedDescriptors).stream()
                    .filter(secretView -> !mappedSecrets.contains(secretView)).collect(Collectors.toSet());
                merged.addAll(mappedSecrets);

                final List<SecretView> sorted  = new LinkedList<>(merged);
                sorted.sort((o1, o2) -> Boolean.compare(o1.isDynamic(),o2.isDynamic()));

                final SiteView siteView = new SiteView(host.getIdentifier(), host.getHostname(), sorted);
                return Optional.of(new AppView(appDescriptor, 1L,
                        ImmutableList.of(siteView)));

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
     * Save/Create a secret for the given info on the from.
     * @param form Secret specific-form
     * @param user Logged in user.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void saveSecretForm(final String key, final String siteId, final SecretForm form, final User user)
            throws DotSecurityException, DotDataException {

        if (!UtilMethods.isSet(key)) {
            throw new IllegalArgumentException("Required param Key isn't set.");
        }
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

        final AppDescriptor appDescriptor = optionalAppDescriptor.get();
        try {
            saveSecretForm(key, host, appDescriptor, form, user);
        } finally {
            form.destroySecretTraces();
        }
    }

    /**
     * Save/Create a secret for the given info on the from.
     * @param key
     * @param host
     * @param appDescriptor
     * @param form
     * @param user
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private void saveSecretForm(final String key, final Host host,
            final AppDescriptor appDescriptor, final SecretForm form, final User user) throws DotSecurityException, DotDataException {
        final Map<String, Input> params = validateFormForSave(form, appDescriptor);
        final Optional<AppSecrets> appSecretsOptional = appsAPI.getSecrets(key, host, user);
        if (appSecretsOptional.isPresent()) {
            appsAPI.deleteSecrets(key, host, user);
        }
        //Create a brand new secret for the present app.
        final AppSecrets.Builder builder = new AppSecrets.Builder();
        builder.withKey(key);
        for (final Entry<String, Input> stringParamEntry : params.entrySet()) {
            final String name = stringParamEntry.getKey();
            final ParamDescriptor describedParam = appDescriptor.getParams().get(name);
            final Input inputParam = stringParamEntry.getValue();
            final boolean dynamic = null == describedParam;
            final Secret secret;
            if(dynamic){
                secret = Secret.newSecret(inputParam.getValue(), Type.STRING, inputParam.isHidden());
            } else {
                secret = Secret.newSecret(inputParam.getValue(), describedParam.getType(), describedParam.isHidden());
            }
            builder.withSecret(name, secret);
        }
        appsAPI.saveSecrets(builder.build(), host, user);
    }

    /**
     * This method allows saving a form according to the app definition.
     * @param key
     * @param siteId
     * @param form
     * @param user
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void saveUpdateSecrets(final String key, final String siteId, final SecretForm form, final User user)
            throws DotSecurityException, DotDataException {
        if (!UtilMethods.isSet(key)) {
            throw new IllegalArgumentException("Required param Key isn't set.");
        }
        if (!UtilMethods.isSet(siteId)) {
            throw new IllegalArgumentException("Required Param siteId isn't set.");
        }
        final Host host = hostAPI.find(siteId, user, false);
        if(null == host) {
            throw new IllegalArgumentException(String.format(" Couldn't find any host with identifier `%s` ",siteId));
        }
        final Optional<AppDescriptor> optionalAppDescriptor = appsAPI.getAppDescriptor(key, user);
        if (!optionalAppDescriptor.isPresent()) {
            throw new DoesNotExistException(  String.format("Unable to find an app descriptor bound to the  Key `%s`. You must upload a yml descriptor.",key));
        }
        final AppDescriptor appDescriptor = optionalAppDescriptor.get();
        final Optional<AppSecrets> appSecretsOptional = appsAPI.getSecrets(key, host, user);
        if (!appSecretsOptional.isPresent()) {
            saveSecretForm(key, host, appDescriptor, form, user);
        } else {
            try {
                final Map<String, Input> params = validateFormForUpdate(form, appDescriptor);
                //Update individual secrets/properties.
                for (final Entry<String, Input> stringParamEntry : params.entrySet()) {
                    final String name = stringParamEntry.getKey();
                    final ParamDescriptor describedParam = appDescriptor.getParams().get(name);
                    final Input inputParam = stringParamEntry.getValue();
                    final boolean dynamic = null == describedParam;
                    final Secret secret;
                    if (dynamic) {
                        secret = Secret.newSecret(inputParam.getValue(), Type.STRING,
                                inputParam.isHidden());
                    } else {
                        secret = Secret.newSecret(inputParam.getValue(), describedParam.getType(),
                                describedParam.isHidden());
                    }
                    appsAPI.saveSecret(key, Tuple.of(name, secret), host, user);
                }
            }finally {
                form.destroySecretTraces();
            }
        }
    }

    /**
     * This method allows deleting a single secret/property from a stored integration.
     * TODO: if a required property/secret is deleted.. the app must enter into some sort of invalid state.
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
        validateFormForDelete(params, appDescriptor);

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
     * This validation is intended to behave as a form validation. It'll make sure that all required values are present at save time.
     * And nothing else besides the params described are allowed. Unless they app-desciptor establishes that extraParams are allowed.
     * @param form a set of paramNames
     * @param appDescriptor the app template
     * @throws DotDataException This will give bac an exception if you send an invalid param.
     */
    private  Map<String, Input> validateFormForSave(final SecretForm form,
            final AppDescriptor appDescriptor)
            throws IllegalArgumentException {

        final Map<String, Input> params = form.getInputParams();
        if(!UtilMethods.isSet(params)){
            throw new IllegalArgumentException("Required Params aren't set.");
        }

        //Param/Property names are case sensitive.
        final Map<String, ParamDescriptor> appDescriptorParams = appDescriptor.getParams();

        for (final Entry<String, ParamDescriptor> appDescriptorParam : appDescriptorParams.entrySet()) {
            final String describedParamName = appDescriptorParam.getKey();
            final Input input = params.get(describedParamName);
            if (appDescriptorParam.getValue().isRequired() && (input == null || UtilMethods.isNotSet(input.getValue()))) {
                throw new IllegalArgumentException(
                    String.format(
                        "Param `%s` is marked required in the descriptor but does not come with a value.",
                        describedParamName
                    )
                );
            }
        }

        if (!appDescriptor.isAllowExtraParameters()) {
            final SetView<String> extraParamsFound = Sets
                    .difference(params.keySet(), appDescriptorParams.keySet());

            if (!extraParamsFound.isEmpty()) {
                throw new IllegalArgumentException(
                    String.format(
                        "Unknown additional params `%s` not allowed by the app descriptor.",
                        String.join(", ", extraParamsFound)
                    )
                );
            }
        }
        return params;
    }

    /**
     * This method is meant to validate inputs for an update that can be performed on individual properties.
     * It assumes there's an instance already saved and only performs validations on the new incoming params.
     * This gives the flexibility to modify the value on individual properties that are already saved.
     * If the app
     * We're not expecting
     * @param form
     * @param appDescriptor
     * @return
     * @throws IllegalArgumentException
     */
    private Map<String, Input> validateFormForUpdate(final SecretForm form,
            final AppDescriptor appDescriptor)
            throws IllegalArgumentException {

        final Map<String, Input> params = form.getInputParams();
        if (!UtilMethods.isSet(params)) {
            throw new IllegalArgumentException("Required Params aren't set.");
        }

        //Param/Property names are case sensitive.
        final Map<String, ParamDescriptor> appDescriptorParams = appDescriptor.getParams();
        for (final Entry<String, Input> entry : params.entrySet()) {
            final String paraName = entry.getKey();
            final ParamDescriptor paramDescriptor = appDescriptorParams.get(paraName);
            if (null == paramDescriptor && !appDescriptor.isAllowExtraParameters()) {
                throw new IllegalArgumentException(String.format(
                        "Unknown additional Param `%s` not allowed by the app descriptor.",
                        paraName));
            } else {
                if (null != paramDescriptor && paramDescriptor.isRequired() && null != entry
                        .getValue() && UtilMethods.isNotSet(entry.getValue().getValue())) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Param `%s` is marked required in the descriptor but does not come with a value.",
                                    paraName
                            )
                    );
                }
            }
        }

        return params;
    }

    /**
     * Validate the incoming param names match the params described by an appDescriptor yml.
     * This is mostly useful to validate a delete param request
     * @param inputParamNames
     * @param appDescriptor
     * @throws DotDataException
     */
    private void validateFormForDelete(final Set<String> inputParamNames, final AppDescriptor appDescriptor)
            throws IllegalArgumentException {

        //Param/Property names are case sensitive.
        final Map<String, ParamDescriptor> appDescriptorParams = appDescriptor.getParams();
        for (final String inputParamName : inputParamNames) {

            final ParamDescriptor describedParam = appDescriptorParams.get(inputParamName);
            if(appDescriptor.isAllowExtraParameters() && null == describedParam){
                //if the param isn't found in our description but the allow extra params flag is true we're ok
                continue;
            }
            //If the flag isn't true. Then we must reject the unknown param.
            if(null == describedParam) {
                throw new IllegalArgumentException(String.format(
                        "Params named `%s` can not be matched against the app descriptor. ",
                        inputParamName));
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
    void removeApp(final String key, final User user, final boolean removeDescriptor)
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
