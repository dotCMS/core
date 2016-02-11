package com.dotmarketing.portlets.rules.util;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotmarketing.portlets.rules.model.LogicalOperator;
import java.util.List;
import java.util.Stack;

/**
 * @author Geoff M. Granum
 */
public class LogicalStatement implements LogicalCondition {

    private final List<Term> terms = Lists.newArrayList();


    public LogicalStatement() {
    }

    public LogicalStatement and(LogicalCondition condition){
        this.addTerm(LogicalOperator.AND, condition);
        return this;
    }

    public LogicalStatement or(LogicalCondition condition) {
        this.addTerm(LogicalOperator.OR, condition);
        return this;
    }

    protected void addTerm(LogicalOperator op, LogicalCondition condition) {
        if(!terms.isEmpty()) {
            terms.get(terms.size() - 1).op = op;
        }
        terms.add(new Term(condition));
    }

    @Override
    public boolean evaluate() {
        Stack<Term> stack = new Stack<>();
        for (int i = terms.size() - 1; i >= 0; i--) {
            Term term = terms.get(i);
            stack.push(term);
        }

        Boolean result = null;
        while (!stack.empty()) {
            Term left = stack.pop();
            result = left.evaluate();
            if(stack.empty() || left.op.isShortCircuitedBy(result)) {
                break;
            }
            Term right = stack.pop();
            if(right.op == null) {
                result = left.op.evaluate(result, right.condition);
                break;
            }
            if(right.op.precedence > left.op.precedence) {
                Term c = stack.pop();
                LogicalStatement s = new LogicalStatement();
                s.and(right.condition).and(c.condition);
                Term temp = new Term(s);
                temp.op = c.op;
                stack.push(temp);
                stack.push(left);
            } else {
                result = left.op.evaluate(result, right.condition);
                stack.push(new BooleanTerm(new BooleanCondition(result), right.op));
            }
        }
        return result == null ? true : result;
    }

    private static class Term {

        private LogicalOperator op;
        private LogicalCondition condition;
        private Boolean result = null;

        public Term(LogicalCondition condition) {
            this.condition = condition;
        }

        public boolean evaluate() {
            if(result == null) {
                result = condition.evaluate();
            }
            return result;
        }
    }

    protected static class BooleanCondition implements LogicalCondition {

        boolean value;

        public BooleanCondition(boolean value) {
            this.value = value;
        }

        @Override
        public boolean evaluate() {
            return value;
        }
    }

    private static class BooleanTerm extends Term {

        public BooleanTerm(LogicalCondition condition, LogicalOperator op) {
            super(condition);
            super.op = op;
        }
    }
}
 
