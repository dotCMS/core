package com.dotcms.content.elasticsearch.business;

import java.sql.Connection;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import io.vavr.control.Try;

/**
 * IMPORTANT: This Is marked Deprecated and will be removed once we complete migration to OpenSearch 3.x
 */
@Deprecated(forRemoval = true)
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
        final User currentUser = Try.of(() -> PortalUtil.getUser(HttpServletRequestThreadLocal.INSTANCE.getRequest()))
                .getOrNull();
        
        final String userInfo = (currentUser != null) 
            ? currentUser.getUserId() + " (" + currentUser.getEmailAddress() + ")"
            : "system user";
            
        final String indexInfo = String.format("working: %s, live: %s, reindex_working: %s, reindex_live: %s, site_search: %s", 
            newInfo.getWorking(), 
            newInfo.getLive(), 
            newInfo.getReindexWorking(), 
            newInfo.getReindexLive(), 
            newInfo.getSiteSearch());
            
        Logger.info(this, "Indices configuration updated by user: " + userInfo + " - " + indexInfo + " at " + new java.util.Date());
        
        ifac.point(newInfo);
    }

}
