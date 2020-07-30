package com.dotcms.rest.api.v2.languages;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.keyvalue.model.KeyValue;
import com.dotcms.rendering.velocity.viewtools.util.ConversionUtils;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.I18NForm;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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

//        final List<LanguageView> languageViews = languages.stream().map(LanguageView::new).collect(
//                Collectors.toList());

        return Response.ok(new ResponseEntityView(languages.stream().map(LanguageView::new).collect(
                Collectors.toList()))).build();
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
            final LanguageForm languageForm) {
        this.webResource.init(null, request, response,
                true, PortletID.LANGUAGES.toString());
        DotPreconditions.notNull(languageForm,"Expected Request body was empty.");
        final Language language = saveOrUpdateLanguage(null, languageForm);
        return Response.ok(new ResponseEntityView(language)).build(); // 200
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
            final LanguageForm languageForm) {
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

    private Language saveOrUpdateLanguage(final String languageId, final LanguageForm form) {
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

        this.languageAPI.saveLanguage(newLanguage);
        return newLanguage;
    }

    private boolean doesLanguageExist(final String languageId) {
        return languageAPI.getLanguage(languageId)!=null &&
                languageAPI.getLanguage(languageId).getId()>0;
    }
}
