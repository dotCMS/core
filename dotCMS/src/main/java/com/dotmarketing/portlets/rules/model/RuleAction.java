package com.dotmarketing.portlets.rules.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.RuleComponentModel;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RuleAction implements RuleComponentModel, Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String ruleId;
    private int priority;
    private String actionlet;
    private Date modDate;
    private Map<String, ParameterModel> parameters;
    private transient RuleActionlet actionDef;
    private transient volatile RuleComponentInstance instance;

    /**
     * Stored in {@code instance} when {@link #checkValid()} refuses the action; {@link #evaluate}
     * then returns {@code false} without running the actionlet.
     */
    private static final RuleComponentInstance REFUSED = new RuleComponentInstance() { };

    /**
     * Actionlet and refused name pairs already logged on this node, so each refusal is logged once.
     */
    private static final Set<String> REFUSED_LOGGED = ConcurrentHashMap.newKeySet();

    public RuleAction() {

    }

    public RuleAction(RuleAction ruleActionToCopy){
        id = ruleActionToCopy.id;
        ruleId = ruleActionToCopy.ruleId;
        priority = ruleActionToCopy.priority;
        actionlet = ruleActionToCopy.actionlet;
        modDate = ruleActionToCopy.modDate;
        if(ruleActionToCopy.getParameters().values() != null){
            parameters = Maps.newHashMap();
            for (ParameterModel parameterModel : ruleActionToCopy.getParameters().values()) {
                parameters.put(parameterModel.getKey(), new ParameterModel(parameterModel));
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getActionlet() {
        return actionlet;
    }

    public void setActionlet(String actionlet) {
        this.actionlet = actionlet;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    @JsonProperty("parameters")
    public void setParameters(Map<String, ParameterModel> parameters) {
        this.parameters = parameters;
    }

    @JsonIgnore
    public void setParameters(List<ParameterModel> params) {
        Map<String, ParameterModel> finalParameters = Maps.newHashMap();
        for (ParameterModel parameter : params) {
            finalParameters.put(parameter.getKey(), parameter);
        }
        this.parameters = finalParameters;
    }

    public void addParameter(ParameterModel parameter) {
        if (this.parameters == null){
            this.parameters = Maps.newHashMap();
        }

        this.parameters.put(parameter.getKey(), parameter);
    }

    public Map<String, ParameterModel> getParameters(){
        return parameters;
    }

    /**
     * Validates this action before its rule fires and prepares it for {@link #evaluate}. An action
     * whose actionlet refuses one of its names (see {@link RuleActionlet#getRefusedName(Map)}) is
     * marked as refused instead of throwing, so the rule's other actions and the site's other rules
     * keep running; the refusal is logged once per actionlet and name on this node.
     *
     * @throws IllegalArgumentException When the actionlet of this action does not exist.
     */
    public void checkValid() {
        final RuleActionlet<?> actionDefinition = getActionDefinition();

        if (actionDefinition == null){
            String message = String.format("RuleActionlet %1$s doesn't exist", actionlet);
            throw new IllegalArgumentException(message);
        }else{
            final Optional<String> refusedName = actionDefinition.getRefusedName(getParameters());
            if (refusedName.isPresent()) {
                this.instance = REFUSED;
                if (REFUSED_LOGGED.add(actionlet + ":" + refusedName.get())) {
                    Logger.warn(RuleAction.class, "Rule action ignored: " + refusedName.get()
                            + " is not allowed for " + actionlet);
                }
                return;
            }
            this.instance = actionDefinition.doCheckValid(this);
        }
    }

    /**
     * Runs this action. Call {@link #checkValid()} first.
     *
     * @param req The current request.
     * @param res The current response.
     * @return The actionlet result, or {@code false} without doing anything when the action was
     * refused by {@link #checkValid()}.
     */
    public final boolean evaluate(HttpServletRequest req, HttpServletResponse res) {
        final RuleComponentInstance current = this.instance;
        if (current == REFUSED) {
            return false;
        }
        //noinspection unchecked
        return getActionDefinition().doEvaluate(req, res, current);
    }

    public RuleActionlet getActionDefinition(){
        if(actionDef == null) {
            actionDef = APILocator.getRulesAPI().findActionlet(actionlet);
        }
        return actionDef;
    }

	@Override
	public String toString() {
		return "RuleAction [id=" + id + ", priority=" + priority + ", actionlet=" + actionlet
				+ ", modDate=" + modDate + "]";
	}

}
