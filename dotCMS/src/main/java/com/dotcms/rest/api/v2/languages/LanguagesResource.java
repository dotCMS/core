package com.dotcms.rest.api.v2.languages;

import static com.dotcms.rest.ResponseEntityView.OK;
import static com.dotmarketing.util.UtilMethods.isNotSet;
import static com.dotmarketing.util.WebKeys.*;

import com.dotcms.keyvalue.model.KeyValue;
import com.dotcms.rendering.velocity.viewtools.util.ConversionUtils;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.MessageEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.I18NForm;
import com.dotcms.rest.api.v1.languages.LanguageTransform;
import com.dotcms.rest.api.v1.languages.RestLanguage;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.quartz.job.DefaultLanguageTransferAssetJob;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
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
import org.apache.commons.beanutils.BeanUtils;
import org.glassfish.jersey.server.JSONP;

/**
 * Language end point
 */
@Path("/v2/languages")
public class LanguagesResource {

    private final LanguageAPI languageAPI;
    private final WebResource webResource;
    private final com.dotcms.rest.api.v1.languages.LanguagesResource oldLanguagesResource;

    public LanguagesResource() {
        this(APILocator.getLanguageAPI(),
                new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    public LanguagesResource(final LanguageAPI languageAPI,
                                final WebResource webResource) {

        this.languageAPI  = languageAPI;
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

        return Response.ok(new ResponseEntityView(new LanguageView(language))).build();
    }

    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    /**
     * return a array with all the languages
     */
    public Response  list(@Context final HttpServletRequest request, @Context final HttpServletResponse response, @QueryParam("contentInode") final String contentInode)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> String.format("listing languages %s", request.getRequestURI()));

        final InitDataObject init = webResource.init(request, response, true);
        final User user = init.getUser();

        final List<Language> languages = contentInode != null ?
                languageAPI.getAvailableContentLanguages(contentInode, user) :
                languageAPI.getLanguages();

        return Response.ok(
                new ResponseEntityView<>(languages.stream()
                        .map(instanceLanguageView())
                        .collect(Collectors.toList())))
                .build();
    }

    public Function<Language, LanguageView> instanceLanguageView() {
        return LanguageView::new;
    }

