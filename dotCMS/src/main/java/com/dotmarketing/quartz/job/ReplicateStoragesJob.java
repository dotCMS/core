package com.dotmarketing.quartz.job;

import com.dotcms.storage.FileStorageAPI;
import com.dotcms.storage.ObjectPath;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.storage.StorageType;
import com.dotcms.util.LogTime;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
                        "toStoragesType", toStoragesType instanceof Serializable? (Serializable) toStoragesType: new ArrayList<>(toStoragesType),
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

        try {

            final StoragePersistenceAPI storagePersistenceAPI = StoragePersistenceProvider.INSTANCE.get().getStorage(fromStorageType);
            final List<StoragePersistenceAPI> toStoragePersistenceAPIs = toStoragesType.stream()
                    .map(type -> StoragePersistenceProvider.INSTANCE.get().getStorage(type))
                    .filter(Objects::nonNull).collect(Collectors.toList());
            if (null != storagePersistenceAPI && !toStoragePersistenceAPIs.isEmpty()) {

                for (final String group: storagePersistenceAPI.listGroups()) {

                    ensureGroup(toStoragePersistenceAPIs, group);
                    int count = 0;
                    for (final ObjectPath objectPath : storagePersistenceAPI.toIterable(group)) {

                        this.replicateToStorages(toStoragePersistenceAPIs, group, objectPath);
                        if (count++ % 100 == 0) { // todo: both values may be configurable

                            DateUtil.sleep(DateUtil.SECOND_MILLIS);
                        }
                    }
                }
            } else {

                Logger.debug(this, () -> "The from storage type: " + fromStorageType.name() + " is not available or " +
                        " not any of the to storage types: " + toStoragesType + " are available");
            }
        } catch (DotDataException e) {
            Logger.error(this, "Error replicating storages: " + e.getMessage(), e);
        } catch (Throwable e) {
            Logger.error(this, "Error replicating storages", e);
        }
    } // run

    private static void ensureGroup(final List<StoragePersistenceAPI> toStoragePersistenceAPIs,
                                    final String group) throws DotDataException {

        for (final StoragePersistenceAPI toStoragePersistenceAPI : toStoragePersistenceAPIs) {

            if (!toStoragePersistenceAPI.existsGroup(group)) {
                toStoragePersistenceAPI.createGroup(group);
            }
        }
    } // ensureGroup.

    private void replicateToStorages(final List<StoragePersistenceAPI> toStoragePersistenceAPIs,
                                     final String group, final ObjectPath objectPath) {

        for (final StoragePersistenceAPI toStoragePersistenceAPI : toStoragePersistenceAPIs) {

            try {

                if (objectPath.getObject() instanceof Serializable) {

                    toStoragePersistenceAPI.pushObject(group, objectPath.getPath(),
                            FileStorageAPI.DEFAULT_OBJECT_WRITER_DELEGATE,
                            Serializable.class.cast(objectPath.getObject()), null);
                } else {

                    Logger.debug(this, () -> "The object: " + objectPath.getObject().toString() +
                            " under the group: " + group + " and path: " + objectPath.getPath() +
                            " is not serializable");
                }
            } catch (Exception e) {

                Logger.error(this, "Error replicating storages", e);
            }
        }
    } // replicateToStorages.
}
