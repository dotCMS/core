/**
 * 
 */
package com.dotmarketing.common.reindex;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * @author Jason Tesser
 * @since 1.6.5c
 *
 */
public class ReindexQueueAPIImpl implements ReindexQueueAPI {

    private final ReindexQueueFactory reindexQueueFactory;

    public ReindexQueueAPIImpl() {
        this.reindexQueueFactory = (ReindexQueueFactory) FactoryLocator.getDistributedJournalFactory();
    }

    @WrapInTransaction
    public void addStructureReindexEntries(String structureInode) throws DotDataException {
        reindexQueueFactory.addStructureReindexEntries(structureInode);
    }

    @WrapInTransaction
    public synchronized void addAllToReindexQueue() throws DotDataException {
        reindexQueueFactory.addAllToReindexQueue();
    }

    @CloseDBIfOpened
    public Map<String,ReindexEntry> findContentToReindex() throws DotDataException {
        return this.findContentToReindex(this.reindexQueueFactory.REINDEX_RECORDS_TO_FETCH);
    }

    @CloseDBIfOpened
    public Map<String,ReindexEntry> findContentToReindex(final int recordsToReture) throws DotDataException {
        return reindexQueueFactory.findContentToReindex(recordsToReture);
    }

    @WrapInTransaction
    public void deleteReindexEntry(ReindexEntry iJournal) throws DotDataException {
        reindexQueueFactory.deleteReindexEntry(iJournal);
    }

    @CloseDBIfOpened
    public boolean areRecordsLeftToIndex() throws DotDataException {
        return reindexQueueFactory.areRecordsLeftToIndex();
    }

    @CloseDBIfOpened
    public long recordsInQueue() throws DotDataException {
        return recordsInQueue(DbConnectionFactory.getConnection());
    }

    public long recordsInQueue(Connection conn) throws DotDataException {
        return reindexQueueFactory.recordsInQueue(conn);
    }

    long lastTimeIRequedRecords = 0;

    @Override
    public void requeStaleReindexRecords(final int secondsOld) throws DotDataException {
        if(lastTimeIRequedRecords+(secondsOld*1000)<System.currentTimeMillis()) {
            reindexQueueFactory.requeStaleReindexRecords(secondsOld);
        }
    }

    @WrapInTransaction
    public void distReindexJournalCleanup(int time, boolean add, boolean includeInodeCheck, DateType type) throws DotDataException {
        reindexQueueFactory.distReindexJournalCleanup(time, add, includeInodeCheck, type);

    }

    @WrapInTransaction
    public void cleanDistReindexJournal() throws DotDataException {
        reindexQueueFactory.cleanDistReindexJournal();
    }

    @WrapInTransaction
    public void refreshContentUnderHost(Host host) throws DotDataException {
        reindexQueueFactory.refreshContentUnderHost(host);
    }

    @WrapInTransaction
    public void refreshContentUnderFolder(Folder folder) throws DotDataException {
        reindexQueueFactory.refreshContentUnderFolder(folder);
    }

    @WrapInTransaction
    public void refreshContentUnderFolderPath(String hostId, String folderPath) throws DotDataException {
        reindexQueueFactory.refreshContentUnderFolderPath(hostId, folderPath);
    }

    @WrapInTransaction
    @Override
    public void addIdentifierReindex(final String id) throws DotDataException {

        this.reindexQueueFactory.addIdentifierReindex(id);
    }
    
    @WrapInTransaction
    @Override
    public void addIdentifierReindex(final String id, int priority) throws DotDataException {

        this.reindexQueueFactory.addIdentifierReindex(id, priority);
    }

    @WrapInTransaction
    @Override
    public void addReindexHighPriority(final String identifier) throws DotDataException {

        this.reindexQueueFactory.addReindexHighPriority(identifier);
    }

    @WrapInTransaction
    @Override
    public int addIdentifierReindex(final Collection<String> ids) throws DotDataException {

        return this.reindexQueueFactory.addIdentifierReindex(ids);
    }

    @WrapInTransaction
    @Override
    public int addReindexHighPriority(final Collection<String> ids) throws DotDataException {

        return this.reindexQueueFactory.addReindexHighPriority(ids);
    }

    @WrapInTransaction
    @Override
    public void addContentletReindex(final Contentlet contentlet) throws DotDataException {

        this.reindexQueueFactory.addIdentifierReindex(contentlet.getIdentifier());
    }

    @WrapInTransaction
    @Override
    public void addContentletsReindex(final Collection<Contentlet> contentlet) throws DotDataException {
        contentlet.forEach(con -> {
            try {
                addIdentifierReindex(con.getIdentifier());
            } catch (DotDataException e) {
                Logger.warnAndDebug(this.getClass(), e);
            }

        });

    }

    @WrapInTransaction
    @Override
    public void addIdentifierReindex(final Identifier identifier) throws DotDataException {

        this.reindexQueueFactory.addIdentifierReindex(identifier.getId());
    }

    @WrapInTransaction
    public void deleteReindexEntry(Collection<ReindexEntry> recordsToDelete) throws DotDataException {
        reindexQueueFactory.deleteReindexEntry(recordsToDelete);
    }

    @WrapInTransaction
    public void resetServerForReindexEntry(Collection<ReindexEntry> recordsToModify) throws DotDataException {
        reindexQueueFactory.resetServerForReindexEntry(recordsToModify);
    }

    @Override
    @WrapInTransaction
    public void resetServersRecords() throws DotDataException {

        new DotConnect().setSQL("update dist_reindex_journal set serverid=null where serverid=?").addParam(ConfigUtils.getServerId())
                .loadResult();

    }
    
    @Override
    @WrapInTransaction
    public void updateIndexJournalPriority(long id, int priority) throws DotDataException{
        reindexQueueFactory.updateIndexJournalPriority(id,  priority);
    
    }

    @Override
    @WrapInTransaction
    public void markAsFailed(final ReindexEntry idx, final String cause) throws DotDataException{
        Logger.warn(this.getClass(), "Reindex failed for :" +idx + " because " + cause);
        reindexQueueFactory.markAsFailed(idx, UtilMethods.shortenString(cause, 300));
    
    }
    
    
    
}
