package org.apache.felix.framework;

import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import org.osgi.framework.BundleContext;

import java.util.Collection;
import java.util.Optional;

public class OSGIRegistrationCheck {

    private final BundleContext context;
    private final WorkFlowActionlet actionlet;
    private final Collection<WorkFlowActionlet> actionlets;
    private int attempts;
    private boolean readyToRegister;

    public OSGIRegistrationCheck(final BundleContext context,
                                 final WorkFlowActionlet actionlet,
                                 final Collection<WorkFlowActionlet> actionlets) {
        this.context = context;
        this.actionlet = actionlet;
        this.actionlets = actionlets;
        attempts = 0;
        readyToRegister = false;
    }

    public BundleContext getContext() {
        return context;
    }

    public WorkFlowActionlet getActionlet() {
        return actionlet;
    }

    public Collection<WorkFlowActionlet> getActionlets() {
        return actionlets;
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isReadyToRegister() {
        return readyToRegister;
    }

    public void setReadyToRegister(boolean readyToRegister) {
        this.readyToRegister = readyToRegister;
    }

    public void incrementAttempts() {
        attempts++;
    }

    @Override
    public String toString() {
        return "OSGIRegistrationCheck{" +
                "actionlet=" + Optional.ofNullable(actionlet).map(WorkFlowActionlet::getName).orElse(null) +
                ", attempts=" + attempts +
                ", readyToRegister=" + readyToRegister +
                '}';
    }
}
