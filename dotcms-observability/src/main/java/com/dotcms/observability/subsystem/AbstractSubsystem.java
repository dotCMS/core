package com.dotcms.observability.subsystem;

import com.dotcms.observability.resilence.ResilienceExecutor;
import com.dotcms.observability.service.SystemStateService;
import com.dotcms.observability.state.State;
import com.dotcms.observability.state.StateChangeEvent;
import jakarta.enterprise.event.Event;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;


public abstract class AbstractSubsystem implements Subsystem {

    private String name;
    private String description;
    private AbstractSubsystem parent;
    private ConcurrentHashMap<String, AbstractSubsystem> children;
    private AtomicReference<State> currentState;
    protected SystemStateService stateService;
    protected ScheduledExecutorService scheduler;
    protected Event<StateChangeEvent> stateChangeEvent;
    private AtomicReference<ScheduledFuture<?>> statusCheckTask;
    private AtomicReference<Duration> currentCheckInterval;
    private ResilienceExecutor resilienceExecutor;

    protected abstract State checkStatus();

    protected void init() {
        // implementation
    }

    protected void initializeStatusCheck() {
        // implementation
    }

    protected void submitStateChange(StateChangeEvent event) {
        // implementation
    }

    public void setParent(AbstractSubsystem parent) {
        this.parent = parent;
    }

    public Optional<AbstractSubsystem> getParent() {
        return Optional.ofNullable(parent);
    }

    public void addChild(AbstractSubsystem child) {
        children.put(child.getName(), child);
    }

    public void removeChild(String name) {
        children.remove(name);
    }

    public Map<String, AbstractSubsystem> getChildren() {
        return children;
    }

    protected void updateState(State state) {
        // implementation
    }

    protected void propagateStateChangeToParent() {
        // implementation
    }

    protected void onChildStateChange() {
        // implementation
    }

    private void performStatusCheck() {
        // implementation
    }

    private void scheduleNextStatusCheck(Duration duration) {
        // implementation
    }
/*
    protected State aggregateChildStates() {
        return State.builder().build();
    }

    protected State fallbackOperation() {
        return State.builder().build();
    }
 */
}

