package com.dotmarketing.portlets.rules.model;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.util.LogicalCondition;
import com.dotmarketing.portlets.rules.util.LogicalStatement;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ConditionGroup implements Serializable, Comparable<ConditionGroup> {
    private static final long serialVersionUID = 1L;
    private String id;
    private String ruleId;
    private LogicalOperator operator;
    private Date modDate;
    private int priority;
    List<Condition> conditions;

    public ConditionGroup(){

    }

    public ConditionGroup(ConditionGroup conditionGroupToCopy){
        id = conditionGroupToCopy.id;
        ruleId = conditionGroupToCopy.ruleId;
        operator = conditionGroupToCopy.operator;
        modDate = conditionGroupToCopy.modDate;
        priority = conditionGroupToCopy.priority;
        if(conditionGroupToCopy.getConditions() != null) {
            conditions = Lists.newArrayList();
            for (Condition condition : conditionGroupToCopy.getConditions()) {
                conditions.add(new Condition(condition));
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

    public LogicalOperator getOperator() {
        return operator;
    }

    public void setOperator(LogicalOperator operator) {
        this.operator = operator;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<Condition> getConditions() {
        if(conditions == null) {
            try {
                //This will return the Conditions sorted by priority asc directly from DB.
                conditions = FactoryLocator.getRulesFactory().getConditionsByGroup(this.id);
            } catch (DotDataException e) {
                throw new RuleEngineException(e, "Could not load conditions for group %s.", this.toString());
            }
        }

        //Return a shallow copy of the list.
        return Lists.newArrayList(conditions);
    }

    public void checkValid(){
        for (Condition condition : getConditions()) {
            condition.checkValid();
        }
    }

    public boolean evaluate(HttpServletRequest req, HttpServletResponse res, List<Condition> conditions) {
        LogicalStatement statement = new LogicalStatement();
        for (Condition cond : conditions) {
            ConditionLogicalCondition logicalCondition = new ConditionLogicalCondition(cond, req, res);
            if(cond.getOperator() == LogicalOperator.AND) {
                statement.and(logicalCondition);
            } else {
                statement.or(logicalCondition);
            }
        }
        return statement.evaluate();
    }

    @Override
	public String toString() {
		return "ConditionGroup [id=" + id + ", ruleId=" + ruleId
				+ ", operator=" + operator + ", modDate=" + modDate
				+ ", priority=" + priority + "]";
	}

    @Override
    public int compareTo(ConditionGroup c) {
        return Integer.compare(this.priority, c.getPriority());
    }
    private final class ConditionLogicalCondition implements LogicalCondition {

        private final Condition condition;
        private final HttpServletRequest req;
        private final HttpServletResponse res;

        public ConditionLogicalCondition(Condition condition, HttpServletRequest req, HttpServletResponse res) {
            this.condition = condition;
            this.req = req;
            this.res = res;
        }

        @Override
        public boolean evaluate() {
            return condition.evaluate(req, res);
        }
    }

}
