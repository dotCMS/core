package com.dotcms.rest;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.CacheControl;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.lang.StringEscapeUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
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
	@Path("/rules/{params:.*}")
	@Produces("application/json")
	public Response getRules(@Context HttpServletRequest request, @PathParam("params") String params) throws DotStateException, DotDataException, DotSecurityException, LanguageException, JSONException {
		InitDataObject initData = init(params, true, request, true);
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        User user = initData.getUser();

        String ruleId = initData.getParamsMap().get("id");

        if(UtilMethods.isSet(ruleId)) {
            Rule rule = APILocator.getRulesAPI().getRuleById(ruleId, user, false);
            JSONObject ruleObject = new JSONObject(rule);
            return responseResource.response(ruleObject.toString());
        }

        JSONArray jsonRules = new JSONArray();
        String hostIdentifier = initData.getParamsMap().get("host");
        String folderIdentifier = initData.getParamsMap().get("folder");

        if(!UtilMethods.isSet(hostIdentifier) || !UtilMethods.isSet(folderIdentifier)) {
            return responseResource.response(jsonRules.toString());
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

        return responseResource.response(jsonRules.toString());
    }

    /**
     * <p>Returns a JSON with the Condition Groups and its Conditions for the rule with the given ruleId.
     * <br>Each Rule node contains all fields in  .
     *
     * Usage: /condition/
     * @throws com.dotmarketing.util.json.JSONException
     *
     */

    @GET
    @Path("/conditions/{params:.*}")
    @Produces("application/json")
    public Response getConditions(@Context HttpServletRequest request, @PathParam("params") String params) throws DotStateException, DotDataException, DotSecurityException, LanguageException, JSONException {
        InitDataObject initData = init(params, true, request, true);
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        User user = initData.getUser();

        String conditionId = initData.getParamsMap().get("id");

        if(UtilMethods.isSet(conditionId)) {
            Condition condition = APILocator.getRulesAPI().getConditionById(conditionId, user, false);
            JSONObject conditionObject = new JSONObject(condition);
            return responseResource.response(conditionObject.toString());
        }

        JSONObject jsonRuleExpression = new JSONObject();

        String ruleId = initData.getParamsMap().get("ruleId");

        if(!UtilMethods.isSet(ruleId)) {
            return responseResource.response(jsonRuleExpression.toString());
        }

        Rule rule = APILocator.getRulesAPI().getRuleById(ruleId, user, false);

        if(!UtilMethods.isSet(rule) || !UtilMethods.isSet(rule.getId())) {
            return responseResource.response(jsonRuleExpression.toString());
        }

        JSONObject resultsObject = new JSONObject();
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

        return null;
    }


    /**
     * <p>Returns a JSON with the Condition Groups and its Conditions for the rule with the given ruleId.
     * <br>Each Rule node contains all fields in  .
     *
     * Usage: /getconditions/{roleid}
     * @throws com.dotmarketing.util.json.JSONException
     *
     */

    @GET
    @Path("/conditionlets/{params:.*}")
    @Produces("application/json")
    public Response getConditionlets(@Context HttpServletRequest request, @PathParam("params") String params) throws DotStateException, DotDataException, DotSecurityException, LanguageException, JSONException {
        InitDataObject initData = init(params, true, request, true);
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        User user = initData.getUser();

        JSONObject resultsObject = new JSONObject();
        JSONArray jsonConditionGroups = new JSONArray();

        List<Conditionlet> conditionlets = APILocator.getRulesAPI().getConditionlets();

        resultsObject.put("conditionGroups", jsonConditionGroups);
        String a = "sdf";
        a

        return null;
    }




}
