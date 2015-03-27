package com.dotcms.rest;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.CacheControl;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.apache.commons.lang.StringEscapeUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.rules.conditionlet.Comparison;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Path("/rules-engine")
public class RulesResource extends WebResource {

	/**
	 * <p>Returns a JSON representation of the rules defined in the given Host or Folder
	 * <br>Each Rule node contains all fields in  .
	 *
	 * Usage: /rules/{hostOrFolderIdentifier}
	 * @throws com.dotmarketing.util.json.JSONException
	 *
	 */

	@GET
	@Path("/rules/{ruleId}")
	@Produces("application/json")
	public Response getRules(@Context HttpServletRequest request, @PathParam("ruleId") String ruleId) throws DotDataException, DotSecurityException, JSONException {
		InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        User user = initData.getUser();

        if(UtilMethods.isSet(ruleId)) {
            Rule rule = APILocator.getRulesAPI().getRuleById(ruleId, user, false);
            JSONObject ruleObject = new JSONObject(rule);
            return responseResource.response(ruleObject.toString());
        }

        JSONObject resultsObject = new JSONObject();
        JSONArray jsonRules = new JSONArray();
        String hostIdentifier = initData.getParamsMap().get("host");
        String folderIdentifier = initData.getParamsMap().get("folder");

        if(!UtilMethods.isSet(hostIdentifier) || !UtilMethods.isSet(folderIdentifier)) {
            resultsObject.put("rules", new JSONArray());
            return responseResource.response(resultsObject.toString());
        }

        if(UtilMethods.isSet(hostIdentifier)) {
            Host host = APILocator.getHostAPI().find(hostIdentifier, user, false);

            if (UtilMethods.isSet(host)) {
                List<Rule> rules = APILocator.getRulesAPI().getRulesByHost(host.getIdentifier(), user, false);
                jsonRules = new JSONArray(rules);
            }
        }

        else if(UtilMethods.isSet(folderIdentifier)) {
            Folder folder = APILocator.getFolderAPI().find(folderIdentifier, user, false);

            if (UtilMethods.isSet(folder)) {
                List<Rule> rules = APILocator.getRulesAPI().getRulesByFolder(folder.getIdentifier(), user, false);
                jsonRules = new JSONArray(rules);
            }
        }

        resultsObject.put("rules", jsonRules);
        return responseResource.response(resultsObject.toString());
    }

    /**
     * <p>Returns a JSON with the Condition Groups and its Conditions for the rule with the given ruleId.
     * <br>Each Rule node contains all fields in  .
     *
     * <p>If a conditionId is provided, it will return the condition whose id matches the provided conditionId.
     *
     * Usage: /conditions/
     * @throws com.dotmarketing.util.json.JSONException
     *
     */

    @GET
    @Path("/rules/{ruleId}/conditions/{conditionId}")
    @Produces("application/json")
    public Response getConditions(@Context HttpServletRequest request, @PathParam("ruleId") String ruleId, @PathParam("conditionId") String conditionId) throws DotDataException, DotSecurityException, JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        User user = initData.getUser();

        if(UtilMethods.isSet(conditionId)) {
            Condition condition = APILocator.getRulesAPI().getConditionById(conditionId, user, false);
            JSONObject conditionObject = new JSONObject(condition);
            return responseResource.response(conditionObject.toString());
        }

        JSONObject resultsObject = new JSONObject();

        if(!UtilMethods.isSet(ruleId)) {
            resultsObject.put("conditionGroups", new JSONArray());
            return responseResource.response(resultsObject.toString());
        }

        Rule rule = APILocator.getRulesAPI().getRuleById(ruleId, user, false);

        if(!UtilMethods.isSet(rule) || !UtilMethods.isSet(rule.getId())) {
            resultsObject.put("conditionGroups", new JSONArray());
            return responseResource.response(resultsObject.toString());
        }


        JSONArray jsonConditionGroups = new JSONArray();

        List<ConditionGroup> conditionGroups = APILocator.getRulesAPI().getConditionGroupsByRule(ruleId, user, false);

        for (ConditionGroup conditionGroup : conditionGroups) {
            JSONObject jsonConditionGroup = new JSONObject();
            jsonConditionGroup.put("conditionGroupId", conditionGroup.getId());
            jsonConditionGroup.put("operator", conditionGroup.getOperator());

            JSONArray jsonGroupConditions = new JSONArray();

            List<Condition> conditions = APILocator.getRulesAPI().getConditionsByConditionGroup(conditionGroup.getId(), user, false);

            for (Condition condition : conditions) {
                JSONObject conditionObject = new JSONObject();
                conditionObject.put("conditionId", condition.getId());
                conditionObject.put("conditionName", condition.getName());
                conditionObject.put("operator", condition.getOperator());
                jsonGroupConditions.put(conditionObject);
            }

            jsonConditionGroup.put("conditions", jsonGroupConditions);

        }

        resultsObject.put("conditionGroups", jsonConditionGroups);

