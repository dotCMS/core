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
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.conditionlet.Comparison;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;


@Path("/rules-engine")
public class RulesResource extends WebResource {

    private static final boolean UPDATE = false;
    private static final boolean SAVE = true;
    private RulesAPI rulesAPI = APILocator.getRulesAPI();

    /**
     * <p>Returns a JSON representation of the rules defined in the given Host or Folder
     * <br>Each Rule node contains all fields in  .
     * <p/>
     * Usage: /rules/{hostOrFolderIdentifier}
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/sites/{id}/rules")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRules(@Context HttpServletRequest request, @PathParam("id") String siteId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject rulesJSON = new JSONObject();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if (UtilMethods.isSet(host)) {
                List<Rule> rules = rulesAPI.getRulesByHost(host.getIdentifier(), user, false);

                for (Rule rule : rules) {
                    rulesJSON.put(rule.getId(), getRuleJSON(rule, user));
                }
            }

        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }

        return responseResource.response(rulesJSON.toString());
    }

    /**
     * <p>Returns a JSON representation of the Rule with the given ruleId
     * <p/>
     * Usage: GET api/rules-engine/rules/{ruleId}
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/rules/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRule(@Context HttpServletRequest request, @PathParam("id") String ruleId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {
            Rule rule = rulesAPI.getRuleById(ruleId, user, false);
            JSONObject ruleJSON = getRuleJSON(rule, user);

            if (rule != null) {
                return responseResource.response(ruleJSON.toString());
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

     * Usage: /conditions/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/rules/{id}/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConditions(@Context HttpServletRequest request, @PathParam("id") String ruleId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();

        try {

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (!UtilMethods.isSet(rule) || !UtilMethods.isSet(rule.getId())) {
                resultsObject.put("conditionGroups", new JSONArray());
                return responseResource.response(resultsObject.toString());
            }

            JSONArray jsonConditionGroups = new JSONArray();

            List<ConditionGroup> conditionGroups = rulesAPI.getConditionGroupsByRule(ruleId, user, false);

            for (ConditionGroup conditionGroup : conditionGroups) {
                JSONObject jsonConditionGroup = new JSONObject();
                jsonConditionGroup.put("conditionGroupId", conditionGroup.getId());
                jsonConditionGroup.put("operator", conditionGroup.getOperator());

                JSONArray jsonGroupConditions = new JSONArray();

                List<Condition> conditions = rulesAPI.getConditionsByConditionGroup(conditionGroup.getId(), user, false);

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
    @Path("/rules/conditions/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCondition(@Context HttpServletRequest request, @PathParam("id") String conditionId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {
            Condition condition = rulesAPI.getConditionById(conditionId, user, false);
            JSONObject conditionObject = new com.dotmarketing.util.json.JSONObject(condition);
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

        JSONObject jsonConditionlets = new JSONObject();

        try {

            List<Conditionlet> conditionlets = rulesAPI.findConditionlets();

            for (Conditionlet conditionlet : conditionlets) {
                JSONObject conditionletObject = new JSONObject();
                conditionletObject.put("name", conditionlet.getLocalizedName());
                jsonConditionlets.put(conditionlet.getClass().getSimpleName(), conditionletObject);
            }

            return responseResource.response(jsonConditionlets.toString());

        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Returns a JSON with the Comparisons of a given contentlet.
     * <br>Each Comparisons node contains the id and label
     * <p/>
     * Usage: /comparisons/conditionlet/{id}
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/conditionlets/{id}/comparisons")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getComparisons(@Context HttpServletRequest request, @PathParam("id") String conditionletId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();


        JSONObject jsonComparisons = new JSONObject();

        if (!UtilMethods.isSet(conditionletId)) {
            return responseResource.response(jsonComparisons.toString());
        }

        try {
            Conditionlet conditionlet = rulesAPI.findConditionlet(conditionletId);

            if (!UtilMethods.isSet(conditionlet)) {
                return responseResource.response(jsonComparisons.toString());
            }

            Set<Comparison> comparisons = conditionlet.getComparisons();

            for (Comparison comparison : comparisons) {
                JSONObject comparisonJSON = new JSONObject();
                comparisonJSON.put("name", comparison.getLabel());
                jsonComparisons.put(comparison.getId(), comparisonJSON);
            }

            return responseResource.response(jsonComparisons.toString());
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
    @Path("/conditionlets/{id}/comparisons/{comparison}/inputs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConditionletInputs(@Context HttpServletRequest request, @PathParam("id") String conditionletId, @PathParam("comparison") String comparison) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());

        JSONObject resultsObject = new JSONObject();
        com.dotmarketing.util.json.JSONArray jsonInputs = new com.dotmarketing.util.json.JSONArray();

        if (!UtilMethods.isSet(conditionletId) || !UtilMethods.isSet(comparison)) {
            resultsObject.put("conditionletinputs", (Object) jsonInputs);
            return responseResource.response(resultsObject.toString());
        }

        try {
            Conditionlet conditionlet = rulesAPI.findConditionlet(conditionletId);

            if (!UtilMethods.isSet(conditionlet)) {
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

        JSONObject jsonActionlets = new JSONObject();

        try {

            List<RuleActionlet> actionlets = rulesAPI.findActionlets();

            for (RuleActionlet actionlet : actionlets) {
                JSONObject actionletObject = new JSONObject();
                actionletObject.put("name", actionlet.getLocalizedName());
                jsonActionlets.put(actionlet.getClass().getSimpleName(), actionletObject);
            }

            return responseResource.response(jsonActionlets.toString());

        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Returns a JSON with the RuleActions defined for the Rule with the given ruleId.
     * <p/>
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/rule/{id}/actions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRuleActions(@Context HttpServletRequest request, @PathParam("id") String ruleId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();
        JSONArray jsonActions = new JSONArray();

        try {
            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (!UtilMethods.isSet(rule) || !UtilMethods.isSet(rule.getId())) {
                resultsObject.put("ruleactions", (Object) new JSONArray());
                return responseResource.response(resultsObject.toString());
            }

            List<RuleAction> actions = rulesAPI.getRuleActionsByRule(rule.getId(), user, false);

            for (RuleAction action : actions) {
                JSONObject actionletObject = new JSONObject();
                actionletObject.put("id", action.getId());
                actionletObject.put("name", action.getName());
                actionletObject.put("actionlet", action.getActionlet());
                jsonActions.put(actionletObject);
            }

            resultsObject.put("ruleactions", (Object) jsonActions);

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
    public Response saveRule(@Context HttpServletRequest request, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject ruleJSON) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();

        try {
            Rule rule = saveUpdateRule(user, ruleJSON, null, SAVE);
            resultsObject.put("id", rule.getId());
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
    @Path("/rules/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRule(@Context HttpServletRequest request, @PathParam("id") String ruleId, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject ruleJSON) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {
            saveUpdateRule(user, ruleJSON, ruleId, UPDATE);

            return responseResource.response(ruleJSON.toString());
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }

    }

    /**
     * <p>Saves a Condition Group
     * <br>
     * <p/>
     * Usage: /rules/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @POST
    @Path("/rules/{id}/conditiongroups")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveConditionGroup(@Context HttpServletRequest request, @PathParam("id") String ruleId, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject groupJSON) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();

        try {
            ConditionGroup group = new ConditionGroup();
            group.setRuleId(ruleId);
            group.setOperator(Condition.Operator.valueOf(groupJSON.optString("operator", Condition.Operator.AND.name())));
            group.setPriority(groupJSON.optInt("priority", 0));

            rulesAPI.saveConditionGroup(group, user, false);
            resultsObject.put("id", group.getId());
            return responseResource.response(resultsObject.toString());

        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Updates a Condition Group
     * <br>
     * <p/>
     * Usage: /rules/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @PUT
    @Path("/rules/conditiongroups/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateConditionGroup(@Context HttpServletRequest request, @PathParam("id") String groupId,
                                         com.dotcms.repackage.org.codehaus.jettison.json.JSONObject groupJSON) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {
            if(!UtilMethods.isSet(groupId)) {
                Logger.info(getClass(), "Unable to update Condition Group - 'id' not provided");
                throw new DotDataException("Unable to update Condition Grou - 'id' not provided");
            }

            ConditionGroup group = rulesAPI.getConditionGroupById(groupId, user, false);

            if (!UtilMethods.isSet(group)) {
                throw new DotDataException("Unable to update Condition Group with id:" + groupId);
            }

                group.setOperator(Condition.Operator.valueOf(groupJSON.optString("operator", Condition.Operator.AND.name())));
                group.setPriority(groupJSON.optInt("priority", 0));

            return responseResource.response(groupJSON.toString());


        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
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
    @Path("/rules/{ruleId}/conditiongroups/{groupId}/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveCondition(@Context HttpServletRequest request, @PathParam("ruleId") String ruleId, @PathParam("groupId") String groupId, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject conditionJSON) throws DotDataException, DotSecurityException, JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();

        try {
            Condition condition = new Condition();
            condition.setName(conditionJSON.getString("name"));
            condition.setRuleId(ruleId);
            condition.setConditionletId(conditionJSON.getString("conditionletId"));
            condition.setConditionGroup(groupId);
            condition.setComparison(conditionJSON.getString("comparison"));
            condition.setOperator(Condition.Operator.valueOf(conditionJSON.optString("operator", Condition.Operator.AND.name())));
            condition.setPriority(conditionJSON.optInt("priority", 0));

            // TODO: SET CONDITION VALUES
            com.dotcms.repackage.org.codehaus.jettison.json.JSONArray values = conditionJSON.getJSONArray("values");

            if(UtilMethods.isSet(values)) {
                for(int i=0; i<values.length(); i++) {
                    JSONObject value = (JSONObject) values.get(i);

                }
            }


            rulesAPI.saveCondition(condition, user, false);
            resultsObject.put("id", condition.getId());
            return responseResource.response(resultsObject.toString());

        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Saves a Rule Action
     * <br>
     * <p/>
     * Usage: /rules/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @POST
    @Path("/rules/{id}/actions/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveRuleAction(@Context HttpServletRequest request, @PathParam("id") String ruleId, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject actionJSON) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();

        try {
            RuleAction action = new RuleAction();
            action.setRuleId(ruleId);
            action.setName(actionJSON.getString("actionletName"));
            action.setPriority(actionJSON.optInt("priority", 0));
            action.setActionlet(actionJSON.getString("actionlet"));

            rulesAPI.saveRuleAction(action, user, false);

            resultsObject.put("id", action.getId());
            return responseResource.response(resultsObject.toString());

        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Updates the Rule Action with the given id
     * <br>
     * <p/>
     * Usage: /rules/
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @PUT
    @Path("/rules/actions/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRuleAction(@Context HttpServletRequest request,  @PathParam("id") String actionId, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject actionJSON) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {

            RuleAction action = rulesAPI.getRuleActionById(actionId, user, false);

            action.setRuleId(actionJSON.getString("ruleId"));
            action.setName(actionJSON.getString("actionletName"));
            action.setPriority(actionJSON.optInt("priority", 0));
            action.setActionlet(actionJSON.getString("actionlet"));

            rulesAPI.saveRuleAction(action, user, false);

            return responseResource.response(actionJSON.toString());

        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }


    /**
     * <p>Deletes a Rule
     * <br>
     * <p/>
     * Usage: DELETE api/rules-engine/rules/{ruleId}
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @DELETE
    @Path("/rules/{ruleId}")
    public Response deleteRule(@Context HttpServletRequest request, @PathParam("ruleId") String ruleId) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        User user = initData.getUser();

        try {
            if(!UtilMethods.isSet(ruleId)) {
                Logger.info(getClass(), "Unable to delete rule - 'ruleId' not provided");
                throw new DotDataException("Unable to delete rule - 'ruleId' not provided");
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            rulesAPI.deleteRule(rule, user, false);

            return Response.status(HttpStatus.SC_NO_CONTENT).build();
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Deletes a Condition Group and all its child Conditions
     * <br>
     * <p/>
     * Usage: DELETE api/rules-engine/conditiongroups
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @DELETE
    @Path("/rules/conditiongroups/{conditionGroupId}")
    public Response deleteConditionGroup(@Context HttpServletRequest request, @PathParam("conditionGroupId") String conditionGroupId) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        User user = initData.getUser();

        try {
            if(!UtilMethods.isSet(conditionGroupId)) {
                Logger.info(getClass(), "Unable to delete condition group - 'conditionGroupId' not provided");
                throw new DotDataException("Unable to delete condition group - 'conditionGroupId' not provided");
            }

            ConditionGroup conditionGroup = rulesAPI.getConditionGroupById(conditionGroupId, user, false);
            rulesAPI.deleteConditionGroup(conditionGroup, user, false);

            return Response.status(HttpStatus.SC_NO_CONTENT).build();
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Deletes a Condition
     * <br>
     * <p/>
     * Usage: DELETE api/rules-engine/rules
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @DELETE
    @Path("/conditions")
    public Response deleteCondition(@Context HttpServletRequest request, @PathParam("conditionId") String conditionId) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        User user = initData.getUser();

        try {
            if(!UtilMethods.isSet(conditionId)) {
                Logger.info(getClass(), "Unable to delete condition - 'conditionId' not provided");
                throw new DotDataException("Unable to delete condition - 'conditionId' not provided");
            }

            Condition condition = rulesAPI.getConditionById(conditionId, user, false);
            rulesAPI.deleteCondition(condition, user, false);

            return Response.status(HttpStatus.SC_NO_CONTENT).build();
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Deletes the RuleAction with the given ruleActionId
     * <br>
     * <p/>
     * Usage: DELETE api/rules-engine/rules/actions/{ruleActionId}
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @DELETE
    @Path("/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteRuleAction(@Context HttpServletRequest request, @PathParam("ruleActionId") String ruleActionId) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        User user = initData.getUser();

        try {
            if(!UtilMethods.isSet(ruleActionId)) {
                Logger.info(getClass(), "Unable to delete RuleAction - 'ruleActionId' not provided");
                throw new DotDataException("Unable to delete RuleAction - 'ruleActionId' not provided");
            }

            RuleAction ruleAction = rulesAPI.getRuleActionById(ruleActionId, user, false);
            rulesAPI.deleteRuleAction(ruleAction, user, false);

            return Response.status(HttpStatus.SC_NO_CONTENT).build();
        } catch (DotDataException | DotSecurityException e) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }


    private JSONObject getRuleJSON(Rule rule, User user) throws DotDataException, DotSecurityException, JSONException {
        JSONObject ruleJSON = new com.dotmarketing.util.json.JSONObject(rule);

        List<ConditionGroup> groups = rulesAPI.getConditionGroupsByRule(rule.getId(), user, false);
        JSONObject groupsJSON = new JSONObject();

        for (ConditionGroup group : groups) {
            groupsJSON.put(group.getId(), new JSONObject(group, new String[]{"operator", "priority"}));
        }

        ruleJSON.put("conditionGroups", groupsJSON);

        List<RuleAction> actions = rulesAPI.getRuleActionsByRule(rule.getId(), user, false);
        JSONObject actionsJSON = new JSONObject();

        for (RuleAction action : actions) {
            groupsJSON.put(action.getId(), new JSONObject(action, new String[]{"priority"}));
        }

        ruleJSON.put("actions", actionsJSON);
        return ruleJSON;
    }

    private Rule saveUpdateRule(User user, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject ruleJSON, String ruleId, boolean save) throws DotDataException, DotSecurityException,
            JSONException {


        Rule rule;

        if (save) {
            Host host = APILocator.getHostAPI().find(ruleJSON.getString("site"), user, false);

            if (!UtilMethods.isSet(host) || !UtilMethods.isSet(host.getIdentifier())) {
                Logger.error(this.getClass(), "Invalid Site identifier provided");
                throw new DotDataException("Invalid Site identifier provided ");
            }

            rule = new Rule();
            rule.setHost(host.getIdentifier());
            rule.setName(ruleJSON.getString("name"));

        } else {

            if(!UtilMethods.isSet(ruleId)) {
                Logger.info(getClass(), "Unable to update rule - 'ruleId' not provided");
                throw new DotDataException("Unable to update rule - 'ruleId' not provided");
            }

            rule = rulesAPI.getRuleById(ruleId, user, false);

            if (!UtilMethods.isSet(rule)) {
                throw new DotDataException("Unable to update Rule with id:" + ruleId);
            }

            try {
                rule.setName(ruleJSON.getString("ruleName"));
            } catch (com.dotcms.repackage.org.codehaus.jettison.json.JSONException e) {
                Logger.info(getClass(), "Unable to set 'ruleName' - Invalid value provided - Using default");
                throw new DotDataException("No 'ruleName' provided");
            }
        }

        rule.setFireOn(Rule.FireOn.valueOf(ruleJSON.optString("fireOn", Rule.FireOn.EVERY_PAGE.name())));

        rule.setPriority(ruleJSON.optInt("priority", 0));

        rule.setShortCircuit(ruleJSON.optBoolean("shortCircuit", false));

        rule.setEnabled(ruleJSON.optBoolean("enabled", true));

        rulesAPI.saveRule(rule, user, false);

        return rule;
    }


}
