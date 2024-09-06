package com.dotcms.observability.state;

import org.immutables.value.Value;

@Value.Immutable
public interface StateChangeEvent {
    String subsystemName();
    State newState();
}
