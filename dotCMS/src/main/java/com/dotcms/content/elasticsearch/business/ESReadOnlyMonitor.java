package com.dotcms.content.elasticsearch.business;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This monitor is run when either the Live or Working indices are in read-only mode.  It attempts to set both Live and Working indices to write mode again.
 * When setting write-mode fails it will retry after one minute.
 */
public class ESReadOnlyMonitor {
    @VisibleForTesting
    final long timeToWaitAfterWriteModeSet;

    private String readOnlyMessageKey;

    public static final int INTERVAL_IN_MINUTES_TO_CHECK_READ_ONLY = 1;
    private final RoleAPI roleAPI;
    private final SystemMessageEventUtil systemMessageEventUtil;

    private final AtomicBoolean started = new AtomicBoolean();

    private ESReadOnlyMonitor(
            final SystemMessageEventUtil systemMessageEventUtil,
            final RoleAPI roleAPI) {
        super();
        this.systemMessageEventUtil = systemMessageEventUtil;
        this.roleAPI = roleAPI;
        started.set(false);

        timeToWaitAfterWriteModeSet = ElasticsearchUtil.getClusterUpdateInterval() +
                TimeUnit.MINUTES.toMillis(INTERVAL_IN_MINUTES_TO_CHECK_READ_ONLY) +
                TimeUnit.SECONDS.toMillis(10);
    }

    private ESReadOnlyMonitor() {
        this(
                SystemMessageEventUtil.getInstance(),
                APILocator.getRoleAPI()
        );
    }

    // Inner class to provide instance of class
    private static class Singleton
    {
        private static final ESReadOnlyMonitor INSTANCE = new ESReadOnlyMonitor();
    }

    @VisibleForTesting
    public static ESReadOnlyMonitor getInstance(
            final SystemMessageEventUtil systemMessageEventUtil,
            final RoleAPI roleAPI)
    {
        return new ESReadOnlyMonitor(systemMessageEventUtil, roleAPI);
    }
    public static ESReadOnlyMonitor getInstance()
    {
        return Singleton.INSTANCE;
    }

    public void start(final String message){
        if (this.start()) {
            Logger.error(this.getClass(), message);
        }
    }

    /**
     * Start a {@link ESReadOnlyMonitor} is it is not started yet
     * @return false if a ESReadOnlyMonitor was started before
     */
    public boolean start(){

        final boolean clusterInReadOnlyMode = ElasticsearchUtil.isClusterInReadOnlyMode();
        final boolean eitherLiveOrWorkingIndicesReadOnly = ElasticsearchUtil.isEitherLiveOrWorkingIndicesReadOnly();

        if (started.compareAndSet(false, true)) {
            if (clusterInReadOnlyMode) {
                this.readOnlyMessageKey = "es.cluster.read.only.message";
                sendReadOnlyMessage();
                startClusterMonitor();
            } else if (eitherLiveOrWorkingIndicesReadOnly) {
                this.readOnlyMessageKey = "es.index.read.only.message";
                sendReadOnlyMessage();
                startIndexMonitor();
            } else {
                started.set(false);
            }

            return this.started.get();
        } else {
            return false;
        }
    }

    public void sendReadOnlyMessage() {
        sendMessage(readOnlyMessageKey);
    }

    private void sendMessage(final String messageKey) {
        try {
            final String message = LanguageUtil.get(messageKey);

            final Role adminRole = roleAPI.loadCMSAdminRole();
            final List<String> usersId = roleAPI.findUsersForRole(adminRole)
                    .stream()
                    .map(user -> user.getUserId())
                    .collect(Collectors.toList());

            final SystemMessageBuilder messageBuilder = new SystemMessageBuilder()
                    .setMessage(message)
                    .setSeverity(MessageSeverity.ERROR)
                    .setType(MessageType.SIMPLE_MESSAGE)
                    .setLife(TimeUnit.SECONDS.toMillis(5));
            Logger.error(this.getClass(), message);
            systemMessageEventUtil.pushMessage(messageBuilder.create(), usersId);
        } catch (final  LanguageException | DotDataException | DotSecurityException e) {
            Logger.warn(ESReadOnlyMonitor.class, () -> e.getMessage());
        }
    }

    private void putCurrentIndicesToWriteMode() throws ElasticsearchResponseException {
        Logger.debug(this.getClass(), () -> "Trying to set the current indices to Write mode");
        ElasticsearchUtil.setLiveAndWorkingIndicesToWriteMode();
    }

    private void putClusterToWriteMode() throws ElasticsearchResponseException {
        Logger.debug(this.getClass(), () -> "Trying to set the cluster to Write mode");
        ElasticsearchUtil.setClusterToWriteMode();
    }

    private void startIndexMonitor() {
         schedule(
                 this::putCurrentIndicesToWriteMode,
                 ElasticsearchUtil::isEitherLiveOrWorkingIndicesReadOnly,
                 "es.index.write.allow.message"
         );
    }

    private synchronized void schedule(
            final PutRequestFunction putRequestFunction,
            final ReadOnlyCheckerFunction checkFunction,
            final String writeModeMessageKey) {

        DotConcurrentFactory.getInstance()
                .getSubmitter()
                .submit(
                        new MonitorRunnable(
                this, putRequestFunction, checkFunction, writeModeMessageKey)
                );
    }

    private void startClusterMonitor() {
        schedule(
                this::putClusterToWriteMode,
                ElasticsearchUtil::isClusterInReadOnlyMode,
                "es.cluster.write.allow.message"
        );
    }

    private synchronized void stop() {
        this.started.set(false);
    }

    public boolean isIndexOrClusterReadOnly() {
        return this.started.get();
    }

    private static class MonitorRunnable implements Runnable {
        private final PutRequestFunction putRequestFunction;
        private final ReadOnlyCheckerFunction readOnlyCheckerFunction;
        private final ESReadOnlyMonitor esReadOnlyMonitor;
        private final String writeModeMessageKey;

        MonitorRunnable(
                final ESReadOnlyMonitor esReadOnlyMonitor,
                final PutRequestFunction putRequestFunction,
                final ReadOnlyCheckerFunction readOnlyCheckerFunction,
                final String writeModeMessageKey) {

            this.putRequestFunction = putRequestFunction;
            this.readOnlyCheckerFunction = readOnlyCheckerFunction;
            this.esReadOnlyMonitor = esReadOnlyMonitor;
            this.writeModeMessageKey = writeModeMessageKey;
        }

        @Override
        public void run() {
            while(true) {
                try {

                    this.putRequestFunction.sendRequest();
                    Thread.sleep(esReadOnlyMonitor.timeToWaitAfterWriteModeSet);

                    if (!this.readOnlyCheckerFunction.isReadOnly()) {
                        esReadOnlyMonitor.sendMessage(this.writeModeMessageKey);
                        this.esReadOnlyMonitor.stop();
                        break;
                    }
                } catch (final ElasticsearchResponseException | InterruptedException e) {
                    Logger.info(ESReadOnlyMonitor.class, () -> e.getMessage());
                    TimeUnit.MINUTES.toMillis(INTERVAL_IN_MINUTES_TO_CHECK_READ_ONLY);
                }
            }
        }
    }

    @FunctionalInterface
    public interface PutRequestFunction {
        void sendRequest() throws ElasticsearchResponseException;
    }

    @FunctionalInterface
    public interface ReadOnlyCheckerFunction {
        boolean isReadOnly() throws ElasticsearchResponseException;
    }
}
