package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;
import static com.dotcms.util.DotPreconditions.checkNotNull;

import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotmarketing.util.json.JSONException;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.InternalServerException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.rules.exception.RuleConstructionFailedException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.dotcms.rest.annotation.SwaggerCompliant;

@SwaggerCompliant(value = "Rules engine and business logic APIs", batch = 6)
@Path("/v1/sites/{siteId}/ruleengine")
@Tag(name = "Rules Engine")
public class ConditionResource {

    private final RulesAPI rulesAPI;
    private final WebResource webResource;
    private final ConditionTransform conditionTransform;
    private HostAPI hostAPI;

    public ConditionResource() {
        this(new ApiProvider());
    }

    private ConditionResource(ApiProvider apiProvider) {
        this(apiProvider, new WebResource(apiProvider));
    }

    @VisibleForTesting
    protected ConditionResource(ApiProvider apiProvider, WebResource webResource) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.hostAPI = apiProvider.hostAPI();
        this.webResource = webResource;
        this.conditionTransform = new ConditionTransform();
    }

    @Operation(
        summary = "Get condition by ID",
        description = "Retrieves a specific rule condition by its identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Condition retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = RestCondition.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid site ID or condition ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Site or condition not found",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @NoCache
    @Path("/conditions/{conditionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response self(@Context HttpServletRequest request, @Context final HttpServletResponse response, 
                        @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId, 
                        @Parameter(description = "Condition identifier", required = true) @PathParam("conditionId") String conditionId)
            throws JSONException {
        User user = getUser(request, response);

        try {
            getHost(siteId, user);
            Condition condition = checkNotNull(rulesAPI.getConditionById(conditionId, user, false)
                , BadRequestException.class, "Not valid Condition");
            RestCondition restCondition = conditionTransform.appToRest(condition);
            return Response.ok(restCondition).build();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        } 
    }

    @Operation(
        summary = "Create rule condition",
        description = "Creates a new rule condition for the specified site"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Condition created successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "JSON object containing the created condition ID in format: { 'id': 'condition-uuid' }"))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid site ID or condition data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Site not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest request, @Context final HttpServletResponse response, 
                       @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId, 
                       @io.swagger.v3.oas.annotations.parameters.RequestBody(
                           description = "Rule condition data", 
                           required = true,
                           content = @Content(schema = @Schema(implementation = RestCondition.class))
                       ) RestCondition condition) {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site id is required.");
        condition = checkNotNull(condition, BadRequestException.class, "Condition is required.");
        User user = getUser(request, response);
        Host host = getHost(siteId, user);
        String conditionId = createConditionInternal(condition, user);

        try {
            URI path = new URI(conditionId);
            return Response.ok().type(MediaType.APPLICATION_JSON).entity("{ \"id\": \"" + conditionId + "\" }").build();
        } catch (URISyntaxException e) {
            throw new InternalServerException(e, "Could not create valid URI to Rule id '%s'", conditionId);
        }
    }

    @Operation(
        summary = "Update rule condition",
        description = "Updates an existing rule condition"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Condition updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = RestCondition.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid parameters or condition data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Site or condition not found",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/conditions/{conditionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestCondition update(@Context HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId,
                                @Parameter(description = "Condition identifier", required = true) @PathParam("conditionId") String conditionId,
                                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                    description = "Updated rule condition data", 
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = RestCondition.class))
                                ) RestCondition restCondition) throws DotDataException, DotSecurityException, JSONException {

        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        conditionId = checkNotEmpty(conditionId, BadRequestException.class, "Condition Id is required.");
        User user = getUser(request, response);
        Host host = getHost(siteId, user); // forces check that host exists. This should be handled by rulesAPI?

        updateConditionInternal(user, conditionId, restCondition);

        return restCondition;
    }

    @Operation(
        summary = "Delete rule condition",
        description = "Removes a rule condition from the system"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", 
                    description = "Condition deleted successfully"),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid site ID or condition ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Site or condition not found",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/conditions/{conditionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response remove(@Context HttpServletRequest request, @Context final HttpServletResponse response, 
                          @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId, 
                          @Parameter(description = "Condition identifier", required = true) @PathParam("conditionId") String conditionId)
            throws JSONException {
        User user = getUser(request, response);

        try {
            getHost(siteId, user);
            Condition condition = getCondition(conditionId, user);
            rulesAPI.deleteCondition(condition, user, false);

            return Response.status(HttpStatus.SC_NO_CONTENT).build();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    @VisibleForTesting
    User getUser(final HttpServletRequest request, final HttpServletResponse response) {
        return webResource.init(request, response, true).getUser();
    }

    @VisibleForTesting
    private Host getHost(String siteId, User user) {


    	Host host  = new Host();
    	host.setIdentifier(siteId);
    	return host;
    }

    @VisibleForTesting
    Condition getCondition(String conditionId, User user) {
        try {
            Condition condition = rulesAPI.getConditionById(conditionId, user, false);
            if(condition == null) {
                throw new NotFoundException("Condition not found: '%s'", conditionId);
            }
            return condition;
        } catch (DotDataException e) {
            // @todo ggranum: These messages potentially expose internal details to consumers, via response headers. See Note 1 in HttpStatusCodeException.
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException  e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private String createConditionInternal(RestCondition restCondition, User user) {
        try {
            Condition condition = conditionTransform.restToApp(restCondition);

            rulesAPI.saveCondition(condition, user, false);
            return condition.getId();
        }  catch (DotDataException | DotRuntimeException | RuleConstructionFailedException e ) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private String updateConditionInternal(User user, String conditionId, RestCondition restCondition) {
        try {
            Condition condition = rulesAPI.getConditionById(conditionId, user, false);
            if(condition == null) {
                throw new NotFoundException("Condition with id '%s' not found: ", conditionId);
            }
            condition= conditionTransform.applyRestToApp(restCondition, condition);
            rulesAPI.saveCondition(condition, user, false);
            return condition.getId();
        } catch (DotDataException | DotRuntimeException | RuleConstructionFailedException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException  e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }
}