    private Language validateLanguageExists(final LanguageForm languageForm) {
        DotPreconditions.checkArgument(UtilMethods.isSet(languageForm.getLanguageCode()),
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
        if(null != language){
            return Response.ok(new ResponseEntityView(language, ImmutableList.of(new MessageEntity("Language already exists.")))).build(); // 200
        }
        return Response.ok(new ResponseEntityView(saveOrUpdateLanguage(null, languageForm))).build(); // 200
    }

    @POST
    @JSONP
    @NoCache
    @Path("/{languageTag}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveFromLanguageTag(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("languageTag") final String languageTag
    ) throws AlreadyExistException {
        DotPreconditions.notNull(languageTag, "Expected languageTag Param path was empty.");
        this.webResource.init(null, request, response,
                true, PortletID.LANGUAGES.toString());

        final Locale locale = validateLanguageTag(languageTag);

        final LanguageForm languageForm = new LanguageForm.Builder()
                .language(locale.getDisplayLanguage()).languageCode(locale.getLanguage())
                .country(locale.getDisplayCountry()).countryCode(locale.getCountry()).build();

        final Language language = validateLanguageExists(languageForm);
        if(null != language){
           return Response.ok(new ResponseEntityView(language, ImmutableList.of(new MessageEntity("Language already exists.")))).build(); // 200
        }
        return Response.ok(new ResponseEntityView(saveOrUpdateLanguage(null, languageForm))).build(); // 200
    }


    @GET
    @Path("/{languageTag}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getFromLanguageTag (
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("languageTag") final String languageTag) {

        this.webResource.init(null, request, response,
                true, PortletID.LANGUAGES.toString());

        final Locale locale = validateLanguageTag(languageTag);
        final LanguageForm languageForm = new LanguageForm.Builder()
                .language(locale.getDisplayLanguage()).languageCode(locale.getLanguage())
                .country(locale.getDisplayCountry()).countryCode(locale.getCountry()).build();

        DotPreconditions.notNull(languageTag, "Expected languageTag Param path was empty.");
        final Language language = getLanguage(languageForm);
        if(null == language){
           return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(new ResponseEntityView(language)).build(); // 200

    }

    private Locale validateLanguageTag(final String languageTag)throws DoesNotExistException {
        final Locale locale = Locale.forLanguageTag(languageTag);
        final boolean validCountry = (isNotSet(locale.getCountry()) || Stream.of(Locale.getISOCountries()).collect(Collectors.toSet()).contains(locale.getCountry()));
        final boolean validLang = Stream.of(Locale.getISOLanguages()).collect(Collectors.toSet()).contains(locale.getLanguage());
        if(validLang && validCountry) {
            return locale;
        } else {
           throw new DoesNotExistException(String.format(" `%s` is an invalid language tag ", languageTag));
        }
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
        return Response.ok(new ResponseEntityView(language)).build(); // 200
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
        return Response.ok(new ResponseEntityView(OK)).build(); // 200
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
            @PathParam("language") final String language){

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredAnonAccess(AnonymousAccess.READ)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(false).init();

        final User user = initData.getUser();

        final Locale locale = "default".equalsIgnoreCase(language) ? APILocator.getLanguageAPI().getDefaultLanguage().asLocale() : ConversionUtils.toLocale(language);
        final Locale[] locales = LanguageUtil.getAvailableLocales();
        for(int i=0;i<locales.length;i++){
            final Locale currentLocale = locales[i];
            if(currentLocale.getLanguage().equalsIgnoreCase(locale.getLanguage())){
                if(UtilMethods.isSet(locale.getCountry())){
                    if(currentLocale.getCountry().equalsIgnoreCase(locale.getCountry())){
                        break;
                    }
                }else{
                    break;
                }

            } else if(i == (locales.length-1)){
                final String message = "Locale: "+ locale + " not found in Portal.properties file";
                Logger.error(this,message);
                throw new DoesNotExistException(message);
            }
        }

        //Messages in the properties file
        final Map mapPropertiesFile = LanguageUtil.getAllMessagesByLocale(locale);

        final Map result = new TreeMap(mapPropertiesFile);

        final Language language1 = APILocator.getLanguageAPI().getLanguage(locale.getLanguage(),locale.getCountry());
        if(UtilMethods.isSet(language1)) {
            //Language Keys
            final Map mapLanguageKeys = APILocator.getLanguageAPI()
                    .getLanguageKeys(locale.getLanguage()).stream().collect(
                            Collectors.toMap(LanguageKey::getKey, LanguageKey::getValue));

            result.putAll(mapLanguageKeys);

            //Language Variable
            long langId = language1.getId();
            final Map mapLanguageVariables = Try.of(()->APILocator.getLanguageVariableAPI().getAllLanguageVariablesKeyStartsWith("", langId,
                    user, -1)).getOrElse(ArrayList::new).stream().collect(Collectors.toMap(
                    KeyValue::getKey,KeyValue::getValue));

            result.putAll(mapLanguageVariables);

        }


        return Response.ok(new ResponseEntityView(result)).build();
    }

    private Language saveOrUpdateLanguage(final String languageId, final LanguageForm form)
            throws AlreadyExistException {
        final Language newLanguage = new Language();

        if (StringUtils.isSet(languageId)) {
            final Language origLanguage = this.languageAPI.getLanguage(languageId);
            Sneaky.sneaked(()->BeanUtils.copyProperties(newLanguage, origLanguage));
            newLanguage.setId(origLanguage.getId());
        }

        newLanguage.setLanguageCode(form.getLanguageCode());
        newLanguage.setLanguage(form.getLanguage());
        newLanguage.setCountryCode(form.getCountryCode());
        newLanguage.setCountry(form.getCountry());

        DotPreconditions.checkArgument(UtilMethods.isSet(newLanguage.getLanguageCode()),
                "Language Code can't be null or empty");

        final Language existingLang = languageAPI.getLanguage(newLanguage.getLanguageCode(), newLanguage.getCountryCode());
        if(null != existingLang && Long.parseLong(languageId) != existingLang.getId()){
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
        return this.languageAPI.getLanguage(form.getLanguageCode(), form.getCountryCode());
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
        return Response.ok(new ResponseEntityView(newDefault)).build(); // 200
    }
}
