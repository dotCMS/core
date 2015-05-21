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
import com.dotmarketing.portlets.rules.model.*;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
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
            } else {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Rules", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }

        return responseResource.response(rulesJSON.toString());
    }

    /**
     * <p>Returns a JSON representation of the Rule with the given ruleId
     * <p/>
     * Usage: GET api/rules-engine/sites/{siteId}/rules/{ruleId}
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @GET
    @Path("/sites/{siteId}/rules/{ruleId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRule(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);
            JSONObject ruleJSON = getRuleJSON(rule, user);

            if (rule != null) {
                return responseResource.response(ruleJSON.toString());
            } else {
                return responseResource.response(new JSONObject().toString());
            }
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Rule", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }

    }

    @GET
    @Path("site/{siteId}/rules/{ruleId}/conditiongroups")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConditionGroups(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            JSONObject groupsJSON = new JSONObject();

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (!UtilMethods.isSet(rule) || !UtilMethods.isSet(rule.getId())) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            List<ConditionGroup> conditionGroups = rulesAPI.getConditionGroupsByRule(ruleId, user, false);

            for (ConditionGroup conditionGroup : conditionGroups) {

                JSONObject groupJSON = new com.dotmarketing.util.json.JSONObject(conditionGroup, new String[]{"operator", "priority"});

                List<Condition> conditions = rulesAPI.getConditionsByConditionGroup(conditionGroup.getId(), user, false);

                JSONObject conditionsJSON = new JSONObject();

                for (Condition condition : conditions) {
                    JSONObject conditionJSON = new com.dotmarketing.util.json.JSONObject(condition);

                    JSONObject valuesJSON = new JSONObject();
                    for(ConditionValue value:condition.getValues()) {
                        valuesJSON.put(value.getId(), new com.dotmarketing.util.json.JSONObject(value, new String[]{"value", "priority"}));
                    }

                    conditionJSON.put("values", valuesJSON);
                    conditionsJSON.put(condition.getId(), conditionJSON);
                }

                groupJSON.put("conditions", conditionsJSON);

                groupsJSON.put(conditionGroup.getId(), groupJSON);
            }

            return responseResource.response(groupsJSON.toString());

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Conditions", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConditionGroups(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId,
                                       @PathParam("groupId") String groupId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if (host == null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (!UtilMethods.isSet(rule) || !UtilMethods.isSet(rule.getId())) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            ConditionGroup group = rulesAPI.getConditionGroupById(groupId, user, false);

            if (group == null) {
                Logger.info(this, "Condition Group not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Condition Group not found").build();
            }


            JSONObject groupJSON = new com.dotmarketing.util.json.JSONObject(group, new String[]{"operator", "priority"});

            List<Condition> conditions = rulesAPI.getConditionsByConditionGroup(group.getId(), user, false);

            JSONObject conditionsJSON = new JSONObject();

            for (Condition condition : conditions) {
                JSONObject conditionJSON = new com.dotmarketing.util.json.JSONObject(condition);

                JSONObject valuesJSON = new JSONObject();
                for (ConditionValue value : condition.getValues()) {
                    valuesJSON.put(value.getId(), new com.dotmarketing.util.json.JSONObject(value, new String[]{"value", "priority"}));
                }

                conditionJSON.put("values", valuesJSON);
                conditionsJSON.put(condition.getId(), conditionJSON);
            }

            groupJSON.put("conditions", conditionsJSON);


            return responseResource.response(groupJSON.toString());

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Conditions", e);
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
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConditions(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId,
                                  @PathParam("groupId") String groupId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (!UtilMethods.isSet(rule) || !UtilMethods.isSet(rule.getId())) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            ConditionGroup group = rulesAPI.getConditionGroupById(groupId, user, false);

            if (group == null) {
                Logger.info(this, "Condition Group not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Condition Group not found").build();
            }


            List<Condition> conditions = rulesAPI.getConditionsByConditionGroup(group.getId(), user, false);

            JSONObject conditionsJSON = new JSONObject();

            for (Condition condition : conditions) {
                JSONObject conditionJSON = new com.dotmarketing.util.json.JSONObject(condition);

                JSONObject valuesJSON = new JSONObject();
                for (ConditionValue value : condition.getValues()) {
                    valuesJSON.put(value.getId(), new com.dotmarketing.util.json.JSONObject(value, new String[]{"value", "priority"}));
                }

                conditionJSON.put("values", valuesJSON);
                conditionsJSON.put(condition.getId(), conditionJSON);
            }


            return responseResource.response(conditionsJSON.toString());

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Conditions", e);
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
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}/conditions/{conditionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCondition(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId,
                                 @PathParam("groupId") String groupId, @PathParam("conditionId") String conditionId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            ConditionGroup group = rulesAPI.getConditionGroupById(groupId, user, false);

            if (group == null) {
                Logger.info(this, "Condition Group not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Condition Group not found").build();
            }

            Condition condition = rulesAPI.getConditionById(conditionId, user, false);
            JSONObject conditionObject = new com.dotmarketing.util.json.JSONObject(condition);
            return responseResource.response(conditionObject.toString());
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Condition", e);
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

            return responseResource.response(jsonConditionlets.toString());

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
            Logger.error(this, "Error getting Conditionlet Comparisons", e);
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

        com.dotmarketing.util.json.JSONArray jsonInputs = new com.dotmarketing.util.json.JSONArray();

        if (!UtilMethods.isSet(conditionletId) || !UtilMethods.isSet(comparison)) {
            return responseResource.response(jsonInputs.toString());
        }

        try {
            Conditionlet conditionlet = rulesAPI.findConditionlet(conditionletId);

            if (!UtilMethods.isSet(conditionlet)) {
                return responseResource.response(jsonInputs.toString());
            }

            jsonInputs.addAll(conditionlet.getInputs(comparison));

            return responseResource.response(jsonInputs.toString());
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Conditionlet Inputs", e);
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
            Logger.error(this, "Error getting Rule Actionlets", e);
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
    @Path("sites/{siteId}/rules/{ruleId}/actions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRuleActions(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject jsonActions = new JSONObject();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            List<RuleAction> actions = rulesAPI.getRuleActionsByRule(rule.getId(), user, false);

            for (RuleAction action : actions) {
                JSONObject actionletObject = new JSONObject();
                actionletObject.put("name", action.getName());
                actionletObject.put("actionlet", action.getActionlet());
                jsonActions.put(action.getId(), actionletObject);
            }


            return responseResource.response(jsonActions.toString());

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error getting Rule Action", e);
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
    @Path("/sites/{id}/rules")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveRule(@Context HttpServletRequest request, @PathParam("id") String siteId, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject ruleJSON) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();

        try {
            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = saveUpdateRule(user, ruleJSON, siteId, null, SAVE);
            resultsObject.put("id", rule.getId());
            return responseResource.response(resultsObject.toString());
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error saving Rule", e);
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
    @Path("/sites/{siteId}/rules/{ruleId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRule(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject ruleJSON) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {
            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            saveUpdateRule(user, ruleJSON, siteId, rule, UPDATE);

            return responseResource.response(ruleJSON.toString());
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error updating Rule", e);
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
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveConditionGroup(@Context HttpServletRequest request,  @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject groupJSON) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            ConditionGroup group = new ConditionGroup();
            group.setRuleId(ruleId);
            group.setOperator(Condition.Operator.valueOf(groupJSON.optString("operator", Condition.Operator.AND.name())));
            group.setPriority(groupJSON.optInt("priority", 0));

            rulesAPI.saveConditionGroup(group, user, false);
            resultsObject.put("id", group.getId());
            return responseResource.response(resultsObject.toString());

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error saving Condition Group", e);
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
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateConditionGroup(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId, @PathParam("groupId") String groupId,
                                         com.dotcms.repackage.org.codehaus.jettison.json.JSONObject groupJSON) throws JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            if(!UtilMethods.isSet(groupId)) {
                Logger.info(getClass(), "Unable to update Condition Group - 'id' not provided");
                throw new DotDataException("Unable to update Condition Group - 'id' not provided");
            }

            ConditionGroup group = rulesAPI.getConditionGroupById(groupId, user, false);

            if (group == null) {
                Logger.info(this, "Condition Group not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Condition Group not found").build();
            }

            group.setOperator(Condition.Operator.valueOf(groupJSON.optString("operator", Condition.Operator.AND.name())));
            group.setPriority(groupJSON.optInt("priority", 0));

            return responseResource.response(groupJSON.toString());


        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error updating Condition Group", e);
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
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveCondition(@Context HttpServletRequest request,  @PathParam("siteId") String siteId,  @PathParam("ruleId") String ruleId,
                                  @PathParam("groupId") String groupId, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject conditionJSON) throws DotDataException, DotSecurityException, JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            if(!UtilMethods.isSet(groupId)) {
                Logger.info(getClass(), "Unable to save Condition - 'groupId' not provided");
                throw new DotDataException("Unable to save Condition - 'groupId' not provided");
            }

            ConditionGroup group = rulesAPI.getConditionGroupById(groupId, user, false);

            if(group==null) {
                Logger.info(this, "Condition Group not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Condition Group not found").build();
            }

            Condition condition = new Condition();
            condition.setName(conditionJSON.getString("name"));
            condition.setRuleId(group.getRuleId());
            condition.setConditionletId(conditionJSON.getString("conditionlet"));
            condition.setConditionGroup(groupId);
            condition.setComparison(conditionJSON.getString("comparison"));
            condition.setOperator(Condition.Operator.valueOf(conditionJSON.optString("operator", Condition.Operator.AND.name())));
            condition.setPriority(conditionJSON.optInt("priority", 0));

            com.dotcms.repackage.org.codehaus.jettison.json.JSONArray valuesJSON = conditionJSON.optJSONArray("values");
            List<ConditionValue> values = new ArrayList<>();

            if(UtilMethods.isSet(valuesJSON)) {
                for(int i=0; i<valuesJSON.length(); i++) {
                    JSONObject valueJSON = valuesJSON.getJSONObject(i);
                    ConditionValue value = new ConditionValue();
                    value.setValue(valueJSON.getString("value"));
                    value.setPriority(valueJSON.optInt("priority", 0));
                    values.add(value);
                }
            }

            condition.setValues(values);

            rulesAPI.saveCondition(condition, user, false);

            resultsObject.put("id", condition.getId());

            JSONObject valuesObject = new JSONObject();

            for (ConditionValue value : values) {
                valuesObject.put(value.getId(), value.getValue());
            }

            resultsObject.put("values", valuesObject );
            return responseResource.response(resultsObject.toString());

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error saving Condition", e);
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * <p>Updates a Condition
     * <br>
     * <p/>
     * Usage: PUT /rules/conditiongroups/{groupId}/conditions
     *
     * @throws com.dotmarketing.util.json.JSONException
     */

    @PUT
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}/conditions/{conditionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateCondition(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId,
                                    @PathParam("groupId") String groupId, @PathParam("conditionId") String conditionId, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject conditionJSON) throws DotDataException, DotSecurityException, JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            ConditionGroup group = rulesAPI.getConditionGroupById(groupId, user, false);

            if(group==null) {
                Logger.info(this, "Condition Group not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Condition Group not found").build();
            }

            if(!UtilMethods.isSet(conditionId)) {
                Logger.info(getClass(), "Unable to update Condition - 'id' not provided");
                throw new DotDataException("Unable to update Condition - 'id' not provided");
            }

            Condition condition = rulesAPI.getConditionById(conditionId, user, false);

            if (condition==null) {
                Logger.info(this, "Condition not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Condition not found").build();
            }

            condition.setName(conditionJSON.getString("name"));
            condition.setRuleId(conditionJSON.getString("ruleId"));
            condition.setConditionletId(conditionJSON.getString("conditionlet"));
            condition.setConditionGroup(conditionJSON.getString("conditionGroupId"));
            condition.setComparison(conditionJSON.getString("comparison"));
            condition.setOperator(Condition.Operator.valueOf(conditionJSON.optString("operator", Condition.Operator.AND.name())));
            condition.setPriority(conditionJSON.optInt("priority", 0));

            com.dotcms.repackage.org.codehaus.jettison.json.JSONArray valuesJSON = conditionJSON.getJSONArray("values");
            List<ConditionValue> values = new ArrayList<>();

            if(UtilMethods.isSet(values)) {
                for(int i=0; i<valuesJSON.length(); i++) {
                    JSONObject valueJSON = (JSONObject) valuesJSON.get(i);
                    ConditionValue value = rulesAPI.getConditionValueById(valueJSON.getString("id"), user, false);
                    value.setValue(valueJSON.getString("value"));
                    value.setPriority(valueJSON.optInt("priority", 0));
                    values.add(value);
                }
            }

            condition.setValues(values);

            rulesAPI.saveCondition(condition, user, false);

            JSONObject conditionObject = new com.dotmarketing.util.json.JSONObject(condition);
            return responseResource.response(conditionObject.toString());

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error updating Condition", e);
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
    @Path("/sites/{siteId}/rules/{ruleId}/actions/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveRuleAction(@Context HttpServletRequest request,@PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject actionJSON) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            RuleAction action = new RuleAction();
            action.setRuleId(ruleId);

            RuleActionlet actionlet = rulesAPI.findActionlet(actionJSON.getString("actionlet"));

            if(actionlet==null) {
                Logger.info(this, "Rule Actionlet not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule Actionlet not found").build();
            }

            action.setName(actionlet.getName());
            action.setPriority(actionJSON.optInt("priority", 0));
            action.setActionlet(actionJSON.getString("actionlet"));


            com.dotcms.repackage.org.codehaus.jettison.json.JSONArray parametersJSON = actionJSON.getJSONArray("parameters");
            List<RuleActionParameter> parameters = new ArrayList<>();

            if(UtilMethods.isSet(parameters)) {
                for(int i=0; i<parametersJSON.length(); i++) {
                    JSONObject parameterJSON = (JSONObject) parametersJSON.get(i);
                    RuleActionParameter parameter = new RuleActionParameter();
                    parameter.setKey(parameterJSON.getString("key"));
                    parameter.setValue(parameterJSON.getString("value"));
                    parameters.add(parameter);
                }
            }


            action.setParameters(parameters);

            rulesAPI.saveRuleAction(action, user, false);

            resultsObject.put("id", action.getId());
            return responseResource.response(resultsObject.toString());

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error saving Rule Action", e);
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
    @Path("/sites/{siteId}/rules/{ruleId}/actions/{actionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRuleAction(@Context HttpServletRequest request,@PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId,  @PathParam("actionId") String actionId, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject actionJSON) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        ResourceResponse responseResource = new ResourceResponse(initData.getParamsMap());
        User user = initData.getUser();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            RuleAction action = rulesAPI.getRuleActionById(actionId, user, false);

            if(action==null) {
                Logger.info(this, "Rule Action not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule Action not found").build();
            }


            action.setRuleId(actionJSON.getString("ruleId"));

            RuleActionlet actionlet = rulesAPI.findActionlet(actionJSON.getString("actionlet"));

            if(actionlet==null) {
                Logger.info(this, "Rule Actionlet not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule Actionlet not found").build();
            }

            action.setName(actionlet.getName());
            action.setPriority(actionJSON.optInt("priority", 0));
            action.setActionlet(actionJSON.getString("actionlet"));

            com.dotcms.repackage.org.codehaus.jettison.json.JSONArray parametersJSON = actionJSON.getJSONArray("parameters");
            List<RuleActionParameter> parameters = new ArrayList<>();

            if(UtilMethods.isSet(parameters)) {
                for(int i=0; i<parametersJSON.length(); i++) {
                    JSONObject parameterJSON = (JSONObject) parametersJSON.get(i);
                    RuleActionParameter parameter = rulesAPI.getRuleActionParameterById(parameterJSON.getString("id"), user, false);
                    parameter.setKey(parameterJSON.getString("key"));
                    parameter.setValue(parameterJSON.getString("value"));
                    parameters.add(parameter);
                }
            }

            action.setParameters(parameters);

            rulesAPI.saveRuleAction(action, user, false);

            return responseResource.response(actionJSON.toString());

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error updating Rule Action", e);
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
    @Path("/sites/{siteId}/rules/{ruleId}")
    public Response deleteRule(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("ruleId") String ruleId) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        User user = initData.getUser();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

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
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{conditionGroupId}")
    public Response deleteConditionGroup(@Context HttpServletRequest request, @PathParam("siteId") String siteId,
             @PathParam("ruleId") String ruleId, @PathParam("conditionGroupId") String conditionGroupId) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        User user = initData.getUser();

        try {

            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            ConditionGroup group = rulesAPI.getConditionGroupById(conditionGroupId, user, false);

            if(group==null) {
                Logger.info(this, "Condition Group not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Condition Group not found").build();
            }

            rulesAPI.deleteConditionGroup(group, user, false);

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
    @Path("/sites/{siteId}/rules/{ruleId}/conditiongroups/{groupId}/conditions/{conditionId}")
    public Response deleteCondition(@Context HttpServletRequest request, @PathParam("siteId") String siteId, @PathParam("groupId") String groupId,
                                    @PathParam("ruleId") String ruleId, @PathParam("conditionId") String conditionId) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        User user = initData.getUser();

        try {
            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            ConditionGroup group = rulesAPI.getConditionGroupById(groupId, user, false);

            if(group==null) {
                Logger.info(this, "Condition Group not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Condition Group not found").build();
            }

            Condition condition = rulesAPI.getConditionById(conditionId, user, false);

            if (condition==null) {
                Logger.info(this, "Condition not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Condition not found").build();
            }

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
    @Path("/sites/{siteId}/rules/{ruleId}/actions/{actionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteRuleAction(@Context HttpServletRequest request, @PathParam("siteId") String siteId,
                                     @PathParam("ruleId") String ruleId, @PathParam("actionId") String actionId) throws
            JSONException {
        InitDataObject initData = init(null, true, request, true);
        User user = initData.getUser();

        try {
            Host host = APILocator.getHostAPI().find(siteId, user, false);

            if(host==null) {
                Logger.info(this, "Site not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Site not found").build();
            }

            Rule rule = rulesAPI.getRuleById(ruleId, user, false);

            if (rule==null) {
                Logger.info(this, "Rule not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule not found").build();
            }

            RuleAction action = rulesAPI.getRuleActionById(actionId, user, false);

            if(action==null) {
                Logger.info(this, "Rule Action not found");
                return Response.status(HttpStatus.SC_NOT_FOUND).entity("Rule Action not found").build();
            }

            rulesAPI.deleteRuleAction(action, user, false);

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
            groupsJSON.put(group.getId(), new com.dotmarketing.util.json.JSONObject(group, new String[]{"operator", "priority"}));
        }

        ruleJSON.put("conditionGroups", groupsJSON);

        List<RuleAction> actions = rulesAPI.getRuleActionsByRule(rule.getId(), user, false);
        JSONObject actionsJSON = new JSONObject();

        for (RuleAction action : actions) {
            groupsJSON.put(action.getId(), new com.dotmarketing.util.json.JSONObject(action, new String[]{"priority"}));
        }

        ruleJSON.put("actions", actionsJSON);
        return ruleJSON;
    }

    private Rule saveUpdateRule(User user, com.dotcms.repackage.org.codehaus.jettison.json.JSONObject ruleJSON, String siteId, Rule rule, boolean save) throws DotDataException, DotSecurityException,
            JSONException {


        if (save) {
            rule = new Rule();
            rule.setHost(siteId);
            rule.setName(ruleJSON.getString("name"));

        } else {

            try {
                rule.setName(ruleJSON.getString("name"));
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
