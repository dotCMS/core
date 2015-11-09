package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Objects;
import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Arrays;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class RuleActionlet implements Serializable {

    private static final long serialVersionUID = -6721673381070066205L;

    private final String id;
    private final String name;
    private final List<ActionletParameterWrapper> parameters;

    public RuleActionlet(String name) {
        this.name = name;
        this.id = this.getClass().getSimpleName();
        this.parameters = new ArrayList<ActionletParameterWrapper>();
    }


    public RuleActionlet(String name, ActionletParameterWrapper[] parametersArray) {
        this.name = name;
        this.id = this.getClass().getSimpleName();
        this.parameters = new ArrayList<ActionletParameterWrapper>(Arrays.asList(parametersArray));
    }


    /**
     * The unique type id for this Actionlet implementation.
     *
     * @return a unique id for this Actionlet type
     */
    @NotNull
    public final String getId() {
        return this.id;
    }

    /**
	 * Returns the human readable name for this Actionlet
	 */
	public final String getName(){
        return this.name;
    }

    public String getI18nKey(){
        return "ruleengine.actionlet." + getId();
    }

	/**
	 * returns the map of parameters that are accepted by the implementing actionlet
	 * @return
	 */
    public Map<String, Map<String,String>> getMappedParameters(){
    	Map<String, Map<String,String>> mappedValues = new HashMap<String, Map<String,String>>();
    	if(!parameters.isEmpty()){
    		for(ActionletParameterWrapper parameter: parameters){
    			mappedValues.put(parameter.getKey(), parameter.toMappedValues());
    		}
    	}
    	return mappedValues;
    }

    public List<ActionletParameterWrapper> getParameters(){
    	return parameters;
    }


    /**
     * returns true if the actionlet does not recieve parameters
     */
    public boolean hasParameters(){
    	return parameters.isEmpty();
    }

	/**
	 * if this is set, the all subsequent actionlets will not be fired.  This is true when executing both the
	 * preactions and the postactions
	 */
	public boolean stopProcessing(){
		return false;
	}

	/**
	 * Action that gets executed when the owner {@link com.dotmarketing.portlets.rules.conditionlet.Conditionlet} evaluates to true
	 */
	public abstract void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, RuleActionParameter> params);

    @Override
    public boolean equals(Object o) {
        if(this == o) { return true; }
        if(!(o instanceof RuleActionlet)) { return false; }
        RuleActionlet that = (RuleActionlet)o;
        return Objects.equal(getId(), that.getId()) &&
               Objects.equal(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), getName());
    }
}
