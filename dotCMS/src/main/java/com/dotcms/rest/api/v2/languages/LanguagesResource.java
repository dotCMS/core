package com.dotcms.rest.api.v2.languages;

import com.dotcms.keyvalue.model.KeyValue;
import com.dotcms.languagevariable.business.LanguageVariable;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.rendering.velocity.viewtools.util.ConversionUtils;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.MessageEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.I18NForm;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageCacheImpl;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.quartz.job.DefaultLanguageTransferAssetJob;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.beanutils.BeanUtils;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dotcms.rest.ResponseEntityView.OK;
import static com.dotmarketing.portlets.languagesmanager.business.LanguageAPI.isLocalizationEnhancementsEnabled;
import static com.dotmarketing.util.WebKeys.CONTENT_SELECTED_LANGUAGE;
import static com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE;
import static com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED;

/**
 * Language endpoint for the v2 API
 */
@Path("/v2/languages")
@Tag(name = "Internationalization")
public class LanguagesResource {

    private final LanguageAPI languageAPI;

    private final LanguageVariableAPI languageVariableAPI;

    private final WebResource webResource;
    private final com.dotcms.rest.api.v1.languages.LanguagesResource oldLanguagesResource;

    public LanguagesResource() {
        this(APILocator.getLanguageAPI(),
             APILocator.getLanguageVariableAPI(),
             new WebResource(new ApiProvider())
        );
    }

    @VisibleForTesting
    public LanguagesResource(final LanguageAPI languageAPI,
                             final LanguageVariableAPI languageVariableAPI,
                             final WebResource webResource) {

        this.languageAPI  = languageAPI;
        this.languageVariableAPI = languageVariableAPI;
        this.webResource  = webResource;
        this.oldLanguagesResource = new com.dotcms.rest.api.v1.languages.LanguagesResource(languageAPI, webResource, I18NUtil.INSTANCE);
    }

