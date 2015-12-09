package com.dotmarketing.portlets.rules.model;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Condition implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Operator {
        AND,
        OR;

        @Override
        public String toString() {
            return super.name();
        }
    }

    @JsonIgnore
    private String id;
    private String name;
    private String conditionletId;
    private String conditionGroup;
    private String comparison;
    private List<ConditionValue> values;
    private Date modDate;
    private Operator operator;
    private int priority;
    private Conditionlet conditionlet;

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

    public String getComparison() {
        return comparison;
    }

    public void setComparison(String comparison) {
        this.comparison = comparison;
    }

    public List<ConditionValue> getValues() {
        return values;
    }

    public void setValues(List<ConditionValue> values) {
        this.values = values;
    }

    public void addConditionValue(ConditionValue value) {
        if(values!=null) {
            values.add(value);
        }
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Conditionlet getConditionlet() {
        if(conditionlet==null) {
            try {
                conditionlet = APILocator.getRulesAPI().findConditionlet(conditionletId);
            } catch (DotDataException | DotSecurityException e) {
                Logger.error(this, "Unable to load conditionlet for condition with id: " + id);
            }
        }

        return conditionlet;
    }

    public boolean evaluate(HttpServletRequest req, HttpServletResponse res) {
        return getConditionlet().evaluate(req, res, getComparison(), getValues());
    }
	@Override
	public String toString() {
		return "Condition [id=" + id + ", name=" + name
                + ", conditionletId=" + conditionletId + ", conditionGroup="
				+ conditionGroup + ", comparison=" + comparison + ", values="
				+ values + ", modDate=" + modDate + ", operator=" + operator
				+ ", priority=" + priority + "]";
	}

}
