package com.dotmarketing.quartz.job;

import com.dotcms.exception.ExceptionUtil;
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
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Performs the data replication from one Storage Provider to one or more Providers.
 * <p>Depending on the amount of data to be transferred and the number of destination Storage
 * Providers, this may be a heavy process. In order to overcome this situation, the Job replicates
 * small batches of data at a time and sleeps the process for a given amount of time before
 * proceeding with the next batch.</p>
 *
 * @author jsanca
 */
public class StorageReplicationJob extends DotStatefulJob {

    public static final String BATCH_SIZE_PROP = "storage.replication.batch-size";
    public static final String SLEEP_FOR_PROP = "storage.replication.sleep-for";

    private static final int BATCH_SIZE_DEFAULT = 100;
    private static final long SLEEP_FOR_DEFAULT = DateUtil.SECOND_MILLIS;

    private static final Lazy<Integer> BATCH_SIZE =
            Lazy.of(() -> Config.getIntProperty(BATCH_SIZE_PROP, BATCH_SIZE_DEFAULT));
    private static final Lazy<Long> SLEEP_FOR =
            Lazy.of(() -> Config.getLongProperty(SLEEP_FOR_PROP, SLEEP_FOR_DEFAULT));

    /**
     * Schedules the Storage Replication Quartz Job to be executed immediately.
     *
     * @param fromStorageType The storage type to replicate from.
     * @param storageTypes    The storage types to replicate to.
     * @param user            The {@link User} who triggered this job.
     */
    public static void triggerReplicationStoragesJob(final StorageType fromStorageType,
                                                     final List<StorageType> storageTypes,
                                                     final User user) {
        final Map<String, Serializable> nextExecutionData = Map.of(
                "fromStorageType", fromStorageType,
                "toStorageTypes", storageTypes instanceof Serializable ?
                                (Serializable) storageTypes : new ArrayList<>(storageTypes),
                "user", user);
        try {
            DotStatefulJob.enqueueTrigger(nextExecutionData, StorageReplicationJob.class);
            AdminLogger.log(StorageReplicationJob.class, "triggerJobImmediately",
                    String.format("Replication Storage '%s'", fromStorageType.name()));
        } catch (final ParseException | SchedulerException | ClassNotFoundException e) {
            final String errorMsg = String.format("Failed to schedule the ReplicateStoragesJob " +
                    "replicating from Storage Type '%s' to Types '%s': %s",
                    fromStorageType.name(), storageTypes, ExceptionUtil.getErrorMessage(e));
            Logger.error(StorageReplicationJob.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    /**
     * Makes sure that the specified group exists in all the given {@link StoragePersistenceAPI}
     * providers.
     *
     * @param toStoragePersistenceAPIs The list of {@link StoragePersistenceAPI} providers.
     * @param group                    The group name that must exist.
     *
     * @throws DotDataException An error occurred when checking or creating the group.
     */
    private static void forceGroupCreation(final List<StoragePersistenceAPI> toStoragePersistenceAPIs,
                                           final String group) throws DotDataException {
        for (final StoragePersistenceAPI toStoragePersistenceAPI : toStoragePersistenceAPIs) {
            if (!toStoragePersistenceAPI.existsGroup(group)) {
                toStoragePersistenceAPI.createGroup(group);
            }
        }
    } // ensureGroup.

    @Override
    @LogTime
    public void run(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        final Trigger trigger = jobExecutionContext.getTrigger();
        final Map<String, Serializable> map = getExecutionData(trigger, StorageReplicationJob.class);
        final StorageType fromStorageType = (StorageType) map.get("fromStorageType");
        final List<StorageType> toStorageTypes = (List<StorageType>) map.get("toStorageTypes");
        final User user = (User) map.get("user");
        Logger.info(this, "--------------------------------------");
        Logger.info(this, "ReplicateStoragesJob has started");
        Logger.info(this, String.format("-> Initiated by User: %s", user.getUserId()));
        Logger.info(this, String.format("-> From Storage Type: %s", fromStorageType.name()));
        Logger.info(this, String.format("-> To Storage Type(s): %s", toStorageTypes.toString()));
        Logger.info(this, String.format("-> Batch Size: %d", BATCH_SIZE.get()));
        Logger.info(this, String.format("-> Sleep For: %d millis", SLEEP_FOR.get()));
        Logger.info(this, "This process runs in the background and may take a long time. " +
                "Please wait...");
        replicate(fromStorageType, toStorageTypes);
        Logger.info(this.getClass(), String.format("ReplicateStoragesJob initiated by '%s' from " +
                "type '%s' to type(s) '%s' has finished!", user.getUserId(),
                fromStorageType.name(), toStorageTypes));
    } // run

    /**
     * Replicates the data from the specified {@link StorageType} to the given list of destination
     * Storage Types. The data to be replicated will be provided in batches, and a specific amount
     * of time will be given in order to start with the next batch.
     *
     * @param fromStorageType The source {@link StorageType}.
     * @param toStorageTypes  The list of destination {@link StorageType} inatances.
     */
    @VisibleForTesting
    public void replicate(final StorageType fromStorageType,
                          final List<StorageType> toStorageTypes) {
        try {
            final StoragePersistenceAPI fromStoragePersistenceAPI =
                    StoragePersistenceProvider.INSTANCE.get().getStorage(fromStorageType);
            final List<StoragePersistenceAPI> toStoragePersistenceAPIs = toStorageTypes.stream()
                    .map(type -> StoragePersistenceProvider.INSTANCE.get().getStorage(type))
                    .filter(Objects::nonNull).collect(Collectors.toList());
            if (!isInputDataValid(fromStorageType, toStorageTypes, fromStoragePersistenceAPI,
                    toStoragePersistenceAPIs)) {
                return;
            }
            final int batch = BATCH_SIZE.get();
            for (final String group : fromStoragePersistenceAPI.listGroups()) {
                forceGroupCreation(toStoragePersistenceAPIs, group);
                int count = 0;
                for (final ObjectPath objectPath : fromStoragePersistenceAPI.toIterable(group)) {
                    if (null != objectPath) {
                        this.replicateToStorages(toStoragePersistenceAPIs, group, objectPath);
                        if (count++ % batch == 0) {
                            DateUtil.sleep(SLEEP_FOR.get());
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            Logger.error(this, String.format("Failed to replicate Storage Type '%s': %s",
                    fromStorageType, ExceptionUtil.getErrorMessage(e)), e);
        }
    }

    /**
     * Utility method used to validate that the required data is present before performing the
     * metadata replication process.
     *
     * @param fromStorageType           The source {@link StorageType}.
     * @param toStorageTypes            The list of destination {@link StorageType} instances.
     * @param fromStoragePersistenceAPI The {@link StoragePersistenceAPI} instance for the source.
     * @param toStoragePersistenceAPIs  The list of {@link StoragePersistenceAPI} instances for the
     *                                  destination.
     *
     * @return If all required parameters are correct, returns {@code true}.
     */
    private boolean isInputDataValid(final StorageType fromStorageType,
                                     final List<StorageType> toStorageTypes,
                                     final StoragePersistenceAPI fromStoragePersistenceAPI,
                                     final List<StoragePersistenceAPI> toStoragePersistenceAPIs) {
        if (null == fromStoragePersistenceAPI) {
            Logger.error(this, String.format("Source Storage Type '%s' was not found",
                    fromStorageType));
            return false;
        }
        if (toStoragePersistenceAPIs.isEmpty()) {
            Logger.error(this, String.format("None of the destination Storage Types '%s' were" +
                    " found", toStorageTypes));
            return false;
        }
        if (toStoragePersistenceAPIs.size() != toStorageTypes.size()) {
            Logger.error(this, String.format("Only %d out of %d Storage Types were found: %s",
                    toStoragePersistenceAPIs.size(), toStorageTypes.size(),
                    toStoragePersistenceAPIs.stream().map(storage
                            -> storage.getClass().getSimpleName()).collect(Collectors.joining(", "
                    ))));
            return false;
        }
        try {
            if (UtilMethods.isNotSet(fromStoragePersistenceAPI.listGroups())) {
                Logger.error(this, String.format("Group list for Storage Provider '%s' is empty",
                        fromStoragePersistenceAPI.getClass().getSimpleName()));
                return false;
            }
        } catch (final DotDataException e) {
            Logger.error(this, String.format("Failed to retrieve group list for Storage Provider " +
                            "'%s': %s", fromStoragePersistenceAPI.getClass().getSimpleName(),
                    ExceptionUtil.getErrorMessage(e)));
            return false;
        }
        return true;
    }

    /**
     * Replicates the data of the specified {@link ObjectPath} to the given list of
     * {@link StoragePersistenceAPI} providers. The replication will take place even if such an
     * object already exists in the destination provider. This is intentional because we want to
     * make sure both source and destination storages are synchronized.
     *
     * @param toStoragePersistenceAPIs The list of {@link StoragePersistenceAPI} providers that will
     *                                 contain the replicated data.
     * @param group                    The group name that the {@link ObjectPath} belongs to.
     * @param objectPath               The {@link ObjectPath} containing the data to be replicated.
     */
    private void replicateToStorages(final List<StoragePersistenceAPI> toStoragePersistenceAPIs,
                                     final String group, final ObjectPath objectPath) {
        for (final StoragePersistenceAPI toStoragePersistenceAPI : toStoragePersistenceAPIs) {
            try {
                if (null != objectPath.getObject()) {
                    Logger.debug(this, String.format("-> Replicating object in path: '%s'",
                            objectPath.getPath()));
                    if (objectPath.getObject() instanceof File) {
                        toStoragePersistenceAPI.pushFile(group, objectPath.getPath(),
                                (File) objectPath.getObject(), null);
                    } else if (objectPath.getObject() instanceof Serializable) {
                        toStoragePersistenceAPI.pushObject(group, objectPath.getPath(),
                                FileStorageAPI.DEFAULT_OBJECT_WRITER_DELEGATE,
                                (Serializable) objectPath.getObject(), null);
                    } else {
                        Logger.warn(this, String.format("Object '%s' under group '%s' and path " +
                                "'%s' is NOT serializable", objectPath.getObject(), group,
                                objectPath.getPath()));
                    }
                } else {
                    Logger.warn(this, String.format("Object in path '%s' is null. It will NOT be " +
                            "replicated", objectPath.getPath()));
                }
            } catch (final Exception e) {
                Logger.error(this, String.format("Error replicating to Storage Provider '%s': %s"
                        , toStoragePersistenceAPI.getClass().getSimpleName(),
                        ExceptionUtil.getErrorMessage(e)), e);
            }
        }
    } // replicateToStorages.

}
