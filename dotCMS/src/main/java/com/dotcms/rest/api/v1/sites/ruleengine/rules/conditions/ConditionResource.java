package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

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
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotmarketing.portlets.rules.exception.RuleConstructionFailedException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.liferay.portal.model.User;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;
import static com.dotcms.util.DotPreconditions.checkNotNull;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

@Path("/v1/sites/{siteId}/ruleengine")
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

    /**
     * <p>Returns a JSON with the Condition Groups and its Conditions for the rule with the given ruleId.
     * <br>Each Rule node contains all fields in  .
     * <p>
     * <p>If a conditionId is provided, it will return the condition whose id matches the provided conditionId.
     * <p>
     * Usage: /conditions/{conditionId}
     */
    @GET
    @NoCache
    @Path("/conditions/{conditionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response self(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("conditionId") String conditionId)
            throws JSONException {
        User user = getUser(request);

        try {
            getHost(siteId, user);
            Condition condition = checkNotNull(rulesAPI.getConditionById(conditionId, user, false)
                , BadRequestException.class, "Not valid Condition");
            RestCondition restCondition = conditionTransform.appToRest(condition);
            return Response.ok(restCondition).build();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException | InvalidLicenseException e) {
            throw new ForbiddenException(e, e.getMessage());
        } 
    }

    /**
     * <p>Saves a new Condition
     * <br>
     * <p>
     * Usage: /rules/
     */
    @POST
    @Path("/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest request, @PathParam("siteId") String siteId, RestCondition condition) {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site id is required.");
        condition = checkNotNull(condition, BadRequestException.class, "Condition is required.");
        User user = getUser(request);
        Host host = getHost(siteId, user);
        String conditionId = createConditionInternal(condition, user);

        try {
            URI path = new URI(conditionId);
            return Response.ok().type(MediaType.APPLICATION_JSON).entity("{ \"id\": \"" + conditionId + "\" }").build();
        } catch (URISyntaxException e) {
            throw new InternalServerException(e, "Could not create valid URI to Rule id '%s'", conditionId);
        }
    }

    /**
     * <p>Updates a Condition
     * <br>
     * <p>
     * Usage: PUT /rules/conditiongroups/{groupId}/conditions
     */
    @PUT
    @Path("/conditions/{conditionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestCondition update(@Context HttpServletRequest request,
                                @PathParam("siteId") String siteId,
                                @PathParam("conditionId") String conditionId,
                                RestCondition restCondition) throws DotDataException, DotSecurityException, JSONException {

        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        conditionId = checkNotEmpty(conditionId, BadRequestException.class, "Condition Id is required.");
        User user = getUser(request);
        Host host = getHost(siteId, user); // forces check that host exists. This should be handled by rulesAPI?

        updateConditionInternal(user, conditionId, restCondition);

        return restCondition;
    }

    /**
     * <p>Deletes a Condition
     * <br>
     * <p>
     * Usage: DELETE api/rules-engine/rules
     */
    @DELETE
    @Path("/conditions/{conditionId}")
    public Response remove(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("conditionId") String conditionId)
            throws JSONException {
        User user = getUser(request);

        try {
            getHost(siteId, user);
            Condition condition = getCondition(conditionId, user);
            rulesAPI.deleteCondition(condition, user, false);

            return Response.status(HttpStatus.SC_NO_CONTENT).build();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException | InvalidLicenseException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    @VisibleForTesting
    User getUser(@Context HttpServletRequest request) {
        return webResource.init(true, request, true).getUser();
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
        } catch (DotSecurityException | InvalidLicenseException e) {
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
        } catch (DotSecurityException | InvalidLicenseException e) {
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
        } catch (DotSecurityException | InvalidLicenseException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }
}
