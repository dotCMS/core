package com.dotmarketing.quartz.job;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.lock.IdentifierStripedLock;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.storage.ObjectPath;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.storage.StorageType;
import com.dotcms.util.LogTime;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import java.io.Serializable;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Does the replication from a storage to others
 * This may be a heavy process, so to become a slow task, it gets sleep between batches.
 * @author jsanca
 */
public class ReplicateStoragesJob extends DotStatefulJob {

    public ReplicateStoragesJob() {
    }

    /**
     * This method fires the job immediately
     * @param fromStorageType the storage type to replicate from
     * @param toStoragesType the storages type to replicate to
     * @param user the user who triggered the job
     */
    public static void triggerReplicationStoragesJob(final StorageType fromStorageType, final List<StorageType> toStoragesType,
                                                     final User user) {

        final Map<String, Serializable> nextExecutionData = ImmutableMap
                .of("fromStorageType", fromStorageType,
                        "toStoragesType", toStoragesType instanceof Serializable? (Serializable) toStoragesType: new ArrayList<>(toStoragesType)
                        "user", user);

        try {

            DotStatefulJob.enqueueTrigger(nextExecutionData, ReplicateStoragesJob.class);
        } catch (ParseException | SchedulerException | ClassNotFoundException e) {

            Logger.error(ReplicateStoragesJob.class, "Error scheduling DeleteUserJob", e);
            throw new DotRuntimeException("Error scheduling DeleteUserJob", e);
        }

        AdminLogger.log(ReplicateStoragesJob.class, "triggerJobImmediately",
            String.format("Replication Storage '%s'", fromStorageType.name()));

    }

    @Override
    @LogTime
    public void run(final JobExecutionContext jobExecutionContext) throws JobExecutionException {

        final Trigger trigger = jobExecutionContext.getTrigger();
        final Map<String, Serializable> map = getExecutionData(trigger, ReplicateStoragesJob.class);
        final StorageType fromStorageType = (StorageType) map.get("fromStorageType");
        final List<StorageType> toStoragesType = (List<StorageType>) map.get("toStoragesType");
        final User user = (User) map.get("user");

        Logger.debug(this, ()-> "Starting the replication from: " + fromStorageType.name()
                        + " to: " + toStoragesType.toString() + " by user: " + user.getUserId());
        // todo: we have to iterate from the StorageType N times and replicate in each other StorageType,
        // then sleep current thread between iterations.

        try {
            final StoragePersistenceAPI storagePersistenceAPI = StoragePersistenceProvider.INSTANCE.get().getStorage(fromStorageType);
            final List<StoragePersistenceAPI> toStoragePersistenceAPIs = new ArrayList<>();
            toStoragesType.stream().forEach(storageType ->
                    toStoragePersistenceAPIs.add(StoragePersistenceProvider.INSTANCE.get().getStorage(fromStorageType)));
            if (null != storagePersistenceAPI) {

                for (final String group: storagePersistenceAPI.listGroups()) {

                    for (final ObjectPath objectPath : storagePersistenceAPI.toIterable(group))

                        for (final StoragePersistenceAPI toStoragePersistenceAPI : toStoragePersistenceAPIs) {

                            try {

                                //toStoragePersistenceAPI.pushObject(group, object);
                            } catch (Exception e) {

                                Logger.error(this, "Error replicating storages", e);
                            }
                    }
                }

            } else {

                Logger.debug(this, () -> "The from storage type: " + fromStorageType.name() + " is not available");
            }
        } catch (DotDataException e) {
            Logger.error(this, "Error replicating storages", e);
        } catch (Throwable e) {
            Logger.error(this, "Error replicating storages", e);
        }
    } // run
}
