package com.dotcms.observability.service;

import com.dotcms.observability.state.State;
import com.dotcms.observability.state.StateChangeEvent;
import com.dotcms.observability.subsystem.Subsystem;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class SystemStateServiceImpl implements SystemStateService {

    private ConcurrentHashMap<String, AtomicReference<State>> subsystemStates;
    private ConcurrentHashMap<String, Subsystem> subsystems;
    private ExecutorService stateChangeExecutor;

    @Override
    public State getState(String subsystemName) {
        return subsystemStates.get(subsystemName).get();
    }

    @Override
    public Subsystem getSubsystem(String subsystemName) {
        return subsystems.get(subsystemName);
    }

    @Override
    public List<Subsystem> getSubsystems() {
        return List.copyOf(subsystems.values());
    }

    @Override
    public void submitStateChange(StateChangeEvent event) {
        // implementation
    }

    private void updateSubsystemState(String subsystemName, State newState) {
        // implementation
    }
}

