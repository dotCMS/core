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

    /**
     * <p>Returns a JSON representation of the Rule with the given ruleId
     * <p/>
     * Usage: GET api/rules-engine/sites/sites/{siteId}/rules/{ruleId}
     */
    @GET
    @NoCache
    @JSONP
    @Path("/actions/{actionId}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public RestRuleAction self(@Context HttpServletRequest request,
                         @Context final HttpServletResponse response,
                         @PathParam("siteId") String siteId,
                         @PathParam("actionId") String actionId) {

        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        User user = getUser(request, response);
        getHost(siteId, user);
        return getActionInternal(actionId, user);
    }


    /**
     * <p>Saves a Rule Action
     * <br>
     * <p/>
     * Usage: /rules/
     */

    @POST
    @Path("/actions/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest request,
                        @Context final HttpServletResponse response,
                                   @PathParam("siteId") String siteId,
                                   RestRuleAction ruleAction) throws JSONException {
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

    /**
     * <p>Updates the Rule Action with the given id
     * <br>
     * <p/>
     * Usage: /rules/
     */

    @PUT
    @Path("/actions/{actionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestRuleAction update(@Context HttpServletRequest request,
                                 @Context final HttpServletResponse response,
                                           @PathParam("siteId") String siteId,
                                           @PathParam("actionId") String actionId,
                                           RestRuleAction ruleAction) throws JSONException {
        User user = getUser(request, response);
        getHost(siteId, user); // forces check that host exists. This should be handled by rulesAPI?

        updateRuleActionInternal(user, actionId, ruleAction);

        return ruleAction;
    }

    /**
     * <p>Deletes the RuleAction with the given ruleActionId
     * <br>
     * <p/>
     * Usage: DELETE api/rules-engine/rules/ruleActions/{ruleActionId}
     */
    @DELETE
    @Path("/actions/{actionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(@Context HttpServletRequest request,
                           @Context final HttpServletResponse response,
                                     @PathParam("siteId") String siteId,
                                     @PathParam("actionId") String actionId) throws JSONException {
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
