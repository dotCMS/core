package com.dotcms.rest.api.v1.sites.ruleengine.rules.actions;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;

import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotmarketing.util.json.JSONException;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.InternalServerException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.exception.InvalidActionInstanceException;
import com.dotmarketing.portlets.rules.model.RuleAction;
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
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.annotation.SwaggerCompliant;

@SwaggerCompliant(value = "Rules engine and business logic APIs", batch = 6)
@Path("/v1/sites/{siteId}/ruleengine")
@Tag(name = "Rules Engine")
public class ActionResource {

    private final RulesAPI rulesAPI;
    private final WebResource webResource;
    private final RuleActionTransform actionTransform = new RuleActionTransform();
    private HostAPI hostAPI;

    public ActionResource() {
        this(new ApiProvider());
    }

    private ActionResource(ApiProvider apiProvider) {
        this(apiProvider, new WebResource(apiProvider));
    }

    @VisibleForTesting
    protected ActionResource(ApiProvider apiProvider, WebResource webResource) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.hostAPI = apiProvider.hostAPI();
        this.webResource = webResource;
    }

    @Operation(
        summary = "Get rule action by ID",
        description = "Retrieves a specific rule action by its identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Rule action retrieved successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid site ID or action ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Site or rule action not found",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @NoCache
    @JSONP
    @Path("/actions/{actionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public RestRuleAction self(@Context HttpServletRequest request,
                         @Context final HttpServletResponse response,
                         @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId,
                         @Parameter(description = "Rule action identifier", required = true) @PathParam("actionId") String actionId) {

        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        User user = getUser(request, response);
        getHost(siteId, user);
        return getActionInternal(actionId, user);
    }


    @Operation(
        summary = "Create rule action",
        description = "Creates a new rule action for the specified site"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Rule action created successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid site ID or rule action data",
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
    @Path("/actions/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest request,
                        @Context final HttpServletResponse response,
                                   @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId,
                                   @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                       description = "Rule action data", 
                                       required = true,
                                       content = @Content(schema = @Schema(implementation = RestRuleAction.class))
                                   ) RestRuleAction ruleAction) throws JSONException {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site id is required.");
        User user = getUser(request, response);
        getHost(siteId, user);
        String newId = createRuleActionInternal(ruleAction, user);
        try {
            new URI(newId);
        } catch (URISyntaxException e) {
            throw new InternalServerException(e, "Could not create valid URI to RuleAction id '%s'", newId);
        }
        return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity("{ \"id\": \"" + newId + "\" }").build();
    }

    @Operation(
        summary = "Update rule action",
        description = "Updates an existing rule action"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Rule action updated successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid parameters or rule action data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Site or rule action not found",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/actions/{actionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestRuleAction update(@Context HttpServletRequest request,
                                 @Context final HttpServletResponse response,
                                           @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId,
                                           @Parameter(description = "Rule action identifier", required = true) @PathParam("actionId") String actionId,
                                           @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                               description = "Updated rule action data", 
                                               required = true,
                                               content = @Content(schema = @Schema(implementation = RestRuleAction.class))
                                           ) RestRuleAction ruleAction) throws JSONException {
        User user = getUser(request, response);
        getHost(siteId, user); // forces check that host exists. This should be handled by rulesAPI?

        updateRuleActionInternal(user, actionId, ruleAction);

        return ruleAction;
    }

    @Operation(
        summary = "Delete rule action",
        description = "Removes a rule action from the system"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", 
                    description = "Rule action deleted successfully"),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid site ID or action ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Site or rule action not found",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/actions/{actionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response remove(@Context HttpServletRequest request,
                           @Context final HttpServletResponse response,
                                     @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId,
                                     @Parameter(description = "Rule action identifier", required = true) @PathParam("actionId") String actionId) throws JSONException {
        User user = getUser(request, response);

        try {
            getHost(siteId, user);
            RuleAction action = getRuleAction(actionId, user);
            rulesAPI.deleteRuleAction(action, user, false);

            return Response.status(HttpStatus.SC_NO_CONTENT).build();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        } 
    }

    private User getUser(final HttpServletRequest request, final HttpServletResponse response) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
                return initData.getUser();
    }

    @VisibleForTesting
    private Host getHost(String siteId, User user) {
    	Host host  = new Host();
    	host.setIdentifier(siteId);
    	return host;
    }

    private RuleAction getRuleAction(String ruleActionId, User user) {
        try {
            RuleAction ruleAction = rulesAPI.getRuleActionById(ruleActionId, user, false);
            if(ruleAction == null) {
                throw new NotFoundException("Rule Action not found: '%s'", ruleActionId);
            }
            return ruleAction;
        } catch (DotDataException e) {
            // @todo ggranum: These messages potentially expose internal details to consumers, via response headers. See Note 1 in HttpStatusCodeException.
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }


    private RestRuleAction getActionInternal(String ruleActionId, User user) {
        try {
            RuleAction ruleAction = rulesAPI.getRuleActionById(ruleActionId, user, false);
            if(ruleAction == null) {
                throw new NotFoundException("Rule Action not found: '%s'", ruleActionId);
            }
            return actionTransform.appToRest(ruleAction);
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException  e) {
            throw new ForbiddenException(e, e.getMessage());
        }

    }

    private String createRuleActionInternal(RestRuleAction restRuleAction, User user) {
        try {
            RuleAction action = actionTransform.restToApp(restRuleAction);
            validateActionInstance(action);
            rulesAPI.saveRuleAction(action, user, false);
            return action.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }catch(Exception e){
            throw new BadRequestException(e, e.getMessage());
        }
    }

    private String updateRuleActionInternal(User user, String ruleActionId, RestRuleAction restRuleAction) {
        try {
            RuleAction ruleAction = rulesAPI.getRuleActionById(ruleActionId, user, false);
            if(ruleAction == null) {
                throw new NotFoundException("Rule Action with id '%s' not found: ", ruleActionId);
            }
            ruleAction = actionTransform.applyRestToApp(restRuleAction, ruleAction);
            validateActionInstance(ruleAction);
            ruleAction.setId(ruleActionId);

            rulesAPI.saveRuleAction(ruleAction, user, false);
            return ruleAction.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private void validateActionInstance(RuleAction ruleAction) throws DotDataException, DotSecurityException {
        RuleActionlet actionlet = rulesAPI.findActionlet(ruleAction.getActionlet());
        if(actionlet == null){
            throw new BadRequestException("Actionlet with id '%s' not found: ", ruleAction.getActionlet());
        }
        try {
            actionlet.doCheckValid(ruleAction);
        } catch (InvalidActionInstanceException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