        return responseResource.response(resultsObject.toString());
    }

    /**
     * <p>Returns a JSON with all the Conditionlet Objects defined.
     * <br>Each Conditionlet node contains only its name
     *
     * Usage: /conditionlets/
     * @throws com.dotmarketing.util.json.JSONException
     *
     */

    @GET
    @Path("/conditionlets")
    @Produces("application/json")
    public Response getConditionlets(@Context HttpServletRequest request) throws DotDataException, DotSecurityException, JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

        JSONObject resultsObject = new JSONObject();
        JSONArray jsonConditionlets = new JSONArray();

        List<Conditionlet> conditionlets = APILocator.getRulesAPI().getConditionlets();

        for (Conditionlet conditionlet : conditionlets) {
            JSONObject conditionletObject = new JSONObject();
            conditionletObject.put("id", conditionlet.getClass().getCanonicalName());
            conditionletObject.put("name", conditionlet.getLocalizedName());
            jsonConditionlets.add(conditionletObject);
        }

        resultsObject.put("conditionlets", jsonConditionlets);

        return responseResource.response(resultsObject.toString());
    }

    /**
     * <p>Returns a JSON with the Comparisons of a given contentlet.
     * <br>Each Comparisons node contains the id and label
     *
     * Usage: /comparisons/conditionlet/{conditionletId}
     * @throws com.dotmarketing.util.json.JSONException
     *
     */

    @GET
    @Path("/conditionlets/{id}/comparisons")
    @Produces("application/json")
    public Response getComparisons(@Context HttpServletRequest request, @PathParam("id") String conditionletId) throws DotDataException, DotSecurityException, JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();
        JSONArray jsonComparisons = new JSONArray();

        if(!UtilMethods.isSet(conditionletId)) {
            resultsObject.put("comparisons", jsonComparisons);
            return responseResource.response(resultsObject.toString());
        }

        Conditionlet conditionlet = APILocator.getRulesAPI().findConditionlet(conditionletId);
        jsonComparisons.addAll(conditionlet.getComparisons());
        resultsObject.put("comparisons", jsonComparisons);

        return responseResource.response(resultsObject.toString());
    }

    /**
     * <p>Returns a JSON with the Comparisons of a given contentlet.
     * <br>Each Comparisons node contains the id and label
     *
     * Usage: /conditionletInputs/
     * @throws com.dotmarketing.util.json.JSONException
     *
     */

    @GET
    @Path("/conditionlets/{id}/inputs")
    @Produces("application/json")
    public Response getConditionletInputs(@Context HttpServletRequest request, @PathParam("id") String conditionletId) throws DotDataException, DotSecurityException, JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();
        JSONArray jsonInputs = new JSONArray();

        String comparison = initData.getParamsMap().get("comparison");

        if(!UtilMethods.isSet(conditionletId) || !UtilMethods.isSet(comparison)) {
            resultsObject.put("conditionletinputs", jsonInputs);
            return responseResource.response(resultsObject.toString());
        }



        Conditionlet conditionlet = APILocator.getRulesAPI().findConditionlet(conditionletId);
        jsonInputs.addAll(conditionlet.getInputs(comparison));
        resultsObject.put("conditionletinputs", jsonInputs);

        return responseResource.response(resultsObject.toString());
    }


    /**
     * <p>Saves a new Rule
     * <br>
     *
     * Usage: /rules/
     * @throws com.dotmarketing.util.json.JSONException
     *
     */

    @POST
    @Path("/rules")
    @Produces("application/json")
    public Response saveRule(@Context HttpServletRequest request, @PathParam("params") String params) throws DotDataException, DotSecurityException, JSONException {
        InitDataObject initData = init(params, true, request, true);
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        User user = initData.getUser();
        Map ruleAttributes;

        try {
            ruleAttributes = processJSON(request.getInputStream());
        } catch (com.dotcms.repackage.org.codehaus.jettison.json.JSONException e) {
            Logger.error(this.getClass(), "Error processing JSON for Stream", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        } catch (IOException e) {
            Logger.error(this.getClass(), "Error processing Stream", e);
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        JSONObject resultsObject = new JSONObject();

        Rule rule = new Rule();

        if(!UtilMethods.isSet(ruleAttributes.get("ruleName")) || !UtilMethods.isSet(ruleAttributes.get("site"))) {
            Logger.error(this.getClass(), "Saving Rule - No ruleName or Site provided");
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity("No ruleName or Site provided").build();
        }

        try {
            setRulesProperties(user, ruleAttributes, rule);
        } catch (DotDataException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity("").build();
        }

        APILocator.getRulesAPI().saveRule(rule, user, false);


        resultsObject.put(rule.getId(), new JSONObject(rule));

        return responseResource.response(resultsObject.toString());
    }

    /**
     * <p>Updates a new Rule
     * <br>
     *
     * Usage: /rules/
     * @throws com.dotmarketing.util.json.JSONException
     *
     */

    @PUT
    @Path("/rules")
    @Produces("application/json")
    public Response updateRule(@Context HttpServletRequest request, @PathParam("params") String params) throws DotDataException, DotSecurityException, JSONException {
        InitDataObject initData = init(params, true, request, true);
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        User user = initData.getUser();
        Map ruleAttributes;

        try {
            ruleAttributes = processJSON(request.getInputStream());
        } catch (com.dotcms.repackage.org.codehaus.jettison.json.JSONException e) {
            Logger.error(this.getClass(), "Error processing JSON for Stream", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        } catch (IOException e) {
            Logger.error(this.getClass(), "Error processing Stream", e);
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        JSONObject resultsObject = new JSONObject();

        Rule newRuleVersion = new Rule();

        if(!UtilMethods.isSet(ruleAttributes.get("ruleId")) || !UtilMethods.isSet(ruleAttributes.get("ruleName")) || !UtilMethods.isSet(ruleAttributes.get("site"))) {
            Logger.error(this.getClass(), "Updating Rule - No ruleId or ruleName or Site provided");
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity("No ruleName or Site provided").build();
        }

        Rule existingRule = APILocator.getRulesAPI().getRuleById((String)ruleAttributes.get("ruleId"), user, false);

        if (!UtilMethods.isSet(existingRule)) {
            Logger.error(this.getClass(), "Updating Rule - Invalid ruleId provided");
            return responseResource.response(resultsObject.toString());
        }

        newRuleVersion.setId(existingRule.getId());

        try {
            setRulesProperties(user, ruleAttributes, newRuleVersion);
        } catch (DotDataException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity("").build();
        }

        APILocator.getRulesAPI().saveRule(newRuleVersion, user, false);


        resultsObject.put(newRuleVersion.getId(), new JSONObject(newRuleVersion));

        return responseResource.response(resultsObject.toString());
    }

    /**
     * <p>Saves a new Condition
     * <br>
     *
     * Usage: /rules/
     * @throws com.dotmarketing.util.json.JSONException
     *
     */

    @POST
    @Path("/rules/{ruleId}/conditions")
    @Produces("application/json")
    public Response saveCondition(@Context HttpServletRequest request, @PathParam("params") String params) throws DotDataException, DotSecurityException, JSONException {
        InitDataObject initData = init(params, true, request, true);
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        User user = initData.getUser();
        Map conditionAttributes;

        try {
            conditionAttributes = processJSON(request.getInputStream());
        } catch (com.dotcms.repackage.org.codehaus.jettison.json.JSONException e) {
            Logger.error(this.getClass(), "Error processing JSON for Stream", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        } catch (IOException e) {
            Logger.error(this.getClass(), "Error processing Stream", e);
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        JSONObject resultsObject = new JSONObject();

        ConditionGroup conditionGroup;

//        if(!UtilMethods.isSet(ruleAttributes.get("ruleName")) || !UtilMethods.isSet(ruleAttributes.get("site"))) {
//            Logger.error(this.getClass(), "Saving Rule - No ruleName or Site provided");
//            return Response.status(HttpStatus.SC_BAD_REQUEST).entity("No ruleName or Site provided").build();
//        }
//
//        try {
//            setRulesProperties(user, ruleAttributes, rule);
//        } catch (DotDataException e) {
//            return Response.status(HttpStatus.SC_BAD_REQUEST).entity("").build();
//        }
//
//        APILocator.getRulesAPI().saveRule(rule, user, false);
//
//
//        resultsObject.put(rule.getId(), new JSONObject(rule));

        return responseResource.response(resultsObject.toString());
    }

    private void setRulesProperties(User user, Map ruleAttributes, Rule newRuleVersion) throws DotDataException, DotSecurityException {
        Host host = APILocator.getHostAPI().find((String) ruleAttributes.get("site"), user, false);

        if (!UtilMethods.isSet(host) || !UtilMethods.isSet(host.getIdentifier())) {
            Logger.error(this.getClass(), "Invalid Site identifier provided");
            throw new DotDataException("Invalid Site identifier provided ");
        }

        newRuleVersion.setHost(host.getIdentifier());

        newRuleVersion.setName((String) ruleAttributes.get("ruleName"));

        String firePolicyStr = (String) ruleAttributes.get("firePolicy");

        if(UtilMethods.isSet(firePolicyStr)) {
            try {
                newRuleVersion.setFirePolicy(Rule.FirePolicy.valueOf(firePolicyStr));
            } catch(IllegalArgumentException e) {
                Logger.info(getClass(), "Unable to set Fire Policy - Invalid value provided");
            }
        }

        if(UtilMethods.isSet(ruleAttributes.get("shortCircuit"))) {
            try {
                newRuleVersion.setShortCircuit((boolean) ruleAttributes.get("shortCircuit") );
            } catch (ClassCastException e) {
                Logger.info(getClass(), "Unable to set Short Circuit - Invalid value provided");
            }
        }

        if(UtilMethods.isSet(ruleAttributes.get("enabled"))) {
            try {
                newRuleVersion.setEnabled((boolean) ruleAttributes.get("enabled"));
            } catch (ClassCastException e) {
                Logger.info(getClass(), "Unable to set Enabled - Invalid value provided");
            }
        }
    }

}
