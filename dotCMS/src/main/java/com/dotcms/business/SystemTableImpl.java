package com.dotcms.business;

import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

import java.util.Map;
import java.util.Optional;

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

        Logger.debug(this, ()-> "Finding the key: " + key);
        return Try.of(()->this.systemTableFactory.find(key))
                .getOrElseThrow((e)-> new DotRuntimeException(e.getMessage(), e));
    }

    @Override
    @CloseDBIfOpened
    public Map<String, String> all() {

        Logger.debug(this, ()-> "Returning all table contents");
        return Try.of(()->this.systemTableFactory.findAll())
                .getOrElseThrow((e)-> new DotRuntimeException(e.getMessage(), e));
    }

    @Override
    @WrapInTransaction
    public void set(final String key, final String value) {

        Logger.debug(this, ()-> "Saving or Updating the key: " + key + " value: " + value);
        Try.run(()-> this.systemTableFactory.saveOrUpdate(key, value))
                .getOrElseThrow((e)-> new DotRuntimeException(e.getMessage(), e));
    }

    @Override
    @WrapInTransaction
    public void delete(String key) {

        Logger.debug(this, ()-> "Deleting the key: " + key);
        Try.run(()-> this.systemTableFactory.delete(key))
                .getOrElseThrow((e)-> new DotRuntimeException(e.getMessage(), e));
    }
}
