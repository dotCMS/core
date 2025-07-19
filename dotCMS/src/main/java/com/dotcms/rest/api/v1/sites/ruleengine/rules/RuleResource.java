package com.dotcms.rest.api.v1.sites.ruleengine.rules;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;

import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.InternalServerException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
public class RuleResource {

    private final RulesAPI rulesAPI;
    private final RuleTransform ruleTransform = new RuleTransform();
    private final WebResource webResource;

    @SuppressWarnings("unused")
    public RuleResource() {
        this(new ApiProvider());
    }

    private RuleResource(ApiProvider apiProvider) {
        this(apiProvider, new WebResource(apiProvider));
    }

    @VisibleForTesting
    protected RuleResource(ApiProvider apiProvider, WebResource webResource) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.webResource = webResource;
    }

    @Operation(
        summary = "List rules",
        description = "Returns all rules defined for the specified site"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Rules retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "Map of rule keys to RestRule objects containing all rules for the site"))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid site ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Path("/rules")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, RestRule> list(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String parentId) throws DotSecurityException, DotDataException {

        parentId = checkNotEmpty(parentId, BadRequestException.class, "Site Id is required.");
        User user = getUser(request, response);
        final List<RestRule> restRules = getRulesInternal(parentId, user);
        Map<String, RestRule> hash = Maps.newHashMapWithExpectedSize(restRules.size());
        for (RestRule restRule : restRules) {
            hash.put(restRule.key, restRule);
        }

        return hash;
    }

    @Operation(
        summary = "Get rule by ID",
        description = "Retrieves a specific rule by its identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Rule retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = RestRule.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid site ID or rule ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Rule not found",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Path("/rules/{ruleId}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public RestRule self(@Context HttpServletRequest request, @Context final HttpServletResponse response, 
                        @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId, 
                        @Parameter(description = "Rule identifier", required = true) @PathParam("ruleId") String ruleId) {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        User user = getUser(request, response);
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule Id is required.");
        return getRuleInternal(ruleId, user);
    }

    @Operation(
        summary = "Create rule",
        description = "Creates a new rule for the specified site"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Rule created successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "JSON object containing the created rule ID in format: { 'id': 'rule-uuid' }"))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid site ID or rule data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @Path("/rules")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest request, @Context final HttpServletResponse response, 
                       @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId, 
                       @io.swagger.v3.oas.annotations.parameters.RequestBody(
                           description = "Rule data", 
                           required = true,
                           content = @Content(schema = @Schema(implementation = RestRule.class))
                       ) RestRule restRule) {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site id is required.");
        User user = getUser(request, response);
        Ruleable proxy =  getParent(siteId, user);
        String ruleId = createRuleInternal(proxy.getIdentifier(), restRule, user);

        try {
            @SuppressWarnings("unused")
            URI path = new URI(ruleId);
            return Response.ok("{ \"id\": \"" + ruleId + "\" }").build();
        } catch (URISyntaxException e) {
            throw new InternalServerException(e, "Could not create valid URI to Rule id '%s'", ruleId);
        }
    }

    @Operation(
        summary = "Update rule",
        description = "Updates an existing rule"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Rule updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = RestRule.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid parameters or rule data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Rule not found",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @JSONP
    @Path("/rules/{ruleId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestRule update(@Context HttpServletRequest request, @Context final HttpServletResponse response, 
                          @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId, 
                          @Parameter(description = "Rule identifier", required = true) @PathParam("ruleId") String ruleId, 
                          @io.swagger.v3.oas.annotations.parameters.RequestBody(
                              description = "Updated rule data", 
                              required = true,
                              content = @Content(schema = @Schema(implementation = RestRule.class))
                          ) RestRule restRule) {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule Id is required.");
        User user = getUser(request, response);

        updateRuleInternal(user, new RestRule.Builder().from(restRule).key(ruleId).build());

        return restRule;
    }

    @Operation(
        summary = "Delete rule",
        description = "Removes a rule from the system"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", 
                    description = "Rule deleted successfully"),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid site ID or rule ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Rule not found",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/rules/{ruleId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response remove(@Context HttpServletRequest request, @Context final HttpServletResponse response, 
                          @Parameter(description = "Site identifier", required = true) @PathParam("siteId") String siteId, 
                          @Parameter(description = "Rule identifier", required = true) @PathParam("ruleId") String ruleId) {
        User user = getUser(request, response);

        try {
            Ruleable proxy =  getParent(siteId, user);
            Rule rule = getRule(ruleId, user);
            HibernateUtil.startTransaction();
            rulesAPI.deleteRule(rule, user, false);
            HibernateUtil.closeAndCommitTransaction();
            return Response.status(HttpStatus.SC_NO_CONTENT).build();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        } finally {
            HibernateUtil.closeSessionSilently();
        }
    }

    private Ruleable getParent(String identifier, User user) {
        class ParentProxy  extends PermissionableProxy implements Ruleable{ }
    	
	   	ParentProxy proxy = new ParentProxy();
	   	proxy.setIdentifier(identifier);
    	
    	return proxy;
    }


    private Rule getRule(String ruleId, User user) {
        try {
            Rule rule = rulesAPI.getRuleById(ruleId, user, false);
            if(rule == null) {
                throw new NotFoundException("Rule not found: '%s'", ruleId);
            }
            return rule;
        } catch (DotDataException e) {
            // @todo ggranum: These messages potentially expose internal details to consumers, via response headers. See Note 1 in HttpStatusCodeException.
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private User getUser(final HttpServletRequest request, final HttpServletResponse response) {
        return webResource.init(request, response, true).getUser();
    }

    private List<RestRule> getRulesInternal(
            final String parentId,
            final User user) throws DotSecurityException, DotDataException {

        List<RestRule> restRules = new ArrayList<>();

        final List<Rule> rules = rulesAPI.getAllRulesByParent(parentId, user, false);
        for (Rule rule : rules) {
            try{
                restRules.add(ruleTransform.appToRest(rule, user));
            } catch (Exception transEx) {
                String ruleName = UtilMethods.isSet(rule.getName()) ? rule.getName() : "N/A";
                Logger.error(this, "Error parsing Rule named: " + ruleName + " to ReST: " + transEx.getMessage());
            }
        }
        return restRules;
    }

    private RestRule getRuleInternal(String ruleId, User user) {
        Rule input = getRule(ruleId, user);
        RestRule restRule = ruleTransform.appToRest(input, user);
        return restRule;
    }

    private String createRuleInternal(String siteId, RestRule restRule, User user) {
        try {
            Rule rule = ruleTransform.restToApp(restRule, user);
            rule.setParent(siteId);
            rulesAPI.saveRule(rule, user, false);
            return rule.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private String updateRuleInternal(User user, RestRule restRule) {
        try {
            Rule rule = rulesAPI.getRuleById(restRule.key, user, false);
            if(rule == null) {
                throw new NotFoundException("Rule with key '%s' not found: ", restRule.key);
            }
            rule = ruleTransform.applyRestToApp(restRule, rule, user);
            rulesAPI.saveRule(rule, user, false);
            return rule.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }
}
