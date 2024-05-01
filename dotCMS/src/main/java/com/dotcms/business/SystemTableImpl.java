package com.dotcms.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
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

        Logger.debug(this, ()-> "Saving or Updating the key: " + key + " value: " + value);
        Try.run(()-> this.systemTableFactory.saveOrUpdate(key, value))
                .getOrElseThrow((e)-> new DotRuntimeException(e.getMessage(), e));

        Try.run(()->HibernateUtil.addCommitListener(()->
                APILocator.getLocalSystemEventsAPI().asyncNotify(new SystemTableUpdatedKeyEvent(key))));
    }

    @Override
    @WrapInTransaction
    public void delete(String key) {

        Logger.debug(this, ()-> "Deleting the key: " + key);
        Try.run(()-> this.systemTableFactory.delete(key))
                .getOrElseThrow((e)-> new DotRuntimeException(e.getMessage(), e));

        Try.run(()->HibernateUtil.addCommitListener(()->
                APILocator.getLocalSystemEventsAPI().asyncNotify(new SystemTableUpdatedKeyEvent(key))));
    }
}
