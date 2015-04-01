package com.dotcms.rest;

import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@Path("/rules-engine")
public class RulesResource extends WebResource {

    private static final boolean UPDATE = false;
    private static final boolean SAVE = true;

    /**
     * <p>Returns a JSON representation of the rules defined in the given Host or Folder
     * <br>Each Rule node contains all fields in  .
     * <p/>
     * Usage: /rules/{hostOrFolderIdentifier}
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/sites/{siteId}/rules")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRules(@Context HttpServletRequest request, @PathParam("siteId") String siteId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();
        JSONArray jsonRules = new JSONArray();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if (UtilMethods.isSet(host)) {
                List<Rule> rules = APILocator.getRulesAPI().getRulesByHost(host.getIdentifier(), user, false);
                jsonRules = new JSONArray(rules);
            }

        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }

        resultsObject.put("rules", (Object) jsonRules);
        return responseResource.response(resultsObject.toString());
    }

    /**
     * <p>Returns a JSON representation of the rules defined in the given Host or Folder
     * <br>Each Rule node contains all fields in  .
     * <p/>
     * Usage: /rules/{hostOrFolderIdentifier}
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/rules/{ruleId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRule(@Context HttpServletRequest request, @PathParam("ruleId") String ruleId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {
            Rule rule = APILocator.getRulesAPI().getRuleById(ruleId, user, false);
            String[] ruleFields = {"name", "enabled", "site", "priority", "fireOn", "folder", "shortCircuit"};
            JSONObject jsonRule = new JSONObject(rule, ruleFields);

            List<ConditionGroup> groups = APILocator.getRulesAPI().getConditionGroupsByRule(rule.getId(), user, false);
            JSONObject jsonGroups = new JSONObject();

            for (ConditionGroup group : groups) {
//                jsonGroups.put(group.getId(), new JSONObject(group, ))
            }

            jsonRule.put("conditionGroups", jsonGroups);

            if (rule != null) {
                return responseResource.response(new JSONObject(rule).toString());
            } else {
                return responseResource.response(new JSONObject().toString());
            }
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }

    }

    /**
     * <p>Returns a JSON with the Condition Groups and its Conditions for the rule with the given ruleId.
     * <br>Each Rule node contains all fields in  .
     * <p/>
     * <p>If a conditionId is provided, it will return the condition whose id matches the provided conditionId.
     * <p/>
     * Usage: /conditions/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/rules/{ruleId}/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConditions(@Context HttpServletRequest request, @PathParam("ruleId") String ruleId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();

        try {

            Rule rule = APILocator.getRulesAPI().getRuleById(ruleId, user, false);

            if (!UtilMethods.isSet(rule) || !UtilMethods.isSet(rule.getId())) {
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

        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Returns a JSON with the Condition Groups and its Conditions for the rule with the given ruleId.
     * <br>Each Rule node contains all fields in  .
     * <p/>
     * <p>If a conditionId is provided, it will return the condition whose id matches the provided conditionId.
     * <p/>
     * Usage: /conditions/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/rules/conditions/{conditionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCondition(@Context HttpServletRequest request, @PathParam("conditionId") String conditionId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {
            Condition condition = APILocator.getRulesAPI().getConditionById(conditionId, user, false);
            JSONObject conditionObject = new JSONObject(condition);
            return responseResource.response(conditionObject.toString());
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }

    }

    /**
     * <p>Returns a JSON with all the Conditionlet Objects defined.
     * <br>Each Conditionlet node contains only its name
     * <p/>
     * Usage: /conditionlets/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/conditionlets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConditionlets(@Context HttpServletRequest request) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());

        JSONObject resultsObject = new JSONObject();
        JSONArray jsonConditionlets = new JSONArray();

        try {

            List<Conditionlet> conditionlets = APILocator.getRulesAPI().findConditionlets();

            for (Conditionlet conditionlet : conditionlets) {
                JSONObject conditionletObject = new JSONObject();
                conditionletObject.put("id", conditionlet.getClass().getSimpleName());
                conditionletObject.put("name", conditionlet.getLocalizedName());
                jsonConditionlets.add(conditionletObject);
            }

            resultsObject.put("conditionlets", (Object)jsonConditionlets);

            return responseResource.response(resultsObject.toString());

        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Returns a JSON with the Comparisons of a given contentlet.
     * <br>Each Comparisons node contains the id and label
     * <p/>
     * Usage: /comparisons/conditionlet/{conditionletId}
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/conditionlets/{conditionletId}/comparisons")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getComparisons(@Context HttpServletRequest request, @PathParam("conditionletId") String conditionletId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();


        JSONObject resultsObject = new JSONObject();
        JSONArray jsonComparisons = new JSONArray();

        if (!UtilMethods.isSet(conditionletId)) {
            resultsObject.put("comparisons", (Object) jsonComparisons);
            return responseResource.response(resultsObject.toString());
        }

        try {
            Conditionlet conditionlet = APILocator.getRulesAPI().findConditionlet(conditionletId);

            if(!UtilMethods.isSet(conditionlet)) {
                resultsObject.put("comparisons", (Object) jsonComparisons);
                return responseResource.response(resultsObject.toString());
            }

            jsonComparisons.addAll(conditionlet.getComparisons());
            resultsObject.put("comparisons", (Object) jsonComparisons);

            return responseResource.response(resultsObject.toString());
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Returns a JSON with the Comparisons of a given contentlet.
     * <br>Each Comparisons node contains the id and label
     * <p/>
     * Usage: /conditionletInputs/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/conditionlets/{conditionletId}/comparisons/{comparison}/inputs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConditionletInputs(@Context HttpServletRequest request, @PathParam("conditionletId") String conditionletId, @PathParam("comparison") String comparison) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());

        JSONObject resultsObject = new JSONObject();
        JSONArray jsonInputs = new JSONArray();

        if (!UtilMethods.isSet(conditionletId) || !UtilMethods.isSet(comparison)) {
            resultsObject.put("conditionletinputs", (Object) jsonInputs);
            return responseResource.response(resultsObject.toString());
        }

        try {
            Conditionlet conditionlet = APILocator.getRulesAPI().findConditionlet(conditionletId);

            if(!UtilMethods.isSet(conditionlet)) {
                resultsObject.put("conditionletinputs", (Object) jsonInputs);
                return responseResource.response(resultsObject.toString());
            }

            jsonInputs.addAll(conditionlet.getInputs(comparison));
            resultsObject.put("conditionletinputs", (Object) jsonInputs);

            return responseResource.response(resultsObject.toString());
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Returns a JSON with all the RuleActionlet Objects defined.
     * <p/>
     * Usage: /ruleactionlets/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/ruleactionlets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRuleActionlets(@Context HttpServletRequest request) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());

        JSONObject resultsObject = new JSONObject();
        JSONArray jsonActionlets = new JSONArray();

        try {

            List<RuleActionlet> actionlets = APILocator.getRulesAPI().findActionlets();

            for (RuleActionlet actionlet : actionlets) {
                JSONObject actionletObject = new JSONObject();
                actionletObject.put("id", actionlet.getClass().getSimpleName());
                actionletObject.put("name", actionlet.getLocalizedName());
                jsonActionlets.add(actionletObject);
            }

            resultsObject.put("ruleactionlets", (Object)jsonActionlets);

            return responseResource.response(resultsObject.toString());

        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Returns a JSON with all the RuleActionlet Objects defined.
     * <p/>
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/rule/{ruleId}/actions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRuleActions(@Context HttpServletRequest request, @PathParam("ruleId") String ruleId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();
        JSONArray jsonActions = new JSONArray();

        try {
            Rule rule = APILocator.getRulesAPI().getRuleById(ruleId, user, false);

            if (!UtilMethods.isSet(rule) || !UtilMethods.isSet(rule.getId())) {
                resultsObject.put("conditionGroups", new JSONArray());
                return responseResource.response(resultsObject.toString());
            }

            List<RuleAction> actions = APILocator.getRulesAPI().getActionsByRule(rule.getId(), user, false);

            for (RuleAction action : actions) {
                JSONObject actionletObject = new JSONObject();
                actionletObject.put("id", action.getId());
                actionletObject.put("name", action.getName());
                actionletObject.put("actionlet", action.getActionlet());
                jsonActions.add(actionletObject);
            }

            resultsObject.put("ruleactions", (Object)jsonActions);

            return responseResource.response(resultsObject.toString());

        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }



    /**
     * <p>Saves a new Rule
     * <br>
     * <p/>
     * Usage: /rules/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @POST
    @Path("/rules")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveRule(@Context HttpServletRequest request, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject ruleAttributes) throws
            com.dotcms.repackage.org.codehaus.jettison.json.JSONException, JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();

        try {
            Rule rule = saveUpdateRule(user, ruleAttributes, SAVE);
            resultsObject.put(rule.getId(), new JSONObject(rule));
            return responseResource.response(resultsObject.toString());
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Updates a new Rule
     * <br>
     * <p/>
     * Usage: /rules/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @PUT
    @Path("/rules")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRule(@Context HttpServletRequest request, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject ruleAttributes) throws
            com.dotcms.repackage.org.codehaus.jettison.json.JSONException, JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();

        try {
            Rule rule = saveUpdateRule(user, ruleAttributes, UPDATE);
            resultsObject.put(rule.getId(), new JSONObject(rule));
            return responseResource.response(resultsObject.toString());
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }

    }

    private Rule saveUpdateRule(User user, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject ruleAttributes, boolean save) throws DotDataException, DotSecurityException,
            com.dotcms.repackage.org.codehaus.jettison.json.JSONException {


        Rule rule;

        if (save) {
            Host host = APILocator.getHostAPI().find(ruleAttributes.getString("site"), user, false);

            if (!UtilMethods.isSet(host) || !UtilMethods.isSet(host.getIdentifier())) {
                Logger.error(this.getClass(), "Invalid Site identifier provided");
                throw new DotDataException("Invalid Site identifier provided ");
            }

            rule = new Rule();
            rule.setHost(host.getIdentifier());
            rule.setName(ruleAttributes.getString("ruleName"));

        } else {
            rule = APILocator.getRulesAPI().getRuleById(ruleAttributes.getString("ruleId"), user, false);

            if (!UtilMethods.isSet(rule)) {
                throw new DotDataException("Unable to update Rule with id:" + ruleAttributes.getString("ruleId"));
            }

            try {
                rule.setName(ruleAttributes.getString("ruleName"));
            } catch (com.dotcms.repackage.org.codehaus.jettison.json.JSONException e) {
                Logger.info(getClass(), "Unable to set 'ruleName' - Invalid value provided - Using default");
                throw new DotDataException("No 'ruleName' provided");
            }
        }

        rule.setFireOn(Rule.FireOn.valueOf(ruleAttributes.optString("firePolicy", Rule.FireOn.EVERY_PAGE.name())));

        rule.setShortCircuit(ruleAttributes.optBoolean("shortCircuit", false));

        rule.setEnabled(ruleAttributes.optBoolean("enabled", true));

        APILocator.getRulesAPI().saveRule(rule, user, false);

        return rule;
    }

    /**
     * <p>Saves a new Condition
     * <br>
     * <p/>
     * Usage: /rules/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @POST
    @Path("/rules/{ruleId}/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveCondition(@Context HttpServletRequest request, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject conditionAttributes) throws DotDataException, DotSecurityException, JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

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


}
