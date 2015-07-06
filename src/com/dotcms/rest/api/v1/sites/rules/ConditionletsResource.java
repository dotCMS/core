package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
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
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.conditionlet.Comparison;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

@Path("/v1")
public class ConditionletsResource {

    private final RulesAPI rulesAPI;
    private final AuthenticationProvider authProxy;

    public ConditionletsResource() {
        this(new ApiProvider());
    }

    private ConditionletsResource(ApiProvider apiProvider) {
        this(apiProvider, new AuthenticationProvider(apiProvider));
    }

    @VisibleForTesting
    ConditionletsResource(ApiProvider apiProvider, AuthenticationProvider authProxy) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.authProxy = authProxy;
    }

    @VisibleForTesting
    User getUser(@Context HttpServletRequest request) {
        return authProxy.authenticate(request);
    }

    /**
     * <p>Returns a JSON with all the Conditionlet Objects defined.
     * <br>Each Conditionlet node contains only its name
     * <p/>
     * Usage: /conditionlets/
     */
    @GET
    @Path("/system/conditionlets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@Context HttpServletRequest request) throws JSONException {
        User user = getUser(request);

        JSONObject jsonConditionlets = new JSONObject();

        try {

            List<Conditionlet> conditionlets = rulesAPI.findConditionlets();

            for (Conditionlet conditionlet : conditionlets) {
                JSONObject conditionletObject = new JSONObject();
                conditionletObject.put("name", conditionlet.getLocalizedName());

                Set<Comparison> comparisons = conditionlet.getComparisons();
                JSONObject jsonComparisons = new JSONObject();

                for (Comparison comparison : comparisons) {
                    JSONObject comparisonJSON = new JSONObject();
                    comparisonJSON.put("name", comparison.getLabel());
                    jsonComparisons.put(comparison.getId(), comparisonJSON);
                }

                conditionletObject.put("comparisons", jsonComparisons);

                jsonConditionlets.put(conditionlet.getClass().getSimpleName(), conditionletObject);
            }

            return Response.ok(jsonConditionlets.toString(), MediaType.APPLICATION_JSON).build();
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Conditionlets", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Returns a JSON with the Comparisons of a given contentlet.
     * <br>Each Comparisons node contains the id and label
     * <p/>
     * Usage: /comparisons/conditionlet/{id}
     */
    @GET
    @Path("/conditionlets/{id}/comparisons")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listComparisons(@Context HttpServletRequest request, @PathParam("id") String conditionletId) throws JSONException {
        User user = getUser(request);

        JSONObject jsonComparisons = new JSONObject();

        if(!UtilMethods.isSet(conditionletId)) {
            return Response.ok(jsonComparisons.toString(), MediaType.APPLICATION_JSON).build();
        }

        try {
            Conditionlet conditionlet = rulesAPI.findConditionlet(conditionletId);

            if(!UtilMethods.isSet(conditionlet)) {
                return Response.ok(jsonComparisons.toString(), MediaType.APPLICATION_JSON).build();
            }

            Set<Comparison> comparisons = conditionlet.getComparisons();

            for (Comparison comparison : comparisons) {
                JSONObject comparisonJSON = new JSONObject();
                comparisonJSON.put("name", comparison.getLabel());
                jsonComparisons.put(comparison.getId(), comparisonJSON);
            }

            return Response.ok(jsonComparisons.toString(), MediaType.APPLICATION_JSON).build();
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Conditionlet Comparisons", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Returns a JSON with the Comparisons of a given contentlet.
     * <br>Each Comparisons node contains the id and label
     * <p/>
     * Usage: /conditionletInputs/
     */
    @GET
    @Path("/conditionlets/{id}/comparisons/{comparison}/inputs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listConditionletInputs(@Context HttpServletRequest request,
                                          @PathParam("id") String conditionletId,
                                          @PathParam("comparison") String comparison) throws JSONException {
        User user = getUser(request);

        com.dotmarketing.util.json.JSONArray jsonInputs = new com.dotmarketing.util.json.JSONArray();

        if(!UtilMethods.isSet(conditionletId) || !UtilMethods.isSet(comparison)) {
            return Response.ok(jsonInputs.toString(), MediaType.APPLICATION_JSON).build();
        }

        try {
            Conditionlet conditionlet = rulesAPI.findConditionlet(conditionletId);

            if(!UtilMethods.isSet(conditionlet)) {
                return Response.ok(jsonInputs.toString(), MediaType.APPLICATION_JSON).build();
            }

            jsonInputs.addAll(conditionlet.getInputs(comparison));

            return Response.ok(jsonInputs.toString(), MediaType.APPLICATION_JSON).build();
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Conditionlet Inputs", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

}
