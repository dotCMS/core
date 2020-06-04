package com.dotcms.content.elasticsearch.business;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.LicenseUtil;
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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * Run a Monitor If the current indices are in read only mode and try to set them to write mode again.
 * When the set write mode fail it try again after one minute.
 */
public class ESReadOnlyMonitor {
    private final RoleAPI roleAPI;
    private final SystemMessageEventUtil systemMessageEventUtil;

    private AtomicBoolean started = new AtomicBoolean();
    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    private Timer timer;

    private ESReadOnlyMonitor(final SystemMessageEventUtil systemMessageEventUtil, final RoleAPI roleAPI) {
        this.systemMessageEventUtil = systemMessageEventUtil;
        this.roleAPI = roleAPI;
        started.set(false);

        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        scheduledThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
    }

    private ESReadOnlyMonitor() {
        this(SystemMessageEventUtil.getInstance(), APILocator.getRoleAPI());
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

    public void start(final ReindexEntry reindexEntry, final String cause){
        if (started.compareAndSet(false, true)) {
            Logger.error(this.getClass(), Thread.currentThread().getName() + " - Reindex failed for :" + reindexEntry + " because " + cause);

            if (ESIndexUtil.isAnyCurrentIndicesReadOnly()) {
                ReindexThread.setCurrentIndexReadOnly(true);
                sendMessage("es.index.read.only.message");
                startMonitor();
            } else {
                started.set(false);
            }
        }
    }

    private void sendMessage(final String messageKey) {
        try {
            final Role adminRole = roleAPI.loadCMSAdminRole();
            final List<String> usersId = roleAPI.findUsersForRole(adminRole)
                    .stream()
                    .map(user -> user.getUserId())
                    .collect(Collectors.toList());

            final SystemMessageBuilder message = new SystemMessageBuilder()
                    .setMessage(LanguageUtil.get(messageKey))
                    .setSeverity(MessageSeverity.ERROR)
                    .setType(MessageType.SIMPLE_MESSAGE)
                    .setLife(TimeUnit.SECONDS.toMillis(5));

            systemMessageEventUtil.pushMessage(message.create(), usersId);
        } catch (final LanguageException | DotDataException | DotSecurityException e) {
            Logger.warn(ESReadOnlyMonitor.class, () -> e.getMessage());
        }
    }

    private void putCurrentIndicesToWriteMode() {
        try {
            Logger.debug(this.getClass(), "Trying to set the current indices to Write mode");
            ESIndexUtil.putCurrentIndicesToWriteMode();
            sendMessage("es.index.write.allow.message");
            ReindexThread.setCurrentIndexReadOnly(false);

            this.stop();
        } catch (final ESResponseException e) {
            Logger.info(ESReadOnlyMonitor.class, ()  -> e.getMessage());
        }
    }

    private void startMonitor() {
        timer = new Timer(true);
        timer.schedule(new MonitorTimerTask(this), 0, TimeUnit.MINUTES.toMillis(1));
    }

    private void stop() {
       this.timer.cancel();
       this.timer = null;
        this.started.set(false);
    }

    private static class MonitorTimerTask extends TimerTask{
        private final ESReadOnlyMonitor esReadOnlyMonitor;

        MonitorTimerTask(final ESReadOnlyMonitor esReadOnlyMonitor) {
            this.esReadOnlyMonitor = esReadOnlyMonitor;
        }

        @Override
        public void run() {
            this.esReadOnlyMonitor.putCurrentIndicesToWriteMode();
        }
    }
}
