package com.dotcms.content.elasticsearch.business;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import io.vavr.control.Try;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Implementation of ElasticReadOnlyCommand
 * @author jsanca
 */
class ElasticReadOnlyCommandImpl implements ElasticReadOnlyCommand {

    private static final int TIME_TO_WAIT_AFTER_WRITE_MODE_SET_NOTY_VALUE = -1;
    public static final  int INTERVAL_IN_MINUTES_TO_CHECK_READ_ONLY       = 1;

    private final RoleAPI roleAPI;
    private final SystemMessageEventUtil systemMessageEventUtil;

    private final AtomicBoolean indexOrClusterReadOnly = new AtomicBoolean();

    @VisibleForTesting
    long timeToWaitAfterWriteModeSet = TIME_TO_WAIT_AFTER_WRITE_MODE_SET_NOTY_VALUE;

    private ElasticReadOnlyCommandImpl(
            final SystemMessageEventUtil systemMessageEventUtil,
            final RoleAPI roleAPI) {
        super();
        this.systemMessageEventUtil = systemMessageEventUtil;
        this.roleAPI = roleAPI;
        indexOrClusterReadOnly.set(false);
    }

    private ElasticReadOnlyCommandImpl() {
        this(
                SystemMessageEventUtil.getInstance(),
                APILocator.getRoleAPI()
        );
    }

    // Inner class to provide instance of class
    private static class Singleton {
        private static final ElasticReadOnlyCommandImpl INSTANCE = new ElasticReadOnlyCommandImpl();
    }

    @VisibleForTesting
    public static ElasticReadOnlyCommand getInstance(
            final SystemMessageEventUtil systemMessageEventUtil,
            final RoleAPI roleAPI) {

        return new ElasticReadOnlyCommandImpl(systemMessageEventUtil, roleAPI);
    }

    public static ElasticReadOnlyCommand getInstance() {
        return ElasticReadOnlyCommandImpl.Singleton.INSTANCE;
    }

    @Override
    public boolean isIndexOrClusterReadOnly() {
        return this.indexOrClusterReadOnly.get();
    }

    @Override
    public void sendReadOnlyMessage() {
        sendMessage("es.index.read.only.message");
    }

    @Override
    public void executeCheck() {

        if (timeToWaitAfterWriteModeSet == TIME_TO_WAIT_AFTER_WRITE_MODE_SET_NOTY_VALUE) {
            loadTimeToWaitAfterWriteModeSet();
        }

        final boolean clusterInReadOnlyMode              = Try.of(()-> ElasticsearchUtil.isClusterInReadOnlyMode())
                .onFailure(e->Logger.warn(ElasticReadOnlyCommand.class,  "unable to access ES Cluster Metadata: " + e.getMessage()))
                .getOrElse(true);
        final boolean eitherLiveOrWorkingIndicesReadOnly = Try.of(()-> ElasticsearchUtil.isEitherLiveOrWorkingIndicesReadOnly())
                .onFailure(e->Logger.warn(ElasticReadOnlyCommand.class,  "unable to access ES Index Metadata: " + e.getMessage()))
                .getOrElse(true);

        indexOrClusterReadOnly.set(true); // think it is ready only
        if (clusterInReadOnlyMode) {
            sendMessage("es.cluster.read.only.message");
            startMonitor(
                    this::putClusterToWriteMode,
                    ElasticsearchUtil::isClusterInReadOnlyMode,
                    "es.cluster.write.allow.message"
            );
        } else if (eitherLiveOrWorkingIndicesReadOnly) {
            sendMessage("es.index.read.only.message");
            startMonitor(
                    this::putCurrentIndicesToWriteMode,
                    ElasticsearchUtil::isEitherLiveOrWorkingIndicesReadOnly,
                    "es.index.write.allow.message"
            );
        } else {
            indexOrClusterReadOnly.set(false); // all ok
        }
    }

    private void loadTimeToWaitAfterWriteModeSet() {
        timeToWaitAfterWriteModeSet = ElasticsearchUtil.getClusterUpdateInterval() +
                TimeUnit.MINUTES.toMillis(INTERVAL_IN_MINUTES_TO_CHECK_READ_ONLY) +
                TimeUnit.SECONDS.toMillis(10);
    }

    private void startMonitor (final PutRequestFunction putRequestFunction,
                            final ReadOnlyCheckerFunction readOnlyCheckerFunction,
                            final String writeModeMessageKey) {

        try {

            putRequestFunction.sendRequest();
            Thread.sleep(timeToWaitAfterWriteModeSet);

            if (!readOnlyCheckerFunction.isReadOnly()) {
                sendMessage(writeModeMessageKey);
            } else {
                Logger.warn(this, ()->"ELASTIC INDEX still on read only mode");
            }
        } catch (final ElasticsearchResponseException | InterruptedException e) {

            Logger.info(ElasticReadOnlyCommandImpl.class, () -> e.getMessage());
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

    private void putCurrentIndicesToWriteMode() throws ElasticsearchResponseException {
        Logger.debug(this.getClass(), () -> "Trying to set the current indices to Write mode");
        ElasticsearchUtil.setLiveAndWorkingIndicesToWriteMode();
    }

    private void putClusterToWriteMode() throws ElasticsearchResponseException {
        Logger.debug(this.getClass(), () -> "Trying to set the cluster to Write mode");
        ElasticsearchUtil.setClusterToWriteMode();
    }

    private void sendMessage(final String messageKey) {
        try {
            final String message = LanguageUtil.get(messageKey);

            Logger.info(this, message);

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
            Logger.warn(this, () -> e.getMessage());
        }
    }
}
