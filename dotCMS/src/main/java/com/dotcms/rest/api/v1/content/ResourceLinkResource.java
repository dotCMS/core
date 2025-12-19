package com.dotcms.rest.api.v1.content;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityMapStringObjectView;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ResourceLink;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Exposes a {@link com.dotmarketing.portlets.contentlet.model.ResourceLink} by inode or id
 * @author jsanca
 */
@SwaggerCompliant(value = "Content management and workflow APIs", batch = 2)
@Path("/v1/content/resourcelinks")
@Tag(name = "Content")
public class ResourceLinkResource {

    private final WebResource    webResource;
    private final ContentletAPI contentletAPI;

    public ResourceLinkResource() {
        this(new WebResource(), APILocator.getContentletAPI());
    }

    @VisibleForTesting
    protected ResourceLinkResource(final WebResource webResource, final ContentletAPI contentletAPI) {

        this.webResource   = webResource;
        this.contentletAPI = contentletAPI;
    }

    /**
     * Given an inode or identifier this will build get you a Resource Link for a specific field
     * The inode nor identifier, is expected other wise you'll get exception
     * @param request    {@link HttpServletRequest} http request
     * @param response   {@link HttpServletResponse} http response
     * @param field      {{@link String} field variable name
     * @param inode      {@link String} asset inode
     * @param identifier {@link String} identifier
     * @param language   {@link String} optional parameter
     * @return Response
     * @throws DotDataException
     * @throws DotStateException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Get resource link for specific field",
        description = "Retrieves a resource link for a specific field of a contentlet identified by inode or identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Resource link retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - missing inode/identifier parameter",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - download restricted",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Not found - contentlet or field not found",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Path("field/{field}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response findResourceLink(@Context final HttpServletRequest  request,
                                     @Context final HttpServletResponse response,
                                     @Parameter(description = "Field variable name", required = true)
                                     @PathParam("field")             final String field,
                                     @Parameter(description = "Content inode")
                                     @QueryParam("inode")            final String inode,
                                     @Parameter(description = "Content identifier")
                                     @QueryParam("identifier")       final String identifier,
                                     @Parameter(description = "Language ID", example = "1")
                                     @DefaultValue("-1") @QueryParam("language") final String language) throws DotStateException, DotSecurityException, DotDataException {

        if (!UtilMethods.isSet(inode) && !UtilMethods.isSet(identifier)) {

            throw new IllegalArgumentException("Missing required inode/identifier param");
        }

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource).requiredBackendUser(true).requiredFrontendUser(false)
                        .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user       = initData.getUser();
        final long languageId = LanguageUtil.getLanguageId(language);
        final PageMode mode   = PageMode.get(request);

        try {
            final Contentlet contentlet = this.getContentlet(inode, identifier, languageId,
                    () -> WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(), initData, mode);

            Logger.debug(this, () -> "Finding the resource link for the contentlet: " + contentlet.getIdentifier());

            if (!contentlet.getContentType().fieldMap().containsKey(field)) {

                return Response.status(Response.Status.NOT_FOUND).build();
            }

            final ResourceLink link = new ResourceLink.ResourceLinkBuilder().build(request, user, contentlet, field);
            if (link.isDownloadRestricted()) {

                throw new DotSecurityException("The Resource link to the contentlet is restricted.");
            }

            return Response.ok(new ResponseEntityView<>(this.toMapView(contentlet, link))).build();
        }catch (DoesNotExistException e) {

            return Response.ok(new ResponseEntityView<>(Collections.emptyMap())).build();
        }
    }

    private Contentlet getContentlet(final String inode,
                                     final String identifier,
                                     final long language,
                                     final Supplier<Long> sessionLanguage,
                                     final InitDataObject initDataObject,
                                     final PageMode pageMode) throws DotDataException, DotSecurityException {

        Contentlet contentlet = null;
        try {
            PageMode mode = pageMode;

            if (UtilMethods.isSet(inode)) {

                Logger.debug(this, () -> "Finding the contentlet by inode: " + inode);
                contentlet = this.contentletAPI.find
                        (inode, initDataObject.getUser(), mode.respectAnonPerms);

                DotPreconditions.notNull(contentlet, () -> "contentlet-was-not-found", DoesNotExistException.class);
            } else if (UtilMethods.isSet(identifier)) {

                Logger.debug(this, () -> "Finding the contentlet by identifier: " + identifier);
                mode = PageMode.EDIT_MODE; // when asking for identifier it is always edit
                final Optional<Contentlet> currentContentlet = language <= 0 ?
                        Optional.ofNullable(this.contentletAPI.findContentletByIdentifier(identifier, mode.showLive,
                                sessionLanguage.get(), initDataObject.getUser(), mode.respectAnonPerms)) :
                        this.contentletAPI.findContentletByIdentifierOrFallback
                                (identifier, mode.showLive, language, initDataObject.getUser(), mode.respectAnonPerms);

                DotPreconditions.isTrue(currentContentlet.isPresent(), () -> "contentlet-was-not-found", DoesNotExistException.class);
                contentlet = currentContentlet.get();

            }
        } catch (DoesNotExistException e) {

            throw e;
        } catch(DotRuntimeException e) {

            Logger.error(this, e.getMessage(), e);
            DotPreconditions.notNull(contentlet, () -> "contentlet-was-not-found", DoesNotExistException.class);
            throw e;
        }

        return contentlet;
    }

    /**
     * Given an inode or identifier this will build get you a Resource Link
     * This will retrieve all binaries field for the contentlet
     * The inode nor identifier, is expected other wise you'll get exception
     * @param request    {@link HttpServletRequest} http request
     * @param response   {@link HttpServletResponse} http response
     * @param inode      {@link String} asset inode
     * @param identifier {@link String} identifier
     * @param language   {@link String} optional parameter
     * @return Response
     * @throws DotDataException
     * @throws DotStateException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Get all resource links for contentlet",
        description = "Retrieves resource links for all binary fields of a contentlet identified by inode or identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Resource links retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - missing inode/identifier parameter",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - download restricted",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response findResourceLinks(@Context final HttpServletRequest  request,
                                      @Context final HttpServletResponse response,
                                      @Parameter(description = "Content inode")
                                      @QueryParam("inode")            final String inode,
                                      @Parameter(description = "Content identifier")
                                      @QueryParam("identifier")       final String identifier,
                                      @Parameter(description = "Language ID", example = "1")
                                      @DefaultValue("-1") @QueryParam("language") final String language) throws DotStateException, DotSecurityException, DotDataException {

        if (!UtilMethods.isSet(inode) && !UtilMethods.isSet(identifier)) {

            throw new IllegalArgumentException("Missing required inode/identifier param");
        }

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource).requiredBackendUser(true).requiredFrontendUser(false)
                        .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user       = initData.getUser();
        final long languageId = LanguageUtil.getLanguageId(language);
        final PageMode mode   = PageMode.get(request);
        try {
            final Contentlet contentlet = this.getContentlet(inode, identifier, languageId,
                    () -> WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(), initData, mode);
            final Map<String, Object> resourceLinkMap = new TreeMap<>();

            Logger.debug(this, () -> "Finding the resource links for the contentlet: " + contentlet.getIdentifier());

            for (final Field field : contentlet.getContentType().fields(BinaryField.class)) {

                final String fieldName = field.variable();
                final ResourceLink link = new ResourceLink.ResourceLinkBuilder().build(request, user, contentlet, fieldName);
                if (link.isDownloadRestricted()) {

                    throw new DotSecurityException("The Resource link to the contentlet is restricted.");
                }

                resourceLinkMap.put(fieldName, this.toMapView(contentlet, link));
            }

            return Response.ok(new ResponseEntityView<>(resourceLinkMap)).build();
        } catch (DoesNotExistException e) {

            return Response.ok(new ResponseEntityView<>(Collections.emptyList())).build();
        }
    }

    private Map<String, Object> toMapView (final Contentlet contentlet, final ResourceLink link) {

        final ImmutableMap.Builder mapBuilder =
                new ImmutableMap.Builder<String, Object>();

        if (contentlet.isFileAsset()) {
            mapBuilder.put("href", link.getResourceLinkAsString());
        }

        return mapBuilder.put("text",               link.getResourceLinkUriAsString())
                .put("mimeType",           link.getMimeType())
                .put("idPath",             link.getIdPath())
                .put("versionPath",        link.getVersionPath())
                .put("configuredImageURL", link.getConfiguredImageURL())
                .build();
    }
}
