package com.dotcms.observability.state;

import java.time.Instant;
import org.immutables.value.Value;

@Value.Immutable
public interface  State {

    StateEnum stateEnum();
    String message();
    Instant timestamp();
    SeverityLevel severityLevel();

}
