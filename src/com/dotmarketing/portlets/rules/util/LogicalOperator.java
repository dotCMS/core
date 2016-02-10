package com.dotmarketing.portlets.rules.util;

import java.util.Optional;

/**
 * @author Geoff M. Granum
 */
public enum LogicalOperator {

    AND {
        @Override
        public LogicalConditionResult evaluate(LogicalConditionResult previous, LogicalCondition condition) {
            if(previous.shortCircuited){
                throw new OperationAlreadyCompleteException();
            }
            LogicalConditionResult result;
            if(previous.value.isPresent() && !previous.value.get()){
                result = new LogicalConditionResult(Optional.of(false), true);
            } else {
                result = new LogicalConditionResult(Optional.of(condition.evaluate()), false);
            }
            return result;
        }
    },

    OR {
        @Override
        public LogicalConditionResult evaluate(LogicalConditionResult previous, LogicalCondition condition) {
            if(previous.shortCircuited) {
                throw new OperationAlreadyCompleteException();
            }
            LogicalConditionResult result;
            if(previous.value.isPresent() && previous.value.get()) {
                result = new LogicalConditionResult(Optional.of(true), true);
            } else {
                result = new LogicalConditionResult(Optional.of(condition.evaluate()), false);
            }
            return result;
        }
    };

    LogicalOperator() {
    }

    public abstract LogicalConditionResult evaluate(LogicalConditionResult previous, LogicalCondition condition);

}
 
