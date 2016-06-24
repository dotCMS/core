package com.dotmarketing.portlets.rules.model;

import com.dotmarketing.portlets.rules.util.LogicalCondition;

/**
 * @author Geoff M. Granum
 */
public enum LogicalOperator {
    AND(40) {
        @Override
        public boolean evaluate(boolean previous, LogicalCondition condition) {
            boolean result = !previous;
            if(previous) {
                result = condition.evaluate();
            }
            return result;
        }

        /**
         * For 'A && B()', short circuit if A is false. B will not be evaluated. 'A' will
         * be the result for the comparison.
         */
        @Override
        public boolean isShortCircuitedBy(boolean leftArg) {
            return !leftArg;
        }
    },

    OR(30) {
        @Override
        public boolean evaluate(boolean previous, LogicalCondition condition) {
            boolean result = previous;
            if(!previous) {
                result = condition.evaluate();
            }
            return result;
        }

        /**
         * For 'A || B()', short circuit if A is true. B will not be evaluated. 'A' will
         * be the result for the comparison.
         */
        @Override
        public boolean isShortCircuitedBy(boolean leftArg) {
            return leftArg;
        }
    };

    public final int precedence;
    LogicalOperator(int precedence) {
        this.precedence = precedence;
    }

    public abstract boolean evaluate(boolean previous, LogicalCondition condition);

    @Override
    public String toString() {
        return super.name();
    }

    public abstract boolean isShortCircuitedBy(boolean value);
}
 
