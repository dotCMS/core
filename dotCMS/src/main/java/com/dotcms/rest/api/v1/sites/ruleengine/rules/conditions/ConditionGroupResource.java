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
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
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

@Path("/v1/sites/{siteId}/ruleengine")
@Tag(name = "Rules Engine")
public class ConditionGroupResource  {

    private final RulesAPI rulesAPI;
    private final WebResource webResource;
    private final ConditionGroupTransform groupTransform = new ConditionGroupTransform();
    private HostAPI hostAPI;

    @SuppressWarnings("unused")
    public ConditionGroupResource() {
        this(new ApiProvider());
    }

    private ConditionGroupResource(ApiProvider apiProvider) {
        this(apiProvider, new WebResource(apiProvider));
    }

    @VisibleForTesting
    protected ConditionGroupResource(ApiProvider apiProvider, WebResource webResource) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.hostAPI = apiProvider.hostAPI();
        this.webResource = webResource;
    }

    @GET
    @NoCache
    @Path("/rules/{ruleId}/conditionGroups")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@Context HttpServletRequest request,
                         @Context final HttpServletResponse response,
                         @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId)
            throws JSONException {

        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule Id is required.");
        User user = getUser(request, response);
        getHost(siteId, user);
        Rule rule = getRule(ruleId, user);
        List<RestConditionGroup> restConditionGroups = getGroupsInternal(user, rule);
        java.util.Map<String, RestConditionGroup> hash = restConditionGroups.stream()
                .collect(Collectors.toMap(restGroup -> restGroup.id, Function.identity()));

        return Response.ok(hash).build();
    }

    @GET
    @NoCache
    @Path("/rules/{ruleId}/conditionGroups/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    public RestConditionGroup self(@Context HttpServletRequest request,
                                   @Context final HttpServletResponse response,
                                      @PathParam("siteId") String siteId,
                                      @PathParam("ruleId") String ruleId,
                                      @PathParam("groupId") String groupId) throws JSONException {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule Id is required.");
        User user = getUser(request, response);
        getHost(siteId, user);
        getRule(ruleId, user);
        groupId = checkNotEmpty(groupId, BadRequestException.class, "Condition Group Id is required.");
        return getGroupInternal(groupId, user);
    }

    /**
     * <p>Saves a Condition Group
     * <br>
     * <p/>
     * Usage: /rules/
     */
    @POST
    @Path("/rules/{ruleId}/conditionGroups")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest request,
                        @Context final HttpServletResponse response,
                                       @PathParam("siteId") String siteId,
                                       @PathParam("ruleId") String ruleId,
                                       RestConditionGroup conditionGroup) throws JSONException {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site id is required.");
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule id is required.");
        User user = getUser(request, response);
        getHost(siteId, user);
        getRule(ruleId, user);

        String conditionGroupId = createConditionGroupInternal(ruleId, conditionGroup, user);

        try {
            URI path = new URI(ruleId);
            return Response.ok().type(MediaType.APPLICATION_JSON).entity("{ \"id\": \"" + conditionGroupId + "\" }").build();
        } catch (URISyntaxException e) {
            throw new InternalServerException(e, "Could not create valid URI to Rule id '%s'", ruleId);
        }
    }

    /**
     * <p>Updates a Condition Group
     * <br>
     * <p/>
     * Usage: /rules/
     */
    @PUT
    @Path("/rules/{ruleId}/conditionGroups/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestConditionGroup update(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                                @PathParam("siteId") String siteId,
                                                @PathParam("ruleId") String ruleId,
                                                @PathParam("groupId") String groupId,
                                                RestConditionGroup conditionGroup) throws JSONException {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule Id is required.");
        User user = getUser(request, response);
        Host host = getHost(siteId, user); // forces check that host exists. This should be handled by rulesAPI?
        Rule rule = getRule(ruleId, user);

        updateConditionGroupInternal(user, rule.getId(), groupId, conditionGroup);

        return conditionGroup;
    }

    /**
     * <p>Deletes a Condition Group and all its child Conditions
     * <br>
     * <p/>
     * Usage: DELETE api/rules-engine/conditionGroups
     */

    @DELETE
    @Path("/rules/{ruleId}/conditionGroups/{conditionGroupId}")
    public Response remove(@Context HttpServletRequest request,
                           @Context final HttpServletResponse response,
                                         @PathParam("siteId") String siteId,
                                         @PathParam("ruleId") String ruleId,
                                         @PathParam("conditionGroupId") String groupId) throws JSONException {
        User user = getUser(request, response);

        try {
            getHost(siteId, user);
            getRule(ruleId, user);
            ConditionGroup group = getConditionGroup(groupId, user);
            rulesAPI.deleteConditionGroup(group, user, false);

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
    Rule getRule(String ruleId, User user) {
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

    private RestConditionGroup getGroupInternal(String groupId, User user) {
        ConditionGroup input = getConditionGroup(groupId, user);
        return groupTransform.appToRest(input);
    }

    private List<RestConditionGroup> getGroupsInternal(User user, Rule rule) {
        try {
            List<RestConditionGroup> restConditionGroups = new ArrayList<>();

            List<ConditionGroup> groups = rulesAPI.getConditionGroupsByRule(rule.getId(), user, false);
            for (ConditionGroup group : groups) {
                restConditionGroups.add(groupTransform.appToRest(group));
            }

            return restConditionGroups;

        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    @VisibleForTesting
    ConditionGroup getConditionGroup(String groupId, User user) {
        try {
            ConditionGroup group = rulesAPI.getConditionGroupById(groupId, user, false);
            if(group == null) {
                throw new NotFoundException("ConditionGroup not found: '%s'", groupId);
            }
            return group;
        } catch (DotDataException e) {
            // @todo ggranum: These messages potentially expose internal details to consumers,
            // @todo via response headers. See Note 1 in HttpStatusCodeException.
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private String createConditionGroupInternal(String ruleId, RestConditionGroup restConditionGroup, User user) {
        try {
            ConditionGroup conditionGroup = groupTransform.restToApp(restConditionGroup);
            conditionGroup.setRuleId(ruleId);
            rulesAPI.saveConditionGroup(conditionGroup, user, false);
            return conditionGroup.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private String updateConditionGroupInternal(User user, String ruleId, String conditionGroupId, RestConditionGroup restConditionGroup) {
        try {
            ConditionGroup conditionGroup = rulesAPI.getConditionGroupById(conditionGroupId, user, false);
            checkNotNull(conditionGroup, NotFoundException.class, "Condition Group with id '%s' not found: ", conditionGroupId);
            conditionGroup =  groupTransform.applyRestToApp(restConditionGroup, conditionGroup);
            rulesAPI.saveConditionGroup(conditionGroup, user, false);
            return conditionGroup.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }
}
