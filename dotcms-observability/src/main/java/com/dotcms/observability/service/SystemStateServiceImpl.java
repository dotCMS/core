package com.dotcms.observability.service;

import com.dotcms.observability.state.State;
import com.dotcms.observability.state.StateChangeEvent;
import com.dotcms.observability.subsystem.Subsystem;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class SystemStateServiceImpl implements SystemStateService {

    private ConcurrentHashMap<String, AtomicReference<State>> subsystemStates = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Subsystem> subsystems = new ConcurrentHashMap<>();
    private ExecutorService stateChangeExecutor = Executors.newFixedThreadPool(2);

    @Override
    public Optional<State> getState(String subsystemName) {
        final AtomicReference<State> reference = subsystemStates.get(subsystemName);
        return Optional.ofNullable(reference).map(AtomicReference::get);
    }

    @Override
    public Optional<Subsystem> getSubsystem(String subsystemName) {
        final Subsystem subsystem = subsystems.get(subsystemName);
        return Optional.ofNullable(subsystem);
    }

    @Override
    public List<Subsystem> getSubsystems() {
        return List.copyOf(subsystems.values());
    }

    @Override
    public void submitStateChange(StateChangeEvent event) {
        stateChangeExecutor.submit(() -> updateSubsystemState(event.subsystemName(), event.newState()));
    }

    private void updateSubsystemState(final String subsystemName, final State newState) {
        final AtomicReference<State> reference = subsystemStates.computeIfAbsent(subsystemName, k -> new AtomicReference<>(newState));
        reference.set(newState);
    }
}

