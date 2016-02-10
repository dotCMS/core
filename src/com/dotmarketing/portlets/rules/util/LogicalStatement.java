package com.dotmarketing.portlets.rules.util;

import com.dotcms.repackage.com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;

/**
 * @author Geoff M. Granum
 */
public class LogicalStatement implements LogicalCondition {

    private final List<Thing> terms = Lists.newArrayList();

    public LogicalStatement() {
    }

    public LogicalStatement and(LogicalCondition condition){
        terms.add(new Thing(LogicalOperator.AND, condition));
        return this;
    }

    public LogicalStatement or(LogicalCondition condition) {
        terms.add(new Thing(LogicalOperator.OR, condition));
        return this;
    }

    @Override
    public boolean evaluate() {
        LogicalConditionResult result = new LogicalConditionResult(Optional.empty(), false);
        for (Thing term : terms) {
            result = term.op.evaluate(result, term.condition);
            if(result.shortCircuited){
                break;
            }
        }
        return result.value.orElse(true);
    }

    private static class Thing {
        private final LogicalOperator op;
        private final LogicalCondition condition;

        public Thing(LogicalOperator op, LogicalCondition condition) {
            this.op = op;
            this.condition = condition;
        }
    }

    public static class BooleanCondition implements LogicalCondition {

        private final boolean value;

        public BooleanCondition(boolean value) {
            this.value = value;
        }

        @Override
        public boolean evaluate() {
            return value;
        }

    }
}
 
