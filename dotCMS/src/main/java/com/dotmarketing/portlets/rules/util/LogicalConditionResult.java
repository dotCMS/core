package com.dotmarketing.portlets.rules.util;

import java.util.Optional;

/**
 * @author Geoff M. Granum
 */
public class LogicalConditionResult {
    public final Optional<Boolean> value;
    public final boolean shortCircuited;

    public LogicalConditionResult(Optional<Boolean> value, boolean shortCircuited) {
        this.value = value;
        this.shortCircuited = shortCircuited;
    }
}
 
