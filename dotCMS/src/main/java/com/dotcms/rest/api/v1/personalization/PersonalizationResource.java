package com.dotcms.rest.api.v1.personalization;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
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
@SwaggerCompliant(value = "Core authentication and user management APIs", batch = 1)
@Tag(name = "Personalization")
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


    @Operation(
        summary = "Personalize page containers",
        description = "Copies the current content associated to page containers with default personalization and creates a new set with the specified persona personalization. Requires edit permission on the page."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Page containers personalized successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityPersonalizationView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid page ID, persona tag, or missing parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient edit permissions on page",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Page or persona not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/pagepersonas")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public Response personalizePageContainers (@Context final HttpServletRequest  request,
                                               @Context final HttpServletResponse response,
                                               @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                   description = "Personalization form data with page ID and persona tag", 
                                                   required = true,
                                                   content = @Content(schema = @Schema(implementation = PersonalizationPersonaPageForm.class))
                                               ) final PersonalizationPersonaPageForm personalizationPersonaPageForm) throws DotDataException, DotSecurityException {

        final User user = this.webResource.init(true, request, true).getUser();
        final boolean respectFrontEndRoles = PageMode.get(request).respectAnonPerms;

        Logger.debug(this, ()-> "Personalizing all containers on the page personas per page: " + personalizationPersonaPageForm.getPageId());

        if (this.personaAPI.findPersonaByTag(personalizationPersonaPageForm.getPersonaTag(), user, true).isEmpty()) {

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

        final String newPersonalization = Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON +
                personalizationPersonaPageForm.getPersonaTag();
        final String currentVariantId = WebAPILocator.getVariantWebAPI().currentVariantId();

        return Response.ok(new ResponseEntityView<>(
                        this.multiTreeAPI.copyPersonalizationForPage(
                            personalizationPersonaPageForm.getPageId(), newPersonalization, currentVariantId)
                        )).build();
    } // personalizePageContainers

    @Operation(
        summary = "Delete page personalization",
        description = "Deletes a personalization persona for a page. Can remove any persona personalization for page containers except the default personalization. Requires edit permission on the page."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Page personalization deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid parameters, trying to delete default personalization, or persona doesn't exist",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient edit permissions on page",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Page not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/pagepersonas/page/{pageId}/personalization/{personalization}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public Response personalizePageContainers (@Context final HttpServletRequest  request,
                                               @Context final HttpServletResponse response,
                                               @Parameter(description = "Page identifier", required = true) @PathParam("pageId") final String  pageId,
                                               @Parameter(description = "Personalization/persona tag to delete", required = true) @PathParam("personalization") final String personalization) throws DotDataException, DotSecurityException {

        final User user = this.webResource.init(true, request, true).getUser();
        final boolean respectFrontEndRoles = PageMode.get(request).respectAnonPerms;

        Logger.debug(this, ()-> "Deleting all Personalizing:" + personalization
                + ", on all containers on the page personas per page: " + pageId);

        if (!UtilMethods.isSet(pageId) || !UtilMethods.isSet(personalization)) {

            throw new BadRequestException(
                    "Page or Personalization parameter are missing, should use: /pagepersonas/page/{pageId}/personalization/{personalization}");
        }

        if (MultiTree.DOT_PERSONALIZATION_DEFAULT.equalsIgnoreCase(personalization) ||
                this.personaAPI.findPersonaByTag(personalization, user, true).isEmpty()) {

            throw new BadRequestException("Persona tag: " + personalization + " does not exist or the user does not have permissions to it");
        }

        final Contentlet pageContentlet = contentletAPI.findContentletByIdentifierAnyLanguage(pageId);
        if(!permissionAPI.doesUserHavePermission(pageContentlet, PermissionAPI.PERMISSION_EDIT, user, respectFrontEndRoles)){
            Logger.warn(PersonalizationResource.class,String.format("User `%s` does not have edit permission over page `%s` therefore personalization isn't allowed.  ",user.getUserId(), pageId));
            return Response.status(Status.FORBIDDEN).build();
        }

        final String currentVariantId = WebAPILocator.getVariantWebAPI().currentVariantId();
        this.multiTreeAPI.deletePersonalizationForPage(pageId,
                Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + personalization,
                currentVariantId);

        return Response.ok(new ResponseEntityView<>("OK")).build();
    } // personalizePageContainers
}
