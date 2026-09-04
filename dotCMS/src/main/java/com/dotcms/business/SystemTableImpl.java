package com.dotcms.business;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.util.ObfuscationUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import io.vavr.control.Try;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation for the SystemTable
 * @author jsanca
 */
class SystemTableImpl implements SystemTable {

    private final SystemTableFactory systemTableFactory;


    public SystemTableImpl() {

        this.systemTableFactory = FactoryLocator.getSystemTableFactory();
        this.initIfNeeded();
    }

    @WrapInTransaction
    public void initIfNeeded() {
        this.systemTableFactory.initIfNeeded();
    }

    @Override
    @CloseDBIfOpened
    public Optional<String> get(final String key) {

        try {

            Logger.debug(this, ()-> "Finding by key: " + key);
            final Optional<Object> objOpt = this.systemTableFactory.find(key);
            return objOpt.isPresent()? Optional.ofNullable(objOpt.get().toString()): Optional.empty();
        }catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @CloseDBIfOpened
    public Map<String, String> all() {

        try {

            final Map<String, Object> results = this.systemTableFactory.findAll();
            return Objects.nonNull(results)?
                    results.entrySet().stream().map(entry-> Map.entry(entry.getKey(), entry.getValue().toString()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)): Map.of();
        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @WrapInTransaction
    public void set(final String key, final String value) {

        SecurityLogger.logInfo(this.getClass(), "Saving system table value for key:" + key + "=" + ObfuscationUtil.obfuscateIfNeeded(
                key,value));

        Try.run(()-> this.systemTableFactory.saveOrUpdate(key, value))
                .getOrElseThrow((e)-> new DotRuntimeException(e.getMessage(), e));

        this.notifyKeyUpdatedOnCommit(key);
    }

    @Override
    @WrapInTransaction
    public void delete(final String key) {

        SecurityLogger.logInfo(this.getClass(), "Deleting system table key:" + key );
        Try.run(()-> this.systemTableFactory.delete(key))
                .getOrElseThrow((e)-> new DotRuntimeException(e.getMessage(), e));

        this.notifyKeyUpdatedOnCommit(key);
    }

    /**
     * Notifies, once the current transaction commits, that the value bound to {@code key} has changed.
     * The notification is sent twice on purpose:
     * <ul>
     *     <li>Locally, so subscribers on this node re-resolve the key right away.</li>
     *     <li>Cluster wide, so every other node re-resolves it too. Subscribers latch the resolved
     *     value in memory, so a node that never receives the event keeps the stale value for the
     *     lifetime of its JVM.</li>
     * </ul>
     * Both {@link #set(String, String)} and {@link #delete(String)} route through here; keeping a
     * single path is what prevents the two operations from drifting apart again.
     *
     * @param key The system table key whose value changed.
     */
    private void notifyKeyUpdatedOnCommit(final String key) {

        HibernateUtil.addCommitListenerNoThrow(()-> {

            final SystemTableUpdatedKeyEvent systemTableUpdatedKeyEvent = new SystemTableUpdatedKeyEvent(key);
            // first notify the local system events
            APILocator.getLocalSystemEventsAPI().asyncNotify(systemTableUpdatedKeyEvent);
            // then notify the cluster wide events
            Try.run(()->APILocator.getSystemEventsAPI()                     // CLUSTER WIDE
                    .push(SystemEventType.CLUSTER_WIDE_EVENT, new Payload(systemTableUpdatedKeyEvent)))
                    .onFailure(e -> Logger.error(SystemTableImpl.class,
                            "Could not publish the cluster wide event for the system table key: [" + key
                                    + "]. Other nodes will keep their previous value until restarted. "
                                    + e.getMessage(), e));
        });
    }
}
