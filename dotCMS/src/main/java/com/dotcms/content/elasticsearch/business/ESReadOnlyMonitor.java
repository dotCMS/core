package com.dotcms.content.elasticsearch.business;

import com.dotcms.api.system.event.message.SystemMessageEventUtil;
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

/**
 * Run a Monitor If the current indices are in read only mode and try to set them to write mode again.
 * When the set write mode fail it try again after one minute.
 */
public class ESReadOnlyMonitor {
    private final RoleAPI roleAPI;
    private final SystemMessageEventUtil systemMessageEventUtil;

    private AtomicBoolean started;
    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    final Runnable runnable = () -> ESReadOnlyMonitor.this.putCurrentIndicesToWriteMode();

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
        Logger.info(this.getClass(), "Checking to Start " + reindexEntry);
        if (!started.compareAndSet(false, true)) {
            Logger.error(this.getClass(), "Reindex failed for :" + reindexEntry + " because " + cause);

            if (ESIndexUtil.isAnyCurrentIndicesReadOnly()) {
                ReindexThread.setCurrentIndexReadOnly(true);
                sendLargeMessage("es.index.read.only.message");
                startMonitor();
            } else {
                started.set(false);
            }
        }
    }

    private void sendLargeMessage(final String messageKey) {
        try {
            final Role adminRole = roleAPI.loadCMSAdminRole();
            final List<String> usersId = roleAPI.findUsersForRole(adminRole)
                    .stream()
                    .map(user -> user.getUserId())
                    .collect(Collectors.toList());

            systemMessageEventUtil.pushLargeMessage(LanguageUtil.get(messageKey), usersId);
        } catch (final LanguageException | DotDataException | DotSecurityException e) {
            Logger.warn(ESReadOnlyMonitor.class, () -> e.getMessage());
        }
    }

    private void putCurrentIndicesToWriteMode() {
        try {
            Logger.info(this.getClass(), "putCurrentIndicesToWriteMode");
            Logger.debug(this.getClass(), "Trying to set the current indices to Write mode");
            ESIndexUtil.putCurrentIndicesToWriteMode();
            sendLargeMessage("es.index.write.allow.message");
            ReindexThread.setCurrentIndexReadOnly(false);

            this.stop();
        } catch (final ESResponseException e) {
            Logger.debug(ESReadOnlyMonitor.class, ()  -> e.getMessage());
        }
    }

    private void startMonitor() {
        Logger.info(this.getClass(), "Starting Monitor");
        scheduledThreadPoolExecutor.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.MINUTES);
    }

    private void stop() {
       this.scheduledThreadPoolExecutor.shutdown();
        this.started.set(false);
    }
}
