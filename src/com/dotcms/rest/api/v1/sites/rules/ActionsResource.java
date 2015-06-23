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
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import static com.dotcms.rest.validation.Preconditions.checkNotEmpty;

@Path("/v1")
public class ActionsResource {

    private final RulesAPI rulesAPI;
    private final AuthenticationProvider authProxy;
    private HostAPI hostAPI;

    public ActionsResource() {
        this(new ApiProvider());
    }

    private ActionsResource(ApiProvider apiProvider) {
        this(apiProvider, new AuthenticationProvider(apiProvider));
    }

    @VisibleForTesting
    protected ActionsResource(ApiProvider apiProvider, AuthenticationProvider authProxy) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.hostAPI = apiProvider.hostAPI();
        this.authProxy = authProxy;
    }

    /**
     * <p>Returns a JSON with the RuleActions defined for the Rule with the given ruleId.
     * <p/>
     */

    @GET
    @Path("sites/{siteId}/rules/{ruleId}/actions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRuleActions(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId)
            throws JSONException {

        User user = getUser(request);

        JSONObject jsonActions = new JSONObject();

        try {
            getHost(siteId, user);
            Rule rule = getRule(ruleId, user);

            List<RuleAction> actions = rulesAPI.getRuleActionsByRule(rule.getId(), user, false);

            for (RuleAction action : actions) {
                JSONObject actionletObject = new JSONObject();
                actionletObject.put("name", action.getName());
                actionletObject.put("actionlet", action.getActionlet());
                jsonActions.put(action.getId(), actionletObject);
            }

            return Response.ok(jsonActions.toString(), MediaType.APPLICATION_JSON).build();
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Rule Action", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Saves a Rule Action
     * <br>
     * <p/>
     * Usage: /rules/
     */

    @POST
    @Path("/sites/{siteId}/rules/{ruleId}/actions/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postRuleAction(@Context HttpServletRequest request,
                                   @PathParam("siteId") String siteId,
                                   @PathParam("ruleId") String ruleId,
                                   RestRuleAction ruleAction) throws JSONException {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site id is required.");
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule id is required.");
        User user = getUser(request);
        Host host = getHost(siteId, user);
        Rule rule = getRule(ruleId, user);

        String ruleActionId = createRuleActionInternal(rule.getId(), ruleAction, user);

        try {
            URI path = new URI(ruleId);
            return Response.ok().type(MediaType.APPLICATION_JSON).entity("{ \"id\": \"" + ruleActionId + "\" }").build();
        } catch (URISyntaxException e) {
            throw new InternalServerException(e, "Could not create valid URI to Rule id '%s'", ruleId);
        }
    }

    /**
     * <p>Updates the Rule Action with the given id
     * <br>
     * <p/>
     * Usage: /rules/
     */

    @PUT
    @Path("/sites/{siteId}/rules/{ruleId}/actions/{actionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestRuleAction updateRuleAction(@Context HttpServletRequest request,
                                           @PathParam("siteId") String siteId,
                                           @PathParam("ruleId") String ruleId,
                                           @PathParam("actionId") String actionId,
                                           RestRuleAction ruleAction) throws JSONException {
        ruleId = checkNotEmpty(ruleId, BadRequestException.class, "Rule Id is required.");
        User user = getUser(request);
        getHost(siteId, user); // forces check that host exists. This should be handled by rulesAPI?

        updateRuleActionInternal(user, ruleId, actionId, ruleAction);

        return ruleAction;
    }

    /**
     * <p>Deletes the RuleAction with the given ruleActionId
     * <br>
     * <p/>
     * Usage: DELETE api/rules-engine/rules/actions/{ruleActionId}
     */
    @DELETE
    @Path("/sites/{siteId}/rules/{ruleId}/actions/{actionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteRuleAction(@Context HttpServletRequest request,
                                     @PathParam("siteId") String siteId,
                                     @PathParam("ruleId") String ruleId,
                                     @PathParam("actionId") String actionId) throws JSONException {
        User user = getUser(request);

        try {
            getHost(siteId, user);
            getRule(ruleId, user);
            RuleAction action = getRuleAction(actionId, user);
            rulesAPI.deleteRuleAction(action, user, false);

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
    RuleAction getRuleAction(String ruleActionId, User user) {
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

    private String createRuleActionInternal(String ruleId, RestRuleAction restRuleAction, User user) {
        try {
            RuleAction ruleAction = new RuleAction();
            applyRestRuleActionToRuleAction(ruleId, restRuleAction, ruleAction);
            rulesAPI.saveRuleAction(ruleAction, user, false);
            return ruleAction.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private void applyRestRuleActionToRuleAction(String ruleId, RestRuleAction restRuleAction, RuleAction ruleAction) {
        ruleAction.setRuleId(ruleId);
        ruleAction.setName(restRuleAction.name);
        ruleAction.setActionlet(restRuleAction.actionlet);
        ruleAction.setPriority(restRuleAction.priority);

        if(restRuleAction.parameters != null && !restRuleAction.parameters.isEmpty()) {
            List<RuleActionParameter> parameters = new ArrayList<>();

            for (RestRuleActionParameter restParameter : restRuleAction.parameters) {
                RuleActionParameter parameter = new RuleActionParameter();
                parameter.setId(restParameter.id);
                parameter.setKey(restParameter.key);
                parameter.setValue(restParameter.value);
                parameters.add(parameter);
            }

            ruleAction.setParameters(parameters);
        }
    }

    private String updateRuleActionInternal(User user, String ruleId, String ruleActionId, RestRuleAction restRuleAction) {
        try {
            RuleAction ruleAction = rulesAPI.getRuleActionById(ruleActionId, user, false);

            if(ruleAction == null) {
                throw new NotFoundException("Rule Action with id '%s' not found: ", ruleId);
            }
            applyRestRuleActionToRuleAction(ruleId, restRuleAction, ruleAction);
            rulesAPI.saveRuleAction(ruleAction, user, false);
            return ruleAction.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }
}
