package com.dotcms.rest.api.v1.personalization;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.server.JSONP;

/**
 * Resource to provide personalization stuff on dotCMS
 */
@Path("/v1/personalization")
public class PersonalizationResource {

    private final PersonaAPI    personaAPI;
    private final WebResource   webResource;
    private final MultiTreeAPI  multiTreeAPI;
    private final ContentletAPI contentletAPI;
    private final PermissionAPI permissionAPI;


    @SuppressWarnings("unused")
    public PersonalizationResource() {
        this(APILocator.getPersonaAPI(), APILocator.getMultiTreeAPI(),
                APILocator.getContentletAPI(), APILocator.getPermissionAPI(),
                new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    protected PersonalizationResource(final PersonaAPI personaAPI,
            final MultiTreeAPI multiTreeAPI,
            final ContentletAPI contentletAPI,
            final PermissionAPI permissionAPI,
            final WebResource webResource) {

        this.personaAPI = personaAPI;
        this.multiTreeAPI = multiTreeAPI;
        this.contentletAPI = contentletAPI;
        this.permissionAPI = permissionAPI;
        this.webResource = webResource;
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

        if (!this.personaAPI.findPersonaByTag(personalizationPersonaPageForm.getPersonaTag(), user, true).isPresent()) {

            throw new BadRequestException("Does not exists a Persona with the tag: " + personalizationPersonaPageForm.getPersonaTag());
        }

        final String pageId = personalizationPersonaPageForm.getPageId();

        if (!UtilMethods.isSet(pageId)) {
            throw new BadRequestException(
                    "Page parameter is missing");
        }

        final Contentlet pageContentlet = contentletAPI.findContentletByIdentifierAnyLanguage(pageId);
        if(!permissionAPI.doesUserHavePermission(pageContentlet, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)){
            Logger.warn(PersonalizationResource.class,String.format("User `%s` does not have edit permission over page `%s` therefore personalization isn't allowed.  ",user.getUserId(), pageId));
            return Response.status(Status.FORBIDDEN).build();
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
                !this.personaAPI.findPersonaByTag(personalization, user, true).isPresent()) {

            throw new BadRequestException("Persona tag: " + personalization + " does not exist or the user does not have permissions to it");
        }

        final Contentlet pageContentlet = contentletAPI.findContentletByIdentifierAnyLanguage(pageId);
        if(!permissionAPI.doesUserHavePermission(pageContentlet, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)){
            Logger.warn(PersonalizationResource.class,String.format("User `%s` does not have edit permission over page `%s` therefore personalization isn't allowed.  ",user.getUserId(), pageId));
            return Response.status(Status.FORBIDDEN).build();
        }

        this.multiTreeAPI.deletePersonalizationForPage(pageId, Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + personalization);

        return Response.ok(new ResponseEntityView("OK")).build();
    } // personalizePageContainers
}
