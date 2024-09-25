package com.dotcms.business;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.rest.api.v1.maintenance.JVMInfoResource;
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

        SecurityLogger.logInfo(this.getClass(), "Saving system table value for key:" + key + "=" + JVMInfoResource.obfuscateIfNeeded(
                key,value));

        Try.run(()-> this.systemTableFactory.saveOrUpdate(key, value))
                .getOrElseThrow((e)-> new DotRuntimeException(e.getMessage(), e));

        Try.run(()->HibernateUtil.addCommitListener(()->
                APILocator.getLocalSystemEventsAPI().asyncNotify(new SystemTableUpdatedKeyEvent(key))));
    }

    @Override
    @WrapInTransaction
    public void delete(String key) {

        SecurityLogger.logInfo(this.getClass(), "Deleting system table key:" + key );
        Try.run(()-> this.systemTableFactory.delete(key))
                .getOrElseThrow((e)-> new DotRuntimeException(e.getMessage(), e));

        Try.run(()->HibernateUtil.addCommitListener(()-> {

                    final SystemTableUpdatedKeyEvent systemTableUpdatedKeyEvent = new SystemTableUpdatedKeyEvent(key);
                    // first notify the local system events
                    APILocator.getLocalSystemEventsAPI().asyncNotify(systemTableUpdatedKeyEvent);
                    // then notify the cluster wide events
                    Try.run(()->APILocator.getSystemEventsAPI()					    // CLUSTER WIDE
                            .push(SystemEventType.CLUSTER_WIDE_EVENT, new Payload(systemTableUpdatedKeyEvent)))
                    .onFailure(e -> Logger.error(SystemTableImpl.class, e.getMessage()));
                }));
    }
}
