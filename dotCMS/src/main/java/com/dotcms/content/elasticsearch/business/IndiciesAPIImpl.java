package com.dotcms.content.elasticsearch.business;

import java.sql.Connection;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;


public class IndiciesAPIImpl implements IndiciesAPI {

    protected final IndiciesFactory ifac;

    public IndiciesAPIImpl(){
        ifac = FactoryLocator.getIndiciesFactory();
    }

    @CloseDBIfOpened
    public IndiciesInfo loadIndicies() throws DotDataException {
        return loadIndicies(null);
    }

    @CloseDBIfOpened
    public IndiciesInfo loadIndicies(Connection conn) throws DotDataException {
        return ifac.loadIndicies(conn);
    }

    @WrapInTransaction
    public synchronized void point(IndiciesInfo newInfo) throws DotDataException {
        ifac.point(newInfo);
    }

}
