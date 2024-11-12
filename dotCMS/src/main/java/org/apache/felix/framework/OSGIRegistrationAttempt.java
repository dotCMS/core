package org.apache.felix.framework;

import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import org.osgi.framework.BundleContext;

import java.util.Collection;

public class OSGIRegistrationAttempt {

    private final BundleContext context;
    private final WorkFlowActionlet toRegister;
    private final Collection<WorkFlowActionlet> tracked;
    private int tries;
    private boolean readyToRegister;

    public OSGIRegistrationAttempt(final BundleContext context,
                                   final WorkFlowActionlet toRegister,
                                   final Collection<WorkFlowActionlet> tracked) {
        this.context = context;
        this.toRegister = toRegister;
        this.tracked = tracked;
        tries = 0;
        readyToRegister = false;
    }

    public BundleContext getContext() {
        return context;
    }

    public WorkFlowActionlet getToRegister() {
        return toRegister;
    }

    public Collection<WorkFlowActionlet> getTracked() {
        return tracked;
    }

    public int getTries() {
        return tries;
    }

    public boolean isReadyToRegister() {
        return readyToRegister;
    }

    public void setReadyToRegister(boolean readyToRegister) {
        this.readyToRegister = readyToRegister;
    }

    public void incrementTries() {
        tries++;
    }

}
