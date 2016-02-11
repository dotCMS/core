package com.dotmarketing.portlets.rules.util;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotmarketing.portlets.rules.model.LogicalOperator;
import java.util.List;
import java.util.Optional;

/**
 * @author Geoff M. Granum
 */
public class LogicalStatement implements LogicalCondition {

    private final List<Term> terms = Lists.newArrayList();

    public LogicalStatement() {
    }

    public LogicalStatement and(LogicalCondition condition){
        terms.add(new Term(LogicalOperator.AND, condition));
        return this;
    }

    public LogicalStatement or(LogicalCondition condition) {
        terms.add(new Term(LogicalOperator.OR, condition));
        return this;
    }

    @Override
    public boolean evaluate() {
        LogicalConditionResult result = new LogicalConditionResult(Optional.empty(), false);
        for (Term term : terms) {
            result = term.op.evaluate(result, term.condition);
            if(result.shortCircuited){
                break;
            }
        }
        return result.value.orElse(true);
    }

    private static class Term {
        private final LogicalOperator op;
        private final LogicalCondition condition;

        public Term(LogicalOperator op, LogicalCondition condition) {
            this.op = op;
            this.condition = condition;
        }
    }
}
 
