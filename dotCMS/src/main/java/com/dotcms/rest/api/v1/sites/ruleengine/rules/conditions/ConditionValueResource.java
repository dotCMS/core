package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Lists;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;
import static com.dotcms.util.DotPreconditions.checkNotNull;

@Path("/v1/sites/{siteId}/ruleengine")
@Tag(name = "Rules Engine")
public class ConditionValueResource {

    private final RulesAPI rulesAPI;
    private final WebResource webResource;
    private final ParameterModelTransform parameterModelTransform;
    private HostAPI hostAPI;

    public ConditionValueResource() {
        this(new ApiProvider());
    }

    private ConditionValueResource(ApiProvider apiProvider) {
        this(apiProvider, new WebResource(apiProvider));
    }

    @VisibleForTesting
    protected ConditionValueResource(ApiProvider apiProvider, WebResource webResource) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.hostAPI = apiProvider.hostAPI();
        this.webResource = webResource;
        this.parameterModelTransform = new ParameterModelTransform();
    }

    @GET
    @NoCache
    @Path("/conditions/{conditionId}/conditionValues")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam("siteId") String siteId, @PathParam("conditionId") String conditionId)
            throws JSONException {

        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        conditionId = checkNotEmpty(conditionId, BadRequestException.class, "Condition Id is required.");
        User user = getUser(request, response);
        getHost(siteId, user);
        Condition condition = getCondition(conditionId, user);
        List<RestConditionValue> restConditionValues = getValuesInternal(user, condition);
        java.util.Map<String, RestConditionValue> hash = restConditionValues.stream()
                .collect(Collectors.toMap(restConditionValue -> restConditionValue.id, Function.identity()));

        return Response.ok(hash).build();
    }

    /**
     * <p>Returns a JSON with the Condition Groups and its Conditions for the rule with the given ruleId.
     * <br>Each Rule node contains all fields in  .
     * <p>
     * <p>If a conditionId is provided, it will return the condition whose id matches the provided conditionId.
     * <p>
     * Usage: /conditions/
     */
    @GET
    @NoCache
    @Path("/conditions/{conditionId}/conditionValues/{valueId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response self(@Context HttpServletRequest request,
                         @Context final HttpServletResponse response,
                         @PathParam("siteId") String siteId,
                         @PathParam("conditionId") String conditionId,
                         @PathParam("valueId") String valueId)
            throws JSONException {
        User user = getUser(request, response);

        try {
            getHost(siteId, user);
            ParameterModel value = rulesAPI.getConditionValueById(valueId, user, false);
            RestConditionValue restConditionValue = parameterModelTransform.toRest(value);
            return Response.ok(restConditionValue).build();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
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
    @Path("/conditions/{conditionId}/conditionValues")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(@Context HttpServletRequest request,
                        @Context final HttpServletResponse response,
                        @PathParam("siteId") String siteId,
                        @PathParam("conditionId") String conditionId,
                        RestConditionValue conditionValue) {
        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site id is required.");
        conditionId = checkNotNull(conditionId, BadRequestException.class, "Condition id is required.");
        conditionValue = checkNotNull(conditionValue, BadRequestException.class, "Condition Value is required.");
        User user = getUser(request, response);
        getHost(siteId, user);
        getCondition(conditionId, user);
        String conditionValueId = createConditionValueInternal(conditionId, conditionValue, user);

        try {
            URI path = new URI(conditionValueId);
            return Response.ok().type(MediaType.APPLICATION_JSON).entity("{ \"id\": \"" + conditionValueId + "\" }").build();
        } catch (URISyntaxException e) {
            throw new InternalServerException(e, "Could not create valid URI to Condition Value id '%s'", conditionValueId);
        }
    }

    /**
     * <p>Updates a Condition
     * <br>
     * <p>
     * Usage: PUT /rules/conditiongroups/{groupId}/conditions
     */
    @PUT
    @Path("/conditions/{conditionId}/conditionValues/{valueId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestConditionValue update(@Context HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                @PathParam("siteId") String siteId,
                                @PathParam("conditionId") String conditionId,
                                @PathParam("valueId") String valueId,
                                RestConditionValue restConditionValue) throws DotDataException, DotSecurityException, JSONException {

        siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
        conditionId = checkNotEmpty(conditionId, BadRequestException.class, "Condition Id is required.");
        User user = getUser(request, response);
        getHost(siteId, user); // forces check that host exists. This should be handled by rulesAPI?
        getCondition(conditionId, user); // forces check that condition exists. This should be handled by rulesAPI?

        updateConditionValueInternal(user, valueId, restConditionValue);

        return restConditionValue;
    }

    /**
     * <p>Deletes a Condition
     * <br>
     * <p>
     * Usage: DELETE api/rules-engine/rules
     */
    @DELETE
    @Path("/conditions/{conditionId}/conditionValues/{valueId}")
    public Response remove(@Context HttpServletRequest request,
                           @Context final HttpServletResponse response,
                           @PathParam("siteId") String siteId,
                           @PathParam("conditionId") String conditionId,
                           @PathParam("valueId") String valueId)
            throws JSONException {
        User user = getUser(request, response);

        try {
            siteId = checkNotEmpty(siteId, BadRequestException.class, "Site Id is required.");
            conditionId = checkNotEmpty(conditionId, BadRequestException.class, "Condition Id is required.");
            getHost(siteId, user);
            getCondition(conditionId, user);
            ParameterModel value = getConditionValue(valueId, user);
            rulesAPI.deleteConditionValue(value, user, false);

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

    @VisibleForTesting
    ParameterModel getConditionValue(String valueId, User user) {
        try {
            ParameterModel value = rulesAPI.getConditionValueById(valueId, user, false);
            if(value == null) {
                throw new NotFoundException("Condition Value not found: '%s'", valueId);
            }
            return value;
        } catch (DotDataException e) {
            // @todo ggranum: These messages potentially expose internal details to consumers, via response headers. See Note 1 in HttpStatusCodeException.
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private List<RestConditionValue> getValuesInternal(User user, Condition condition) {
        List<ParameterModel> values = condition.getValues();
        return Lists.transform(values, parameterModelTransform.toRestFn);
    }

    private String createConditionValueInternal(String conditionId, RestConditionValue restValue, User user) {
        try {
            ParameterModel parameterModel = parameterModelTransform.toAppFn.apply(restValue);
            parameterModel.setOwnerId(conditionId);

            rulesAPI.saveConditionValue(parameterModel, user, false);
            return parameterModel.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private String updateConditionValueInternal(User user, String valueId, RestConditionValue restValue) {
        try {
            ParameterModel parameterModel = rulesAPI.getConditionValueById(valueId, user, false);
            if(parameterModel == null) {
                throw new NotFoundException("Condition Value with id '%s' not found: ", valueId);
            }
            parameterModel = parameterModelTransform.applyRestToApp(restValue, parameterModel);
            parameterModel.setId(valueId);
            rulesAPI.saveConditionValue(parameterModel, user, false);
            return parameterModel.getId();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }
}
