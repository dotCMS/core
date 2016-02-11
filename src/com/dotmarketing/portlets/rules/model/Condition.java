package com.dotmarketing.portlets.rules.model;

import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.RuleComponentModel;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;

import java.util.ArrayList;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Condition implements RuleComponentModel, Serializable {

    private static final long serialVersionUID = 1L;
    private transient RuleComponentInstance instance;

    private String id;
    private String name;
    private String conditionletId;
    private String conditionGroup;
    private List<ParameterModel> values;
    private Date modDate;
    private LogicalOperator operator;
    private int priority;
    private transient Conditionlet conditionlet;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConditionletId() {
        return conditionletId;
    }

    public void setConditionletId(String conditionletId) {
        this.conditionletId = conditionletId;
    }

    public String getConditionGroup() {
        return conditionGroup;
    }

    public void setConditionGroup(String conditionGroup) {
        this.conditionGroup = conditionGroup;
    }

    public List<ParameterModel> getValues() {
        return values;
    }

    public Map<String, ParameterModel> getParameters() {
        Map<String, ParameterModel> p = Maps.newLinkedHashMap();

        if(values==null) { values = new ArrayList<>(); }

        for (ParameterModel value : values) {
            p.put(value.getKey(), value);
        }
        return p;
    }

    public void addValue(String key, String value){
        if(getValues()==null)
            setValues(new ArrayList<>());

        getValues().add(new ParameterModel(key, value));
    }

    public void addValue(ParameterModel parameterModel) {
        if(getValues()==null)
            setValues(new ArrayList<>());

        getValues().add(parameterModel);
    }

    public void setValues(List<ParameterModel> values) {
        this.values = values;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    public LogicalOperator getOperator() {
        return operator;
    }

    public void setOperator(LogicalOperator operator) {
        this.operator = operator;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void checkValid() {
        this.instance = getConditionlet().doCheckValid(this);
    }

    public Conditionlet getConditionlet() {
        if(conditionlet==null) {
            conditionlet = APILocator.getRulesAPI().findConditionlet(conditionletId);
        }
        return conditionlet;
    }

    public final boolean evaluate(HttpServletRequest req, HttpServletResponse res) {
        //noinspection unchecked
        return getConditionlet().doEvaluate(req, res, instance);
    }


	@Override
	public String toString() {
		return "Condition [id=" + id + ", name=" + name
                + ", conditionletId=" + conditionletId + ", conditionGroup="
				+ conditionGroup + ", values="
				+ values + ", modDate=" + modDate + ", operator=" + operator
				+ ", priority=" + priority + "]";
	}

}
