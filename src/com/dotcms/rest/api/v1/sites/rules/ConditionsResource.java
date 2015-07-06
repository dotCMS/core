package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
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
import com.dotcms.repackage.net.sf.hibernate.collection.Map;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import static com.dotcms.rest.validation.Preconditions.checkNotEmpty;
import static com.dotcms.rest.validation.Preconditions.checkNotNull;

@Path("/v1")
public class ConditionsResource {

    private final RulesAPI rulesAPI;
    private final AuthenticationProvider authProxy;
    private final ConditionTransform conditionTransform = new ConditionTransform();
    private HostAPI hostAPI;

    public ConditionsResource() {
        this(new ApiProvider());
    }

    private ConditionsResource(ApiProvider apiProvider) {
        this(apiProvider, new AuthenticationProvider(apiProvider));
    }

    @VisibleForTesting
    protected ConditionsResource(ApiProvider apiProvider, AuthenticationProvider authProxy) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.hostAPI = apiProvider.hostAPI();
        this.authProxy = authProxy;
    }

    /**
     * <p>Returns a JSON with the Condition Groups and its Conditions for the rule with the given ruleId.
     * <br>Each Rule node contains all fields in  .
     * <p/>
     * Usage: /conditions/
     */

    @GET
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@Context HttpServletRequest request,
                                  @PathParam("siteId") String siteId,
                                  @PathParam("ruleId") String ruleId,
                                  @PathParam("groupId") String groupId) throws JSONException {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        User user = getUser(request);
        getHost(siteId, user);
        getRule(ruleId, user);
        ConditionGroup group = getConditionGroup(groupId, user);
        List<RestCondition> restConditions = getConditionsInternal(user, group);
        java.util.Map<String, RestCondition> hash = restConditions.stream()
                .collect(Collectors.toMap(restCondition -> restCondition.id, Function.identity()));

        return Response.ok(hash).build();
    }

    /**
     * <p>Returns a JSON with the Condition Groups and its Conditions for the rule with the given ruleId.
     * <br>Each Rule node contains all fields in  .
     * <p/>
     * <p>If a conditionId is provided, it will return the condition whose id matches the provided conditionId.
     * <p/>
     * Usage: /conditions/
     */
    @GET
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}/conditions/{conditionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public RestCondition self(@Context HttpServletRequest request,
                                 @PathParam("siteId") String siteId,
                                 @PathParam("ruleId") String ruleId,
                                 @PathParam("groupId") String groupId,
                                 @PathParam("conditionId") String conditionId) throws JSONException {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        User user = getUser(request);
        getHost(siteId, user);
        getConditionGroup(groupId, user);
        conditionId = checkNotEmpty(conditionId, BadRequestException.class, "Condition Id is required.");
        return getConditionInternal(conditionId, user);
    }

    /**
     * <p>Saves a new Condition
     * <br>
     * <p/>
     * Usage: /rules/
     */
    @POST
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest request,
                                  @PathParam("siteId") String siteId,
                                  @PathParam("ruleId") String ruleId,
                                  @PathParam("groupId") String groupId,
                                  RestCondition condition) throws DotDataException, DotSecurityException, JSONException {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site id is required.");
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule id is required.");
        groupId = checkNotEmpty(groupId, BadRequestException.class, "Condition Group id is required.");
        User user = getUser(request);
        getHost(siteId, user);
        getRule(ruleId, user);
        ConditionGroup group = getConditionGroup(groupId, user);
        String conditionId = createConditionInternal(group.getId(), condition, user);

        try {
            URI path = new URI(ruleId);
            return Response.ok().type(MediaType.APPLICATION_JSON).entity("{ \"id\": \"" + conditionId + "\" }").build();
        } catch (URISyntaxException e) {
            throw new InternalServerException(e, "Could not create valid URI to Rule id '%s'", ruleId);
        }
    }

    /**
     * <p>Updates a Condition
     * <br>
     * <p/>
     * Usage: PUT /rules/conditiongroups/{groupId}/conditions
     */
    @PUT
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}/conditions/{conditionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestCondition update(@Context HttpServletRequest request,
                                      @PathParam("siteId") String siteId,
                                      @PathParam("ruleId") String ruleId,
                                      @PathParam("groupId") String groupId,
                                      @PathParam("conditionId") String conditionId,
                                      RestCondition restCondition) throws DotDataException, DotSecurityException, JSONException {

        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule Id is required.");
        groupId = checkNotEmpty(ruleId, BadRequestException.class, "Condition Group Id is required.");
        conditionId = checkNotEmpty(ruleId, BadRequestException.class, "Condition Id is required.");
        User user = getUser(request);
        Host host = getHost(siteId, user); // forces check that host exists. This should be handled by rulesAPI?

        updateConditionInternal(user, conditionId, restCondition);

        return restCondition;
    }

    /**
     * <p>Deletes a Condition
     * <br>
     * <p/>
     * Usage: DELETE api/rules-engine/rules
     */
    @DELETE
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}/conditions/{conditionId}")
    public Response remove(@Context HttpServletRequest request,
                                    @PathParam("siteId") String siteId,
                                    @PathParam("groupId") String groupId,
                                    @PathParam("ruleId") String ruleId,
                                    @PathParam("conditionId") String conditionId) throws JSONException {
        User user = getUser(request);

        try {
            getHost(siteId, user);
            getRule(ruleId, user);
            getConditionGroup(groupId, user);
            Condition condition = getCondition(conditionId, user);
            rulesAPI.deleteCondition(condition, user, false);

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
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private List<RestCondition> getConditionsInternal(User user, ConditionGroup group) {
        try {
            List<Condition> conditions = rulesAPI.getConditionsByConditionGroup(group.getId(), user, false);
            return conditions.stream().map(conditionTransform.appToRestFn()).collect(Collectors.toList());
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private RestCondition getConditionInternal(String conditionId, User user) {
        Condition input = getCondition(conditionId, user);
        return conditionTransform.appToRest(input);
    }

    private String createConditionInternal(String groupId, RestCondition restCondition, User user) {
        try {
            Condition condition = conditionTransform.restToApp(restCondition);
            condition.setConditionGroup(groupId);
            rulesAPI.saveCondition(condition, user, false);
            return condition.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }


    private String updateConditionInternal(User user, String conditionId, RestCondition restCondition) {
        try {
            Condition condition = rulesAPI.getConditionById(conditionId, user, false);
            checkNotNull(condition,NotFoundException.class, "Condition with id '%s' not found: ", conditionId);
            conditionTransform.applyRestToApp(restCondition, condition);
            rulesAPI.saveCondition(condition, user, false);
            return condition.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

}
