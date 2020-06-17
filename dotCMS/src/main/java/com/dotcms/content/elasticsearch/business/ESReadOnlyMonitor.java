package com.dotcms.content.elasticsearch.business;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This monitor is run when either the Live or Working indices are in read-only mode.  It attempts to set both Live and Working indices to write mode again.
 * When setting write-mode fails it will retry after one minute.
 */
public class ESReadOnlyMonitor {
    private final RoleAPI roleAPI;
    private final SystemMessageEventUtil systemMessageEventUtil;

    private final AtomicBoolean started = new AtomicBoolean();
    private Timer timer;

    private ESReadOnlyMonitor(
            final SystemMessageEventUtil systemMessageEventUtil,
            final RoleAPI roleAPI) {
        super();
        this.systemMessageEventUtil = systemMessageEventUtil;
        this.roleAPI = roleAPI;
        started.set(false);
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
        if (started.compareAndSet(false, true)) {
            if (ElasticsearchUtil.isClusterInReadOnlyMode()) {
                ReindexThread.setCurrentIndexReadOnly(true);
                sendMessage("es.cluster.read.only.message");
                startClusterMonitor();
            } else if (ElasticsearchUtil.isEitherLiveOrWorkingIndicesReadOnly()) {
                ReindexThread.setCurrentIndexReadOnly(true);
                sendMessage("es.index.read.only.message");
                startIndexMonitor();
            } else {
                started.set(false);
            }

            return true;
        } else {
            return false;
        }
    }

    private void sendMessage(final String messageKey) {
        try {
            final Role adminRole = roleAPI.loadCMSAdminRole();
            final List<String> usersId = roleAPI.findUsersForRole(adminRole)
                    .stream()
                    .map(user -> user.getUserId())
                    .collect(Collectors.toList());

            final String message = LanguageUtil.get(messageKey);

            final SystemMessageBuilder messageBuilder = new SystemMessageBuilder()
                    .setMessage(message)
                    .setSeverity(MessageSeverity.ERROR)
                    .setType(MessageType.SIMPLE_MESSAGE)
                    .setLife(TimeUnit.SECONDS.toMillis(5));

            Logger.error(ESReadOnlyMonitor.class, message);
            systemMessageEventUtil.pushMessage(messageBuilder.create(), usersId);
        } catch (final LanguageException | DotDataException | DotSecurityException e) {
            Logger.warn(ESReadOnlyMonitor.class, () -> e.getMessage());
        }
    }

    private void putCurrentIndicesToWriteMode() {
        try {
            Logger.debug(this.getClass(), () -> "Trying to set the current indices to Write mode");
            ElasticsearchUtil.setLiveAndWorkingIndicesToWriteMode();
            sendMessage("es.index.write.allow.message");
            ReindexThread.setCurrentIndexReadOnly(false);

            this.stop();
        } catch (final ElasticsearchResponseException e) {
            Logger.info(ESReadOnlyMonitor.class, ()  -> e.getMessage());
        }
    }

    private void putClusterToWriteMode() {
        try{
            Logger.debug(this.getClass(), () -> "Trying to set the current indices to Write mode");
            ElasticsearchUtil.setClusterToWriteMode();
            sendMessage("es.cluster.write.allow.message");
            ReindexThread.setCurrentIndexReadOnly(false);

            this.stop();
        } catch (final ElasticsearchResponseException e) {
            Logger.info(ESReadOnlyMonitor.class, ()  -> e.getMessage());
        }
    }

    private void startIndexMonitor() {
         schedule(new IndexMonitorTimerTask(this));
    }

    private synchronized void schedule(final TimerTask timerTask) {
        timer = new Timer(true);
        timer.schedule(timerTask, 0, TimeUnit.MINUTES.toMillis(1));
    }

    private void startClusterMonitor() {
        schedule(new ClusterMonitorTimerTask(this));
    }

    private synchronized void stop() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
            this.started.set(false);
        }
    }

    private static class IndexMonitorTimerTask extends TimerTask{
        private final ESReadOnlyMonitor esReadOnlyMonitor;

        IndexMonitorTimerTask(final ESReadOnlyMonitor esReadOnlyMonitor) {
            this.esReadOnlyMonitor = esReadOnlyMonitor;
        }

        @Override
        public void run() {
            this.esReadOnlyMonitor.putCurrentIndicesToWriteMode();
        }
    }

    private static class ClusterMonitorTimerTask extends TimerTask{
        private final ESReadOnlyMonitor esReadOnlyMonitor;

        ClusterMonitorTimerTask(final ESReadOnlyMonitor esReadOnlyMonitor) {
            this.esReadOnlyMonitor = esReadOnlyMonitor;
        }

        @Override
        public void run() {
            this.esReadOnlyMonitor.putClusterToWriteMode();
        }
    }
}
