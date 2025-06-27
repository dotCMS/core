package com.dotcms.rest.api.v1.system.ruleengine.conditionlets;

import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/system/ruleengine")
@Tag(name = "Rules Engine")
public class ConditionletsResource {

    private final RulesAPI rulesAPI;
    private final WebResource webResource;

    public ConditionletsResource() {
        this(new ApiProvider());
    }

    private ConditionletsResource(ApiProvider apiProvider) {
        this(apiProvider, new WebResource(apiProvider));
    }

    @VisibleForTesting
    ConditionletsResource(ApiProvider apiProvider, WebResource webResource) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.webResource = webResource;
    }

    /**
     * <p>Returns a JSON with all the Conditionlet Objects defined.
     * <br>Each Conditionlet node contains only its name
     * <p>
     * Usage: /conditionlets/
     */
    @GET
    @Path("/conditionlets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@Context HttpServletRequest request, final @Context HttpServletResponse response) {
        getUser(request, response);
        return Response.ok(getConditionletsInternal()).build();
    }

//    /**
//     * <p>Returns a JSON with the Comparisons of a given contentlet.
//     * <br>Each Comparisons node contains the id and label
//     * <p>
//     * Usage: /conditionlets/{id}/comparisons
//     */
//    @GET
//    @Path("/conditionlets/{id}/comparisons")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response listComparisons(@Context HttpServletRequest request, @PathParam("id") String conditionletId) throws JSONException {
//        User user = getUser(request);
//
//        JSONObject jsonComparisons = new JSONObject();
//
//        if(!UtilMethods.isSet(conditionletId)) {
//            return Response.ok(jsonComparisons.toString(), MediaType.APPLICATION_JSON).build();
//        }
//
//        try {
//            Conditionlet conditionlet = rulesAPI.findConditionlet(conditionletId);
//
//            if(!UtilMethods.isSet(conditionlet)) {
//                return Response.ok(jsonComparisons.toString(), MediaType.APPLICATION_JSON).build();
//            }
//
//            Set<Comparison> comparisons = conditionlet.getComparisons();
//
//            for (Comparison comparison : comparisons) {
//                JSONObject comparisonJSON = new JSONObject();
//                comparisonJSON.put("name", comparison.getLabel());
//                jsonComparisons.put(comparison.getId(), comparisonJSON);
//            }
//
//            return Response.ok(jsonComparisons.toString(), MediaType.APPLICATION_JSON).build();
//        } catch (DotDataException | DotSecurityException e) {
//            Logger.error(this, "Error getting Conditionlet Comparisons", e);
//            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
//        }
//    }

//    /**
//     * <p>Returns a JSON with the Comparisons of a given contentlet.
//     * <br>Each Comparisons node contains the id and label
//     * <p>
//     * Usage: /conditionlets/{id}/comparisons/{comparison}/inputs
//     */
//    @GET
//    @Path("/conditionlets/{id}/comparisons/{comparison}/inputs")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response listConditionletInputs(
//                                                  @Context HttpServletRequest request,
//                                                  @PathParam("id") String conditionletId,
//                                                  @PathParam("comparison") String comparison) throws JSONException {
//        User user = getUser(request);
//
//        com.dotmarketing.util.json.JSONArray jsonInputs = new com.dotmarketing.util.json.JSONArray();
//
//        if(!UtilMethods.isSet(conditionletId) || !UtilMethods.isSet(comparison)) {
//            return Response.ok(jsonInputs.toString(), MediaType.APPLICATION_JSON).build();
//        }
//
//        try {
//            Conditionlet conditionlet = rulesAPI.findConditionlet(conditionletId);
//
//            if(!UtilMethods.isSet(conditionlet)) {
//                return Response.ok(jsonInputs.toString(), MediaType.APPLICATION_JSON).build();
//            }
//
//            jsonInputs.addAll(conditionlet.getInputs(comparison));
//
//            return Response.ok(jsonInputs.toString(), MediaType.APPLICATION_JSON).build();
//        } catch (DotDataException | DotSecurityException e) {
//            Logger.error(this, "Error getting Conditionlet Inputs", e);
//            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
//        }
//    }

    private Map<String, RestConditionlet> getConditionletsInternal() {
        try {
            List<Conditionlet<?>> conditionlets = rulesAPI.findConditionlets();
            return conditionlets
                           .stream()
                           .map(new ConditionletTransform().appToRestFn())
                           .collect(Collectors.toMap(restCondition -> restCondition.id, Function.identity()));
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        } catch (DotSecurityException  e) {
            throw new ForbiddenException(e, e.getMessage());
        }
    }

    private User getUser(@Context HttpServletRequest request, final @Context HttpServletResponse response) {
         return new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init().getUser();

    }
}