    /**
     * Get a language by Id
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param languageId {@link Long}
     * @return Response
     */
    @GET
    @Path("/id/{languageid}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response get(@Context HttpServletRequest request,
                        @Context final HttpServletResponse response,
                        @PathParam("languageid") final long languageId) {

        webResource.init(request, response, true);

        final Language language = this.languageAPI.getLanguage(languageId);
        if (null == language) {

            throw new DoesNotExistException("The language id = " + languageId + " does not exists");
        }

        return Response.ok(new ResponseEntityView<>(new LanguageView(language, ()->isDefault(language)))).build();
    }

    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    /**
     * return an array with all the languages
     */
    public Response list(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @QueryParam("contentInode") final String contentInode,
             @QueryParam("countLangVars") final boolean countLangVars
    )
            throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> String.format("listing languages %s", request.getRequestURI()));

        final InitDataObject init = webResource.init(request, response, true);
        final User user = init.getUser();

        final List<Language> languages = contentInode != null ?
                languageAPI.getAvailableContentLanguages(contentInode, user) :
                languageAPI.getLanguages();
        if (countLangVars){
            //We calculate the total number of language variables once as this value is the same for all languages
            final int total = languageVariableAPI.countVariablesByKey();
            return Response.ok(
                    new ResponseEntityView<>(languages.stream()
                            .map(language -> withLangVarCounts(language, total))
                            .collect(Collectors.toList())))
                    .build();
        }
        return Response.ok(
                new ResponseEntityView<>(languages.stream()
                        .map(instanceLanguageView())
                        .collect(Collectors.toList())))
                .build();
    }

    @VisibleForTesting
    boolean isDefault(final Language language){
        return language.getId() == languageAPI.getDefaultLanguage().getId();
    }

    /**
     * Returns a {@link LanguageView} with the total number of language variables and the number of language variables
     * @param language {@link Language}
     * @return {@link LanguageView}
     */
   LanguageView withLangVarCounts(final Language language, final int total){
       final int langCount = languageVariableAPI.countVariablesByKey(language.getId());
       return new LanguageView(language, ()->isDefault(language), ()->ImmutableLangVarsCount.builder().total(total).count(langCount).build());
    }

    public Function<Language, LanguageView> instanceLanguageView() {
       return language -> new LanguageView(language, ()->isDefault(language));
    }

    /**
     * Validates if the language exists in the system
     * @param languageForm LanguageForm
     * @return Language
     */
    private Language validateLanguageExists(final LanguageForm languageForm) {
        DotPreconditions.checkArgument(UtilMethods.isSet(languageForm.getLanguageCode())
                        || UtilMethods.isSet(languageForm.getIsoCode()),
                "Language Code can't be null or empty");
        return getLanguage(languageForm);
    }

    /**
     * Persists a new {@link Language}
     *
     * @param request HttpServletRequest
     * @param languageForm LanguageForm
     * @return JSON response including the following properties of the {@link Language}:
     *  <ul>
     *  <li>{@code id}
     *  <li>{@code languageCode}
     *  <li>{@code countryCode}
     *  <li>{@code language}
     *  <li>{@code country}
     *  </ul>
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveLanguage(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final LanguageForm languageForm) throws AlreadyExistException {
        this.webResource.init(null, request, response,
                true, PortletID.LANGUAGES.toString());
        DotPreconditions.notNull(languageForm,"Expected Request body was empty.");
        final Language language = validateLanguageExists(languageForm);
        if(null != language && language != LanguageCacheImpl.LANG_404){
            return Response.ok(new ResponseEntityView<>(language, List.of(new MessageEntity("Language already exists.")))).build(); // 200
        }
        final Language savedOrUpdateLanguage = saveOrUpdateLanguage(null, languageForm);
        return Response.ok(new ResponseEntityView<>(
                new LanguageView(savedOrUpdateLanguage,()->isDefault(savedOrUpdateLanguage) )
        )).build(); // 200
    }

    @POST
    @JSONP
    @NoCache
    @Path("/{languageTag}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveFromLanguageTag(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("languageTag") final String languageTag,
            @DefaultValue("false") @QueryParam("strict") final boolean strict) throws AlreadyExistException {
        DotPreconditions.notNull(languageTag, "Expected languageTag Param path was empty.");
        this.webResource.init(null, request, response,
                true, PortletID.LANGUAGES.toString());

        final LanguageForm languageForm = this.getLanguageData(languageTag, strict);

        final Language language = validateLanguageExists(languageForm);
        if(null != language && language != LanguageCacheImpl.LANG_404){
           return Response.ok(new ResponseEntityView<>(language, List.of(new MessageEntity("Language already exists.")))).build(); // 200
        }
        final Language saveOrUpdateLanguage = saveOrUpdateLanguage(null, languageForm);
        return Response.ok(new ResponseEntityView<>(
                new LanguageView(saveOrUpdateLanguage,()->isDefault(saveOrUpdateLanguage))
        )).build(); // 200
    }

    /**
     * Takes the incoming Language Tag and creates a {@link LanguageForm} object with it. It's worth
     * noting that the languageTag can be in the format of a valid Locale, or any other format
     * specified by the User.
     *
     * @param languageTag The Language Tag that is being transformed into a {@link LanguageForm}
     *                    object.
     * @param strict      If the Language Tag must be checked against a valid Locale, set this to
     *                    {@code true}. If invalid Locales must be supported, set this to
     *                    {@code false}.
     *
     * @return The {@link LanguageForm} object created from the Language Tag.
     */
    private LanguageForm getLanguageData(final String languageTag, final boolean strict) {
        final LanguageForm.Builder languageFormBuilder = new LanguageForm.Builder();
        Locale locale;
        if (strict) {
            locale = this.validateLanguageTag(languageTag);
            languageFormBuilder.language(locale.getDisplayLanguage()).languageCode(locale.getLanguage())
                    .country(locale.getDisplayCountry()).countryCode(locale.getCountry());
            return languageFormBuilder.build();
        }
        locale = Locale.forLanguageTag(languageTag);
        if (UtilMethods.isNotSet(locale.toString())) {
            Logger.warn(this, String.format("Language Tag '%s' " +
                    "may not be a valid Locale. Creating Language with available information", languageTag));
            final Tuple2<String, String> extractedCodes = LanguageUtil.getLanguageCountryCodes(languageTag);
            languageFormBuilder.language(extractedCodes._1).languageCode(extractedCodes._1)
                    .countryCode(extractedCodes._2);
        } else {
            languageFormBuilder.language(locale.getDisplayLanguage()).languageCode(locale.getLanguage())
                    .country(locale.getDisplayCountry()).countryCode(locale.getCountry());
        }
        return languageFormBuilder.build();
    }


    @GET
    @Path("/{languageTag}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getFromLanguageTag (
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("languageTag") final String languageTag,
            @DefaultValue("false") @QueryParam("strict") final boolean strict) {
        final LanguageForm languageForm = this.getLanguageData(languageTag, strict);
        DotPreconditions.notNull(languageTag, "Expected languageTag Param path was empty.");
        final Language language = getLanguage(languageForm);
        if(null == language){
           return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(new ResponseEntityView<>(new LanguageView(language,()->isDefault(language) ))).build();
    }

    private Locale validateLanguageTag(final String languageTag)throws DoesNotExistException {
        return LanguageUtil.validateLanguageTag(languageTag);
    }

    /**
     * Updates an already persisted {@link Language}
     *
     * @param request HttpServletRequest
     * @param languageId languageId
     * @param languageForm LanguageForm
     * @return JSON response including the following properties of the {@link Language}:
     *  <ul>
     *  <li>{@code id}
     *  <li>{@code languageCode}
     *  <li>{@code countryCode}
     *  <li>{@code language}
     *  <li>{@code country}
     *  </ul>
     */
    @PUT
    @Path("/{languageId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response updateLanguage(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("languageId") final String languageId,
            final LanguageForm languageForm) throws AlreadyExistException {
        this.webResource.init(null, request, response,
                true, PortletID.LANGUAGES.toString());
        DotPreconditions.checkArgument(UtilMethods.isSet(languageId),"Language Id is required.");
        DotPreconditions.isTrue(doesLanguageExist(languageId), DoesNotExistException.class, ()->"Language not found");
        DotPreconditions.notNull(languageForm,"Expected Request body was empty.");
        final Language language = saveOrUpdateLanguage(languageId, languageForm);
        return Response.ok(new ResponseEntityView<>(
                new LanguageView(language, ()->isDefault(language))
        )).build(); // 200
    }

    /**
     * Deletes an already persisted {@link Language}
     *
     * @param request HttpServletRequest
     * @param languageId languageId
     * @return 200 response with "Ok" message
     */
    @DELETE
    @Path("/{languageId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteLanguage(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("languageId") final String languageId) {
        this.webResource.init(null, request, response,
                true, PortletID.LANGUAGES.toString());
        DotPreconditions.checkArgument(UtilMethods.isSet(languageId),"Language Id is required.");
        DotPreconditions.isTrue(doesLanguageExist(languageId), DoesNotExistException.class, ()->"Language not found");
        final Language language = languageAPI.getLanguage(languageId);
        languageAPI.deleteLanguage(language);
        return Response.ok(new ResponseEntityView<>(OK)).build(); // 200
    }

    @POST
    @JSONP
    @NoCache
    @Path("/i18n")
    @InitRequestRequired
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getMessages(@Context HttpServletRequest request,
                                final I18NForm i18NForm) {
        return oldLanguagesResource.getMessages(request, i18NForm);
    }

    /**
     * Takes the requested language and resolves it to a locale
     * that the Admin console understands
     * @param language
     * @return
     */
    private Locale resolveAdminLocale(@NotNull String language) {
        final Locale defaultLocale = APILocator.getCompanyAPI().getDefaultCompany().getLocale();
        final Locale requestedLocale = ConversionUtils.toLocale(language);
        final Locale[] locales = LanguageUtil.getAvailableLocales();

        if(null == requestedLocale) {
            return defaultLocale;
        }
        
        for(int i=0;i<locales.length;i++){
            if(locales[i].equals(requestedLocale)) {
                return locales[i];
            }
        }
        
        for(int i=0;i<locales.length;i++){
            if(locales[i].getLanguage().equalsIgnoreCase(requestedLocale.getLanguage())){
                return locales[i];
            }
        }
            

        return defaultLocale;
        
    }
    
    
    /**
     * Gets all the Messages from the language passed.
     * If default is passed it will get the messages for the default language.
     * Checks also if the language exists in dotcms to get all language keys and
     * language variables.
     *
     * @param language languageCode e.g en, it or languageCode_CountryCode e.g en_us, it_it
     * @return all the messages of the language
     */
    @GET
    @Path("{language}/keys")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getAllMessages (
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("language") final String language) throws DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(request, response)
                .requiredAnonAccess(AnonymousAccess.READ)
                .rejectWhenNoUser(false).init();

        final User user = initData.getUser();

        final Locale currentLocale=resolveAdminLocale(language);
        
        //Messages in the properties file
        //These are the resources that are in the properties file added by developers to dotCMS so will always need this
        final Map<?,?> mapPropertiesFile = LanguageUtil.getAllMessagesByLocale(currentLocale);

        final Map<Object,Object> result = new TreeMap<>(mapPropertiesFile);

        final Language language1 = languageAPI.getLanguage(currentLocale.getLanguage(),currentLocale.getCountry());
        if(UtilMethods.isSet(language1)) {

            if(isLocalizationEnhancementsEnabled()) {
                final Language matchingLang = languageAPI.getLanguage(currentLocale.getLanguage(),currentLocale.getCountry());
                // Enhanced Language Vars
                final List<LanguageVariable> variables = languageVariableAPI.findVariables(matchingLang.getId());
                final Map<?,?> map = variables.stream().collect(
                        Collectors.toMap(LanguageVariable::key, LanguageVariable::value, (value1,value2) ->{
                            //Merge function is always a good idea to have.
                            //There can be cases on which the "unique" constraint of the key is lifted allowing for duplicates
                            Logger.warn(this.getClass(),"Duplicate language variable found using latest value: " + value1);
                            return value1;
                        }));
                result.putAll(map);
            } else {
                //Language Keys
                final Map <?,?> mapLanguageKeys = languageAPI
                        .getLanguageKeys(currentLocale.getLanguage()).stream().collect(
                                Collectors.toMap(LanguageKey::getKey, LanguageKey::getValue));

                result.putAll(mapLanguageKeys);

                //Legacy Language Variable
                long langId = language1.getId();
                final Map mapLanguageVariables = Try.of(()->APILocator.getLanguageVariableAPI().getAllLanguageVariablesKeyStartsWith("", langId,
                        user, -1)).getOrElse(ArrayList::new).stream().collect(Collectors.toMap(
                        KeyValue::getKey,KeyValue::getValue, (value1,value2) ->{
                            Logger.warn(this.getClass(),"Duplicate language variable found using latest value: " + value1);
                            return value1;
                        }));
                result.putAll(mapLanguageVariables);
            }

        }


        return Response.ok(new ResponseEntityView<>(result)).build();
    }

    /**
     * Gets all the language variables in the system organized by key
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param renderNulls boolean
     * @param paginationContext {@link PaginationContext}
     * @return all the messages of the language
     * @throws DotDataException if an error occurs
     */
    @GET
    @Path("/variables")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getVariables(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("renderNulls") @DefaultValue("true") final boolean renderNulls,
            @BeanParam final PaginationContext paginationContext) throws DotDataException {

                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(true)
                        .requiredPortlet(PortletID.LANGUAGES.toString())
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();

        final LanguageVariablePageView view = new LanguageVariablesHelper()
                .view(paginationContext, renderNulls);
        return Response.ok(new ResponseEntityView<>(view)).build();
    }


    private Language saveOrUpdateLanguage(final String languageId, final LanguageForm form)
            throws AlreadyExistException {
        final Language newLanguage = new Language();

        if (StringUtils.isSet(languageId)) {
            final Language origLanguage = this.languageAPI.getLanguage(languageId);
            Sneaky.sneaked(()->BeanUtils.copyProperties(newLanguage, origLanguage));
            newLanguage.setId(origLanguage.getId());
        }

        if (StringUtils.isSet(form.getIsoCode())){
            final Locale locale = Locale.forLanguageTag(form.getIsoCode());

            newLanguage.setLanguage(locale.getDisplayLanguage());
            newLanguage.setLanguageCode(locale.getLanguage());
            newLanguage.setCountry(locale.getDisplayCountry());
            newLanguage.setCountryCode(locale.getCountry());
        } else{
            newLanguage.setLanguage(form.getLanguage());
            newLanguage.setCountry(form.getCountry());
            newLanguage.setLanguageCode(form.getLanguageCode());
            newLanguage.setCountryCode(form.getCountryCode());
        }

        DotPreconditions.checkArgument(UtilMethods.isSet(newLanguage.getLanguageCode()),
                "Language Code can't be null or empty");

        final Language existingLang = languageAPI.getLanguage(newLanguage.getLanguageCode(), newLanguage.getCountryCode());
        if(null != existingLang && existingLang != LanguageCacheImpl.LANG_404 && Long.parseLong(languageId) != existingLang.getId()){
           throw new AlreadyExistException(String.format("Update Attempt clashes with an existing language with id `%s`.",existingLang.getId()));
        }

        this.languageAPI.saveLanguage(newLanguage);
        return newLanguage;
    }

    private boolean doesLanguageExist(final String languageId) {
        return languageAPI.getLanguage(languageId)!=null &&
                languageAPI.getLanguage(languageId).getId()>0;
    }

    private Language getLanguage(final LanguageForm form) {
        final String languageCode = (StringUtils.isSet(form.getIsoCode())? form.getIsoCode().split("-")[0]:form.getLanguageCode());
        final String countryCode = ((StringUtils.isSet(form.getIsoCode()) && form.getIsoCode().split("-").length > 1)
                        ? form.getIsoCode().split("-")[1] : form.getCountryCode());
        return this.languageAPI.getLanguage(languageCode, countryCode);
    }

    @PUT
    @Path("/{language}/_makedefault")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response makeDefault(@Context final HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse,
            @PathParam("language") final Long languageId,
            final MakeDefaultLangForm makeDefaultLangForm
    ) throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.LANGUAGES.toString())
                .init().getUser();

        Logger.info(LanguagesResource.class, String.format("Switching to a new default language with id `%s`. ",languageId));
        final Language oldDefaultLanguage = languageAPI.getDefaultLanguage();
        final Language newDefault = languageAPI.makeDefault(languageId, user);
        Logger.info(LanguagesResource.class, String.format("Successfully switched to a new default language with id `%s`. ",languageId));
        if(makeDefaultLangForm.isFireTransferAssetsJob()){
            Logger.info(LanguagesResource.class, String.format(" A Job has been scheduled to transfer all assets from the old default language `%d` to `%d`. ",oldDefaultLanguage.getId(),languageId));
            DefaultLanguageTransferAssetJob
                    .triggerDefaultLanguageTransferAssetJob(oldDefaultLanguage.getId(), newDefault.getId());
        }

        httpServletRequest.getSession().removeAttribute(LANGUAGE_SEARCHED);
        httpServletRequest.getSession().removeAttribute(HTMLPAGE_LANGUAGE);
        httpServletRequest.getSession().removeAttribute(CONTENT_SELECTED_LANGUAGE);
        return Response.ok(new ResponseEntityView<>(
                new LanguageView(newDefault, ()->isDefault(newDefault))
        )).build(); // 200
    }

    @GET
    @Path("/iso")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getIsoLanguagesAndCountries (
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response)  {

        final List<Map<String, String>> languages = Arrays.stream(Locale.getISOLanguages())
                .map(code -> Map.of("code", code, "name", new Locale(code).getDisplayLanguage()))
                .sorted(Comparator.comparing(o -> o.get("name")))
                .collect(Collectors.toList());

        final List<Map<String, String>> countries = Arrays.stream(Locale.getISOCountries())
                .map(code -> Map.of("code", code, "name", new Locale("", code).getDisplayCountry()))
                .sorted(Comparator.comparing(o -> o.get("name")))
                .collect(Collectors.toList());

        return Response.ok(new ResponseEntityView<>(Map.of(
                 "languages", languages,
                 "countries", countries)
               )
        ).build();
    }

    /**
     * Returns the current default {@link Language} in the dotCMS instance.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     *
     * @return A {@link Response} object containing the default {@link Language} in the dotCMS
     * instance.
     *
     * @throws DotDataException An error occurred when retrieving the default {@link Language}.
     */
    @GET
    @Path("/_getdefault")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityView<Language> getDefaultLanguage(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response) throws DotDataException {
        Logger.debug(this, () -> "Retrieving the current default Language");
        return new ResponseEntityView<>(this.languageAPI.getDefaultLanguage());
    }

}
