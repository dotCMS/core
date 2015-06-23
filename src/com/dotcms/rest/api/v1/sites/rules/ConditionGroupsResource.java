package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.DELETE;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.config.AuthenticationProvider;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.InternalServerException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import static com.dotcms.rest.validation.Preconditions.checkNotEmpty;

@Path("/v1")
public class ConditionGroupsResource extends WebResource {

    private final RulesAPI rulesAPI;
    private final AuthenticationProvider authProxy;
    private HostAPI hostAPI;

    public ConditionGroupsResource() {
        this(new ApiProvider());
    }

    private ConditionGroupsResource(ApiProvider apiProvider) {
        this(apiProvider, new AuthenticationProvider(apiProvider));
    }

    @VisibleForTesting
    protected ConditionGroupsResource(ApiProvider apiProvider, AuthenticationProvider authProxy) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.hostAPI = apiProvider.hostAPI();
        this.authProxy = authProxy;
    }

    @GET
    @Path("sites/{siteId}/rules/{ruleId}/conditiongroups")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConditionGroups(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId)
            throws JSONException {

        User user = getUser(request);

        try {
            getHost(siteId, user);
            getRule(ruleId, user);

            JSONObject groupsJSON = new JSONObject();
            List<ConditionGroup> conditionGroups = rulesAPI.getConditionGroupsByRule(ruleId, user, false);

            for (ConditionGroup conditionGroup : conditionGroups) {

                JSONObject groupJSON = new com.dotmarketing.util.json.JSONObject(conditionGroup, new String[]{"operator", "priority"});

                List<Condition> conditions = rulesAPI.getConditionsByConditionGroup(conditionGroup.getId(), user, false);

                JSONObject conditionsJSON = new JSONObject();

                for (Condition condition : conditions) {
                    JSONObject conditionJSON = new com.dotmarketing.util.json.JSONObject(condition);

                    JSONObject valuesJSON = new JSONObject();
                    for (ConditionValue value : condition.getValues()) {
                        valuesJSON.put(value.getId(), new com.dotmarketing.util.json.JSONObject(value, new String[]{"value", "priority"}));
                    }

                    conditionJSON.put("values", valuesJSON);
                    conditionsJSON.put(condition.getId(), conditionJSON);
                }

                groupJSON.put("conditions", conditionsJSON);

                groupsJSON.put(conditionGroup.getId(), groupJSON);
            }

            return Response.ok(groupsJSON.toString(), MediaType.APPLICATION_JSON).build();
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Conditions", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConditionGroup(@Context HttpServletRequest request,
                                      @PathParam("siteId") String siteId,
                                      @PathParam("ruleId") String ruleId,
                                      @PathParam("groupId") String groupId) throws JSONException {
        User user = getUser(request);

        try {

            getHost(siteId, user);
            getRule(ruleId, user);
            ConditionGroup group = getConditionGroup(groupId, user);

            JSONObject groupJSON = new com.dotmarketing.util.json.JSONObject(group, new String[]{"operator", "priority"});

            List<Condition> conditions = rulesAPI.getConditionsByConditionGroup(group.getId(), user, false);

            JSONObject conditionsJSON = new JSONObject();

            for (Condition condition : conditions) {
                JSONObject conditionJSON = new com.dotmarketing.util.json.JSONObject(condition);

                JSONObject valuesJSON = new JSONObject();
                for (ConditionValue value : condition.getValues()) {
                    valuesJSON.put(value.getId(), new com.dotmarketing.util.json.JSONObject(value, new String[]{"value", "priority"}));
                }

                conditionJSON.put("values", valuesJSON);
                conditionsJSON.put(condition.getId(), conditionJSON);
            }

            groupJSON.put("conditions", conditionsJSON);

            return Response.ok(groupJSON.toString(), MediaType.APPLICATION_JSON).build();
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Conditions", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Saves a Condition Group
     * <br>
     * <p/>
     * Usage: /rules/
     */
    @POST
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postConditionGroup(@Context HttpServletRequest request,
                                       @PathParam("siteId") String siteId,
                                       @PathParam("ruleId") String ruleId,
                                       RestConditionGroup conditionGroup) throws JSONException {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site id is required.");
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule id is required.");
        User user = getUser(request);
        Host host = getHost(siteId, user);
        Rule rule = getRule(ruleId, user);

        String conditionGroupId = createConditionGroupInternal(rule.getId(), conditionGroup, user);

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
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestConditionGroup putConditionGroup(@Context HttpServletRequest request,
                                                @PathParam("siteId") String siteId,
                                                @PathParam("ruleId") String ruleId,
                                                @PathParam("groupId") String groupId,
                                                RestConditionGroup conditionGroup) throws JSONException {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule Id is required.");
        User user = getUser(request);
        Host host = getHost(siteId, user); // forces check that host exists. This should be handled by rulesAPI?
        Rule rule = getRule(ruleId, user);

        updateConditionGroupInternal(user, rule.getId(), groupId, conditionGroup);

        return conditionGroup;
    }

    /**
     * <p>Deletes a Condition Group and all its child Conditions
     * <br>
     * <p/>
     * Usage: DELETE api/rules-engine/conditiongroups
     */

    @DELETE
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{conditionGroupId}")
    public Response deleteConditionGroup(@Context HttpServletRequest request,
                                         @PathParam("siteId") String siteId,
                                         @PathParam("ruleId") String ruleId,
                                         @PathParam("conditionGroupId") String groupId) throws JSONException {
        User user = getUser(request);

        try {
            getHost(siteId, user);
            getRule(ruleId, user);
            ConditionGroup group = getConditionGroup(groupId, user);
            rulesAPI.deleteConditionGroup(group, user, false);

            return Response.status(HttpStatus.SC_NO_CONTENT).build();
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @VisibleForTesting
    User getUser(@Context HttpServletRequest request) {
        return authProxy.authenticate(request);
    }

    @VisibleForTesting
    Host getHost(String siteId, User user) {
        try {
            Host host = hostAPI.find(siteId, user, false);
            if(host == null) {
                throw new NotFoundException("Site not found: '%s'", siteId);
            }
            return host;
        } catch (DotDataException e) {
            // @todo ggranum: These messages potentially expose internal details to consumers, via response headers. See Note 1 in HttpStatusCodeException.
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
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

    @VisibleForTesting
    ConditionGroup getConditionGroup(String groupId, User user) {
        try {
            ConditionGroup group = rulesAPI.getConditionGroupById(groupId, user, false);
            if(group == null) {
                throw new NotFoundException("ConditionGroup not found: '%s'", groupId);
            }
            return group;
        } catch (DotDataException e) {
            // @todo ggranum: These messages potentially expose internal details to consumers, via response headers. See Note 1 in HttpStatusCodeException.
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private String createConditionGroupInternal(String ruleId, RestConditionGroup restConditionGroup, User user) {
        try {
            ConditionGroup conditionGroup = new ConditionGroup();
            applyRestConditionGroupToConditionGroup(ruleId, restConditionGroup, conditionGroup);
            rulesAPI.saveConditionGroup(conditionGroup, user, false);
            return conditionGroup.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private void applyRestConditionGroupToConditionGroup(String ruleId, RestConditionGroup restConditionGroup, ConditionGroup conditionGroup) {
        conditionGroup.setRuleId(ruleId);
        conditionGroup.setOperator(Condition.Operator.valueOf(restConditionGroup.getOperator()));
        conditionGroup.setPriority(restConditionGroup.getPriority());
    }

    private String updateConditionGroupInternal(User user, String ruleId, String conditionGroupId, RestConditionGroup restConditionGroup) {
        try {
            ConditionGroup conditionGroup = rulesAPI.getConditionGroupById(conditionGroupId, user, false);
            if(conditionGroup == null) {
                throw new NotFoundException("Condition Group with id '%s' not found: ", conditionGroupId);
            }
            applyRestConditionGroupToConditionGroup(ruleId, restConditionGroup, conditionGroup);
            rulesAPI.saveConditionGroup(conditionGroup, user, false);
            return conditionGroup.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }
}
