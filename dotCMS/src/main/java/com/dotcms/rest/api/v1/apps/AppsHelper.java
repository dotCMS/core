package com.dotcms.rest.api.v1.apps;

import static com.dotmarketing.util.UtilMethods.isNotSet;

import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.apps.view.AppView;
import com.dotcms.rest.api.v1.apps.view.SecretView;
import com.dotcms.rest.api.v1.apps.view.SiteView;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.AppsUtil;
import com.dotcms.security.apps.ParamDescriptor;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.EncryptorException;
import io.vavr.Tuple;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
    private final PermissionAPI permissionAPI;

    private final SecurityLoggerServiceAPI securityLoggerAPI;

    @VisibleForTesting
    AppsHelper(
            final AppsAPI appsAPI, final HostAPI hostAPI, final PermissionAPI permissionAPI, final SecurityLoggerServiceAPI securityLoggerAPI) {
        this.appsAPI = appsAPI;
        this.hostAPI = hostAPI;
        this.permissionAPI = permissionAPI;
        this.securityLoggerAPI = securityLoggerAPI;
    }

    AppsHelper() {
        this(APILocator.getAppsAPI(), APILocator.getHostAPI(), APILocator.getPermissionAPI(), APILocator.getSecurityLogger());
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
            final String regexFilter = "(?i).*"+filter+"(.*)";
            appDescriptors = appDescriptors.stream().filter(appDescriptor -> appDescriptor.getName().matches(regexFilter)).collect(
                    Collectors.toList());
        }
        final Set<String> siteIdentifiers = appsAPI.appKeysByHost().keySet();
        for (final AppDescriptor appDescriptor : appDescriptors) {
            final String appKey = appDescriptor.getKey();
            final int configurationsCount = appsAPI.filterSitesForAppKey(appKey, siteIdentifiers, user).size();
            final int sitesWithWarning = computeWarningsBySite(appDescriptor, siteIdentifiers, user);
            views.add(new AppView(appDescriptor, configurationsCount, sitesWithWarning));
        }
        return views.stream().sorted(compareByCountAndName).collect(CollectionsUtils.toImmutableList());
    }

    /**
     * Computes the number of warnings regardless of site under the given app-descriptor
     * @param appDescriptor
     * @param sitesWithConfigurations
     * @param user
     * @return sum or warnings for the given app-descriptor. Regardless of site.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private int computeWarningsBySite(final AppDescriptor appDescriptor,
            final Set<String> sitesWithConfigurations, final User user)
            throws DotDataException, DotSecurityException {
        final Map<String, Map<String, List<String>>> warningsBySite = appsAPI
                .computeWarningsBySite(appDescriptor, sitesWithConfigurations, user);
        return (int) warningsBySite.values().stream().map(Map::values).filter(lists -> !lists.isEmpty())
                .count();
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
        if (appDescriptorOptional.isEmpty()) {
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

        final Map<String, Map<String, List<String>>> warningsBySite = appsAPI
                .computeWarningsBySite(appDescriptor, sitesWithConfigurations, user);

        final PaginationUtil paginationUtil = new PaginationUtil(new SiteViewPaginator(
                () -> sitesWithConfigurations, ()-> warningsBySite, hostAPI, permissionAPI));
        return paginationUtil
                .getPage(request, user,
                        paginationContext.getFilter(),
                        paginationContext.getPage(),
                        paginationContext.getPerPage(),
                        paginationContext.getOrderBy(),
                        orderDirection,
                        Collections.emptyMap(),
                        (Function<PaginatedArrayList<SiteView>, AppView>) paginatedArrayList -> {
                            final int count = sitesWithConfigurations.size();
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
            if (null == host || host.isArchived()) {
                throw new DoesNotExistException(
                      String.format(" Couldn't find any host with identifier `%s` ", siteId)
                );
            }

            final Optional<AppSecrets> optionalAppSecrets = appsAPI
                    .getSecrets(key, false, host, user);

            //We need to return a view with all the secrets and also descriptors of the remaining parameters merged.
            //So we're gonna need a copy of the params on the yml.
            final Map<String, ParamDescriptor> descriptorParams = appDescriptor.getParams();

            final Map<String, List<String>> warningsMap = appsAPI
                    .computeSecretWarnings(appDescriptor, host, user);

            final Map<String,SecretView> mappedParams = appDescriptor.getParams().keySet().stream()
                .map(paramKey -> new SecretView(paramKey, null, descriptorParams.get(paramKey), warningsMap.get(paramKey)))
                .collect(Collectors.toMap(SecretView::getName, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));

            final AppSecrets appSecrets = optionalAppSecrets.orElseGet(AppSecrets::empty);

            final Map<String, SecretView> mappedSecrets = appSecrets.getSecrets().entrySet()
                    .stream()
                    .map(e -> new SecretView(e.getKey(), e.getValue(),
                            descriptorParams.get(e.getKey()), warningsMap.get(e.getKey())))
                    .sorted((o1, o2) -> Boolean.compare(o1.isDynamic(), o2.isDynamic()))
                    .collect(Collectors.toMap(SecretView::getName, Function.identity(), (a, b) -> a,
                            LinkedHashMap::new));

            final int configurationsCount = appsAPI.filterSitesForAppKey(key, appsAPI.appKeysByHost().keySet(), user).size();

            final List<SecretView> mergedParamsAndSecrets = mappedParams.entrySet().stream()
                .map(paramViewEntry -> {
                    final SecretView secretView = mappedSecrets.remove(paramViewEntry.getKey());
                    return secretView != null ? secretView : paramViewEntry.getValue();
                })
                .collect(Collectors.toList());

            mergedParamsAndSecrets.addAll(
                    mappedSecrets.values().stream().filter(SecretView::isDynamic)
                            .collect(Collectors.toList())
            );

            final SiteView siteView = new SiteView(host.getIdentifier(), host.getHostname(), mergedParamsAndSecrets);
            return Optional.of(new AppView(appDescriptor, configurationsCount,
                    ImmutableList.of(siteView))
            );

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
        if (appDescriptorOptional.isEmpty()) {
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
        securityLoggerAPI.logInfo(this.getClass(),
                String.format("User `%s` deleted secret for app `%s` on host `%s`", user, key, siteId));
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
            throw new DoesNotExistException(String.format(" Couldn't find any host with identifier `%s` ",siteId));
        }
        final Optional<AppDescriptor> optionalAppDescriptor = appsAPI
                .getAppDescriptor(key, user);
        if (optionalAppDescriptor.isEmpty()) {
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
        final Optional<AppSecrets> appSecretsOptional = appsAPI.getSecrets(key, host, user);

        final Map<String, Input> params = validateFormForSave(form, appDescriptor, appSecretsOptional);
        //Create a brand new secret for the present app.
        final AppSecrets.Builder builder = new AppSecrets.Builder();
        builder.withKey(key);
        for (final Entry<String, Input> stringParamEntry : params.entrySet()) {
            final String name = stringParamEntry.getKey();
            final ParamDescriptor describedParam = appDescriptor.getParams().get(name);
            final Input inputParam = stringParamEntry.getValue();
            final Optional<Secret> secret;

            if (Objects.isNull(describedParam)) {
                secret = AppsUtil.dynamicSecret(key, name, inputParam);
            } else {
                //If we're dealing with a hidden param and there's a secret already saved...
                //The param must be overridden and replaced for that reason we must delete the existing saved secret.
                //In order to keep all existing secrets we grab the saved one and push it into the new.
                Logger.debug(AppsHelper.class, () -> "found hidden secret sent with no value.");
                if (isHidden(describedParam, inputParam)) {
                    secret = appSecretsOptional
                            .flatMap(appSecrets -> {
                                Logger.debug(
                                        AppsHelper.class,
                                        () -> " hidden secret sent with masked value we must grab the value from the saved secret so we dont lose it.");
                                return AppsUtil.hiddenSecret(key, name, describedParam, appSecrets);
                            });
                } else {
                    secret = AppsUtil.paramSecret(key, name, inputParam.getValue(), describedParam);
                }
            }
            secret.ifPresent(s -> builder.withSecret(name, s));
        }

        // Make sure if omitted params correspond to secrets which have their values managed by an env-vars and that
        // they're not editable.
        appSecretsOptional
                .map(appSecrets -> appSecrets.getSecrets().entrySet())
                .orElse(Set.of())
                .stream()
                .filter(entry -> !entry.getValue().isEditable() && !params.containsKey(entry.getKey()))
                .forEach(entry -> builder.withSecret(entry.getKey(), entry.getValue()));

        // We're gonna build the secret upfront and have it ready.
        // Since the next step is potentially risky (delete a secret that already exist).
        final AppSecrets secrets = builder.build();

        appsAPI.saveSecrets(secrets, host, user);
        securityLoggerAPI.logInfo(this.getClass(),
                String.format("User `%s` saved secret for app `%s` on host `%s`", user, key, host.getIdentifier()));
        //This operation needs to be executed at the very end.
        appSecretsOptional.ifPresent(AppSecrets::destroy);
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
            throw new DoesNotExistException(String.format(" Couldn't find any host with identifier `%s` ",siteId));
        }
        final Optional<AppDescriptor> optionalAppDescriptor = appsAPI.getAppDescriptor(key, user);
        if (optionalAppDescriptor.isEmpty()) {
            throw new DoesNotExistException(  String.format("Unable to find an app descriptor bound to the  Key `%s`. You must upload a yml descriptor.",key));
        }
        final AppDescriptor appDescriptor = optionalAppDescriptor.get();
        final Optional<AppSecrets> appSecretsOptional = appsAPI.getSecrets(key, host, user);
        if (appSecretsOptional.isEmpty()) {
            saveSecretForm(key, host, appDescriptor, form, user);
        } else {
            try {
                final Map<String, Input> params = validateFormForUpdate(form, appDescriptor);
                //Update individual secrets/properties.
                for (final Entry<String, Input> stringParamEntry : params.entrySet()) {
                    final Input inputParam = stringParamEntry.getValue();
                    final String name = stringParamEntry.getKey();
                    final ParamDescriptor describedParam = appDescriptor.getParams().get(name);
                    final Optional<Secret> secret;
                    if (Objects.isNull(describedParam)) {
                        secret = AppsUtil.dynamicSecret(key, name, inputParam);
                    } else {
                        if (isHidden(describedParam, inputParam)) {
                            Logger.debug(AppsHelper.class, () -> "skipping secret sent with no value.");
                            continue;
                        }

                        secret = AppsUtil.paramSecret(key, name, inputParam.getValue(), describedParam);
                    }

                    if (secret.isPresent()) {
                        appsAPI.saveSecret(key, Tuple.of(name, secret.get()), host, user);
                    }
                }
                securityLoggerAPI.logInfo(this.getClass(),
                        String.format("User `%s` updated secret for app `%s` on host `%s`", user, key, host.getIdentifier()));
            } finally {
                form.destroySecretTraces();
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
        if (optionalAppDescriptor.isEmpty()) {
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
        if (appSecretsOptional.isEmpty()) {
            throw new DoesNotExistException(String.format("Unable to find a secret for app with Key `%s`.",key));
        } else {
            appsAPI.deleteSecret(key, params, host, user);
            securityLoggerAPI.logInfo(this.getClass(),
                    String.format("User `%s` deleted secret for app `%s` on host `%s`", user, key, host.getIdentifier()));
        }
    }

    /**
     * This method allows deleting a single secret/property from a stored integration.
     *
     * @param describedParam the param descriptor
     * @param inputParam the input param
     */
    private boolean isHidden(final ParamDescriptor describedParam, final Input inputParam) {
        return describedParam.isHidden() && isAllFilledWithStars(inputParam.getValue());
    }

    /**
     * Validate the incoming params match the params described by an appDescriptor yml.
     * This validation is intended to behave as a form validation. It'll make sure that all required values are present at save time.
     * And nothing else besides the params described are allowed. Unless they app-descriptor establishes that extraParams are allowed.
     *
     * @param form               a set of paramNames.
     * @param appDescriptor      the app template.
     * @param appSecretsOptional the app secrets.
     * @throws IllegalArgumentException This will give back an exception if you send an invalid param.
     */
    private Map<String, Input> validateFormForSave(final SecretForm form,
                                                   final AppDescriptor appDescriptor,
                                                   final Optional<AppSecrets> appSecretsOptional)
            throws IllegalArgumentException {

        if (!UtilMethods.isSet(form.getInputParams())) {
            throw new IllegalArgumentException("Required Params aren't set.");
        }

        final Map<String, Input> params = form.getInputParams().entrySet().stream()
                .collect(Collectors.toMap(stringInputEntry -> stringInputEntry.getKey().trim(),
                        Entry::getValue));


        AppsUtil.validateForSave(mapForValidation(params), appDescriptor, appSecretsOptional);

        return params;
    }

    /**
     * Map of optionals is a middle ground between {@link SecretForm} and {@link AppSecrets}
     * used by {@link AppsUtil#validateForSave(Map, AppDescriptor, Optional)}
     * @return
     */
    private Map<String, Optional<char[]>> mapForValidation(final Map<String, Input> params) {
        return params.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, stringInputEntry -> {
                final Input input = stringInputEntry.getValue();
                return input == null ? Optional.empty() : Optional.of(input.getValue());
                })
            );
    }


    /**
     * This method is meant to validate inputs for an update that can be performed on individual
     * properties. It assumes there's an instance already saved and only performs validations on the
     * new incoming params. This gives the flexibility to modify the value on individual properties
     * that are already saved. If the app We're not expecting
     * @throws IllegalArgumentException This will give back an exception if you send an invalid param.
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
            final String paramName = entry.getKey();
            final ParamDescriptor paramDescriptor = appDescriptorParams.get(paramName);
            if (null == paramDescriptor && !appDescriptor.isAllowExtraParameters()) {
                throw new IllegalArgumentException(String.format(
                        "Unknown additional Param `%s` not allowed by the app descriptor.",
                        paramName));
            } else {
                if (null != paramDescriptor && paramDescriptor.isRequired() && null != entry
                        .getValue() && isNotSet(entry.getValue().getValue())) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Param `%s` is marked required in the descriptor but does not come with a value.",
                                    paramName
                            )
                    );
                }

                if (paramDescriptor != null && Type.BOOL.equals(paramDescriptor.getType())
                        && UtilMethods.isSet(entry.getValue())) {
                    final String asString = new String(entry.getValue().getValue());
                    final boolean bool = (asString.equalsIgnoreCase(Boolean.TRUE.toString())
                            || asString.equalsIgnoreCase(Boolean.FALSE.toString()));
                    if (!bool) {
                        throw new IllegalArgumentException(
                                String.format(
                                        "Can not convert value `%s` to type BOOL for param `%s`.",
                                        asString, paramName
                                )
                        );
                    }
                }
            }
        }

        return params;
    }


    /**
     * Validate the incoming param names match the params described by an appDescriptor yml.
     * This is mostly useful to validate a delete param request
     * @param inputParamNames a set of paramNames.
     * @param appDescriptor the app template.
     * @throws DotDataException This will give back an exception if you send an invalid param.
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
     * @param multipart The multipart form
     * @param user Logged in dude.
     * @throws IOException
     * @throws DotDataException
     */
    List<AppView> createApp(final FormDataMultiPart multipart, final User user)
            throws IOException, DotDataException, JSONException {

        return processMultipart(multipart, (file, bodyMultipart) -> {
            final AppDescriptor appDescriptor;
            try {
                appDescriptor = appsAPI.createAppDescriptor(file, user);
                securityLoggerAPI.logInfo(this.getClass(),
                        String.format("User `%s` created app descriptor `%s`", user, appDescriptor.getKey()));
                return new AppView(appDescriptor, 0, 0);
            } catch (AlreadyExistException | DotSecurityException  e) {
                throw new DotDataException(e.getMessage(), e);
            }
        });

    }

    /**
     *
     * @param key The app key
     * @param user The user
     * @throws DotSecurityException
     * @throws DotDataException
     */
    void removeApp(final String key, final User user, final boolean removeDescriptor)
            throws DotSecurityException, DotDataException {
        appsAPI.removeApp(key, user, removeDescriptor);
        if(removeDescriptor){
            securityLoggerAPI.logInfo(this.getClass(),
                    String.format("User `%s` removed apps and descriptor `%s`", user, key));
        } else  {
            securityLoggerAPI.logInfo(this.getClass(),
                   String.format("User `%s` removed apps `%s`", user, key));
        }
    }

    /**
     * This method checks if we're looking at a char array all filled with the character `*`
     * @param chars
     * @return
     */
    private boolean isAllFilledWithStars(final char [] chars){
         if(isNotSet(chars)){
           return false;
         }
         for(final char chr: chars){
            if(chr != '*'){
               return false;
            }
         }
         return true;
    }

    /**
     * Secrets export
     * @param form
     * @param user
     * @return
     * @throws DotSecurityException
     * @throws IOException
     * @throws DotDataException
     */
    InputStream exportSecrets(final ExportSecretForm form, final User user)
            throws DotSecurityException, IOException, DotDataException {

        Logger.info(AppsHelper.class,"Secrets export: "+form);
        final Key key = AppsUtil.generateKey(AppsUtil.loadPass(form::getPassword));
        try {
            return Files.newInputStream(appsAPI
                    .exportSecrets(key, form.isExportAll(), form.getAppKeysBySite(), user));
        } finally {
            securityLoggerAPI.logInfo(this.getClass(),
                    String.format("User `%s` exported secrets for `%s` ", user, form));
        }
    }

    /**
     * Secrets import
     * @param multipart
     * @param user
     * @throws IOException
     * @throws DotDataException
     * @throws JSONException
     */
    void importSecrets(final FormDataMultiPart multipart, final User user)
            throws IOException, DotDataException, JSONException {

        processMultipart(multipart, new FileConsumer<Void>() {
            private Key key = null;
            @Override
            public Void apply(final File file, final Map<String, Object> bodyMultipart)
                    throws DotDataException {
                if(null == key){
                    final String password = (String) bodyMultipart.get("password");
                    key = AppsUtil.generateKey(AppsUtil.loadPass(() -> password));
                }
                try {
                    appsAPI.importSecretsAndSave(file.toPath(), key, user);
                    securityLoggerAPI.logInfo(this.getClass(),
                            String.format("User `%s` imported secrets from file `%s`", user, file.getName()));
                    return null;
                } catch (DotSecurityException | IOException | EncryptorException e) {
                    throw new DotDataException(e.getMessage(), e);
                }
            }
        });
    }

    @FunctionalInterface
    interface FileConsumer<T> {

        /**
         * Whatever need to happen with the file and the multipart.Should be halded
         * @param file
         * @param bodyMultipart
         * @return the result
         * @throws DotDataException
         */
        T apply (File file, Map<String, Object> bodyMultipart)
                throws DotDataException;
    }

    /**
     * Multipart common process function
     * whatever specific needs be done in between has to take place in the fileConsumer
     * @param multipart the multipart
     * @param consumer the consumer
     * @param <T> the type of the result
     * @return
     * @throws IOException
     * @throws DotDataException
     * @throws JSONException
     * @throws AlreadyExistException
     * @throws DotSecurityException
     */
   private <T> List<T> processMultipart(final FormDataMultiPart multipart, final FileConsumer<T> consumer)
           throws IOException, DotDataException, JSONException {
        final MultiPartUtils multiPartUtils = new MultiPartUtils();
        final List<File> files = multiPartUtils.getBinariesFromMultipart(multipart);
        try {
            if (!UtilMethods.isSet(files)) {
                throw new DotDataException(
                        "Unable to extract any files from multi-part request.");
            }

            final Map<String, Object> bodyMapFromMultipart = multiPartUtils.getBodyMapFromMultipart(multipart);
            final List<T> result = new ArrayList<>(files.size());
            for (final File file : files) {
                try {
                    if (0 == file.length()) {
                        throw new IllegalArgumentException("Zero length file.");
                    }
                    result.add(consumer.apply(file, bodyMapFromMultipart));
                } finally {
                    file.delete();
                }
            }
              return result;
        } finally {
            removeTempFolder(files.get(0).getParentFile());
        }
    }

    /**
     * cleanup our mess
     * @param parentFolder the parent folder
     */
    private void removeTempFolder(final File parentFolder) {
        final String parentFolderName = parentFolder.getName();
        if (parentFolder.isDirectory() && parentFolderName.startsWith("tmp_upload")) {
            if (parentFolder.delete()) {
                Logger.debug(AppsHelper.class,
                        String.format(" tmp upload directory `%s` removed successfully. ",
                                parentFolder.getAbsolutePath()));
            } else {
                Logger.debug(AppsHelper.class,
                        String.format(" Unable to remove tmp upload directory `%s`. ",
                                parentFolder.getAbsolutePath()));
            }
        }
    }

}
