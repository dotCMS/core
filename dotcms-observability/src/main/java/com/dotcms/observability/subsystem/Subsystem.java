package com.dotcms.observability.subsystem;

import com.dotcms.observability.state.State;
import java.util.List;


public interface Subsystem {
    State getState();
    String getName();
    String getDescription();
    List<Subsystem> getSubsystems();
    void initialize();
    void start();
    void stop();
    void shutdown();
}
