package com.dotmarketing.portlets.rules.model;

import com.dotmarketing.portlets.rules.util.LogicalCondition;
import com.dotmarketing.portlets.rules.util.LogicalConditionResult;
import com.dotmarketing.portlets.rules.exception.OperationAlreadyCompleteException;
import java.util.Optional;

/**
 * @author Geoff M. Granum
 */
public enum LogicalOperator {
    AND {
        @Override
        public LogicalConditionResult evaluate(LogicalConditionResult previous, LogicalCondition condition) {
            if(previous.shortCircuited) {
                throw new OperationAlreadyCompleteException("Logical evaluation is already complete.");
            }
            LogicalConditionResult result;
            if(previous.value.isPresent() && !previous.value.get()) {
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
                throw new OperationAlreadyCompleteException("Logical evaluation is already complete.");
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

    @Override
    public String toString() {
        return super.name();
    }


}
 
