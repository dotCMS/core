package com.dotcms.rest.api.v2.languages;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.beanutils.BeanUtils;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.I18NForm;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

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

        return Response.ok(new ResponseEntityView(ImmutableList.copyOf(languages))).build();
    }

    /**
     * Persists a new {@link Language}
     *
     * @param request HttpServletRequest
     * @param languageForm LanguageForm
     * @return Response
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
     * @return Response
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
     * @return Response
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

    private Language saveOrUpdateLanguage(final String languageId, final LanguageForm form) {
        final Language newLanguage = new Language();

        if (StringUtils.isSet(languageId)) {
            final Language origLanguage = this.languageAPI.getLanguage(languageId);
            Sneaky.sneaked(()->BeanUtils.copyProperties(newLanguage, origLanguage));
        }

        newLanguage.setLanguageCode(form.getLanguageCode());
        newLanguage.setLanguage(form.getLanguage());
        newLanguage.setCountryCode(form.getCountryCode());
        newLanguage.setCountry(form.getCountry());

        this.languageAPI.saveLanguage(newLanguage);
        return newLanguage;
    }

    private boolean doesLanguageExist(String languageId) {
        return languageAPI.getLanguage(languageId)!=null &&
                languageAPI.getLanguage(languageId).getId()>0;
    }
}
