package org.apache.felix.framework;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.util.Config;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class OSGIActionletLateRegistration {

    private static final String OSGI_REGISTRATION_ATTEMPTS_MAX_KEY = "OSGI_REGISTRATION_ATTEMPTS_MAX";
    private static final int REGISTRATION_DELAY = 500;
    private static final Object LOCK = new Object();

    private final Hashtable<String, OSGIRegistrationAttempt> registrationAttempts = new Hashtable<>();

    private Future<List<OSGIRegistrationAttempt>> registrationFuture;

    public void addActionlet(final BundleContext context,
                             final WorkFlowActionlet actionlet,
                             final Collection<WorkFlowActionlet> registered) {
        registrationAttempts.put(actionlet.getName(), new OSGIRegistrationAttempt(context, actionlet, registered));

        synchronized (LOCK) {
            startIfHasNot();
        }


    }

    public boolean isDone() {
        final int maxAttempts = Config.getIntProperty(OSGI_REGISTRATION_ATTEMPTS_MAX_KEY, 12);
        return registrationAttempts.isEmpty()
                || registrationAttempts.values()
                .stream()
                .allMatch(attempt -> attempt.isReadyToRegister() || attempt.getTries() == maxAttempts);
    }

    private void startIfHasNot() {
        synchronized (LOCK) {
            if (registrationFuture == null) {
                registrationFuture = DotConcurrentFactory.getInstance().getSubmitter().submit(this::checkRegistrations);
            }
        }
    }

    private boolean checksLeft(int maxAttempts) {
        return registrationAttempts.isEmpty() ||
                registrationAttempts.values()
                    .stream()
                    .allMatch(attempt -> attempt.isReadyToRegister() || attempt.getTries() == maxAttempts);
    }

    private List<OSGIRegistrationAttempt> checkRegistrations() throws InterruptedException {
        final int maxAttempts = Config.getIntProperty(OSGI_REGISTRATION_ATTEMPTS_MAX_KEY, 12);
        while (!checksLeft(maxAttempts)) {
            registrationAttempts.values().forEach(attempt -> {
                if (OSGIUtil.getInstance().isReadyToRegisterActionlet(attempt.getContext())) {
                    attempt.setReadyToRegister(true);
                } else if (attempt.getTries() < maxAttempts) {
                    attempt.incrementTries();
                }
            });
            TimeUnit.MILLISECONDS.sleep(REGISTRATION_DELAY);
        }

        return new ArrayList<>(registrationAttempts.values());
    }
}
