package org.apache.felix.framework;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OSGIActionletLateRegistration {

    private static final String OSGI_REGISTRATION_CHECKS_MAX_KEY = "OSGI_REGISTRATION_ATTEMPTS_MAX";
    private static final int REGISTRATION_DELAY = 500;
    private static final Object LOCK = new Object();

    private final ConcurrentMap<String, OSGIRegistrationCheck> registrationChecks = new ConcurrentHashMap<>();

    private Future<List<OSGIRegistrationCheck>> registrationCheckFuture;

    public void addActionlet(final BundleContext context,
                             final WorkFlowActionlet actionlet,
                             final Collection<WorkFlowActionlet> registered) {
        Logger.info(this, String.format("Adding actionlet [%s] for later registration", actionlet.getName()));
        registrationChecks.put(actionlet.getName(), new OSGIRegistrationCheck(context, actionlet, registered));
        startIfHasNot();
    }

    public List<OSGIRegistrationCheck> waitForRegistrationChecks() throws InterruptedException, ExecutionException {
        synchronized (LOCK) {
            if (registrationCheckFuture == null) {
                return List.of();
            }

            while(!registrationCheckFuture.isDone()) {
                Logger.info(this, "Waiting for registration checks to finish");
                TimeUnit.MILLISECONDS.sleep(250);
            }

            final List<OSGIRegistrationCheck> checks = registrationCheckFuture.get();
            cleanUp();

            return checks;
        }
    }

    private void startIfHasNot() {
        synchronized (LOCK) {
            if (registrationCheckFuture == null) {
                registrationCheckFuture = DotConcurrentFactory.getInstance().getSubmitter().submit(this::checkRegistrations);
            }
        }
    }

    private boolean checkIfLeft(int maxAttempts) {
        return registrationChecks.isEmpty() ||
                registrationChecks.values()
                    .stream()
                    .allMatch(attempt -> attempt.isReadyToRegister() || attempt.getAttempts() == maxAttempts);
    }

    private List<OSGIRegistrationCheck> checkRegistrations() throws InterruptedException {
        final int maxAttempts = Config.getIntProperty(OSGI_REGISTRATION_CHECKS_MAX_KEY, 10);
        Logger.info(this, "Starting OSGI actionlet registration checks");

        while (!checkIfLeft(maxAttempts)) {
            registrationChecks.values().forEach(attempt -> {
                Logger.info(
                        this,
                        String.format(
                                "New OSGI registration check for actionlet [%s]",
                                attempt.getActionlet().getName()));
                if (OSGIUtil.getInstance().isReadyToRegisterActionlet(attempt.getContext())) {
                    Logger.info(
                            this,
                            String.format(
                                    "Detected that context is ready for actionlet [%s]",
                                    attempt.getActionlet().getName()));
                    attempt.setReadyToRegister(true);
                } else if (attempt.getAttempts() < maxAttempts) {
                    Logger.info(
                            this,
                            String.format(
                                    "Context is NOT ready for actionlet [%s], giving another try",
                                    attempt.getActionlet().getName()));
                    attempt.incrementAttempts();
                }
            });
            TimeUnit.MILLISECONDS.sleep(REGISTRATION_DELAY);
        }

        final List<OSGIRegistrationCheck> result = new ArrayList<>(registrationChecks.values());
        Logger.info(
                this,
                String.format(
                        "Registration checks finished with following registrations statuses: [%s]",
                        result.stream()
                                .map(OSGIRegistrationCheck::toString)
                                .collect(Collectors.joining(StringPool.COMMA))));

        return result;
    }

    private void cleanUp() {
        registrationChecks.clear();
        registrationCheckFuture = null;
    }
}
