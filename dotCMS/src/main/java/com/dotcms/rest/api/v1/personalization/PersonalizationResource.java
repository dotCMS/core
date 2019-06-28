package com.dotcms.rest.api.v1.personalization;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * Resource to provide personalization stuff on dotCMS
 */
@Path("/v1/personalization")
public class PersonalizationResource {

    private final PersonaAPI    personaAPI;
    private final WebResource   webResource;
    private final MultiTreeAPI  multiTreeAPI;



    @SuppressWarnings("unused")
    public PersonalizationResource() {
        this(APILocator.getPersonaAPI(), APILocator.getMultiTreeAPI(), new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    protected PersonalizationResource(final PersonaAPI personaAPI,
                                      final MultiTreeAPI multiTreeAPI,
                                      final WebResource webResource) {

        this.personaAPI    = personaAPI;
        this.multiTreeAPI  = multiTreeAPI;
        this.webResource   = webResource;
    }


    /**
     * Copies the current content associated to the page containers with the personalization personas as {@link com.dotmarketing.beans.MultiTree#DOT_PERSONALIZATION_DEFAULT}
     * and will set the a new same set of them, but with the personalization on the personalizationPersonaPageForm.personaTag
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param  personalizationPersonaPageForm {@link PersonalizationPersonaPageForm} (pageId, personaTag)
     * @return Response, list of MultiTrees with the new personalization
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @POST
    @Path("/pagepersonas")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response personalizePageContainers (@Context final HttpServletRequest  request,
                                               @Context final HttpServletResponse response,
                                               final PersonalizationPersonaPageForm personalizationPersonaPageForm) throws DotDataException, DotSecurityException {

        final User user = this.webResource.init(true, request, true).getUser();
        final boolean respectFrontEndRoles = PageMode.get(request).respectAnonPerms;

        Logger.debug(this, ()-> "Personalizing all containers on the page personas per page: " + personalizationPersonaPageForm.getPageId());

        if (!this.personaAPI.findPersonaByTag(personalizationPersonaPageForm.getPersonaTag(), user, respectFrontEndRoles).isPresent()) {

            throw new BadRequestException("Does not exists a Persona with the tag: " + personalizationPersonaPageForm.getPersonaTag());
        }

        return Response.ok(new ResponseEntityView(
                        this.multiTreeAPI.copyPersonalizationForPage(
                            personalizationPersonaPageForm.getPageId(),
                            Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + personalizationPersonaPageForm.getPersonaTag())
                        )).build();
    } // personalizePageContainers

    /**
     * Deletes a personalization persona for a page, can remove any persona personalization for a page container except {@link com.dotmarketing.beans.MultiTree#DOT_PERSONALIZATION_DEFAULT}
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @DELETE
    @Path("/pagepersonas/page/{pageId}/personalization/{personalization}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response personalizePageContainers (@Context final HttpServletRequest  request,
                                               @Context final HttpServletResponse response,
                                               @PathParam("pageId") final String  pageId,
                                               @PathParam("personalization") final String personalization) throws DotDataException, DotSecurityException {

        final User user = this.webResource.init(true, request, true).getUser();
        final boolean respectFrontEndRoles = PageMode.get(request).respectAnonPerms;

        Logger.debug(this, ()-> "Deleting all Personalizing:" + personalization
                + ", on all containers on the page personas per page: " + pageId);

        if (!UtilMethods.isSet(pageId) || !UtilMethods.isSet(personalization)) {

            throw new BadRequestException(
                    "Page or Personalization parameter are missing, should use: /pagepersonas/page/{pageId}/personalization/{personalization}");
        }

        if (MultiTree.DOT_PERSONALIZATION_DEFAULT.equalsIgnoreCase(personalization) ||
                !this.personaAPI.findPersonaByTag(personalization, user, respectFrontEndRoles).isPresent()) {

            throw new BadRequestException("Does not exists a Persona with the tag: " + personalization);
        }

        this.multiTreeAPI.deletePersonalizationForPage(pageId, Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + personalization);

        return Response.ok(new ResponseEntityView("OK")).build();
    } // personalizePageContainers
}
