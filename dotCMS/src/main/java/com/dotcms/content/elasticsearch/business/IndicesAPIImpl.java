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
import java.util.Optional;


public class IndicesAPIImpl implements IndicesAPI {

    protected final IndicesFactory indicesFactory;

    public IndicesAPIImpl(){
        indicesFactory = FactoryLocator.getIndiciesFactory();
    }

    @CloseDBIfOpened
    public LegacyIndicesInfo loadLegacyIndices() throws DotDataException {
        return loadLegacyIndices(null);
    }

    @CloseDBIfOpened
    public LegacyIndicesInfo loadLegacyIndices(Connection conn) throws DotDataException {
        return indicesFactory.loadLegacyIndices(conn);
    }

    @CloseDBIfOpened
    public Optional<IndicesInfo> loadIndices() throws DotDataException{
        return indicesFactory.loadIndices();
    }

    @WrapInTransaction
    public synchronized void point(IndicesInfo newInfo) throws DotDataException {
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

        if(newInfo.isLegacy()) {
            Logger.info(this,
                    "Legacy Indices configuration updated by user: " + userInfo + " - " + indexInfo
                            + " at " + new java.util.Date());
        } else {
            Logger.info(this,
                    "New Indices configuration updated by user: " + userInfo + " - " + indexInfo
                            + " at " + new java.util.Date());
        }
        indicesFactory.point(newInfo);
    }

}
