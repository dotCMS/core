package com.dotcms.observability.service;

import com.dotcms.observability.state.State;
import com.dotcms.observability.state.StateChangeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;

@ApplicationScoped
public class SubsystemManager {

    private SubsystemDiscoveryService discoveryService;

    private Event<StateChangeEvent> stateChangeEvent;

    public void init() {
        // implementation
    }

    private void onRootStateChange(State state) {
        // implementation
    }

    public void startAllSubsystems() {
        // implementation
    }

    public void stopAllSubsystems() {
        // implementation
    }

    public void restartSubsystem(String subsystemName) {
        // implementation
    }
}
