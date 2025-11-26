/**
 * 
 */
package com.dotmarketing.common.reindex;

import com.dotcms.contenttype.model.type.ContentType;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.reindex.ReindexQueueFactory.Priority;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;

/**
 * @author Jason Tesser
 * @since 1.6.5c
 *
 */
public class ReindexQueueAPIImpl implements ReindexQueueAPI {

    private final ReindexQueueFactory reindexQueueFactory;

    public ReindexQueueAPIImpl() {
        this(FactoryLocator.getReindexQueueFactory());
    }

    @VisibleForTesting
    public ReindexQueueAPIImpl(final ReindexQueueFactory reindexQueueFactory) {
        this.reindexQueueFactory = reindexQueueFactory;
    }

    @Override
    @WrapInTransaction
    public void addStructureReindexEntries(final ContentType contentType) throws DotDataException {
        reindexQueueFactory.addStructureReindexEntries(contentType);
    }

    @Override
    @WrapInTransaction
    public synchronized void addAllToReindexQueue() throws DotDataException {
        reindexQueueFactory.addAllToReindexQueue();
    }

    @Override
    @CloseDBIfOpened
    public Map<String, ReindexEntry> findContentToReindex() throws DotDataException {
        return this.findContentToReindex(this.reindexQueueFactory.REINDEX_RECORDS_TO_FETCH);
    }

    @Override
    @CloseDBIfOpened
    public Map<String, ReindexEntry> findContentToReindex(final int recordsToReturn) throws DotDataException {
        return reindexQueueFactory.findContentToReindex(recordsToReturn);
    }

    @Override
    @WrapInTransaction
    public void deleteReindexEntry(ReindexEntry iJournal) throws DotDataException {
        reindexQueueFactory.deleteReindexEntry(iJournal);
    }

    @Override
    @CloseDBIfOpened
    public boolean areRecordsLeftToIndex() throws DotDataException {
        return reindexQueueFactory.areRecordsLeftToIndex();
    }

    @Override
    @CloseDBIfOpened
    public long recordsInQueue() throws DotDataException {
        return recordsInQueue(DbConnectionFactory.getConnection());
    }
    
    @Override
    @CloseDBIfOpened
    public long failedRecordCount() throws DotDataException {
        return reindexQueueFactory.failedRecordCount();
    }
    @CloseDBIfOpened
    @Override
    public boolean hasReindexRecords() throws DotDataException {
        return reindexQueueFactory.hasReindexRecords();
    }
    @CloseDBIfOpened
    @Override
    public long recordsInQueue(Connection conn) throws DotDataException {
        return reindexQueueFactory.recordsInQueue(conn);
    }

    @Override
    @WrapInTransaction
    public void deleteReindexAndFailedRecords() throws DotDataException {
        reindexQueueFactory.deleteReindexAndFailedRecords();
    }

    @Override
    @WrapInTransaction
    public void deleteReindexRecords() throws DotDataException {
        reindexQueueFactory.deleteReindexRecords();
    }
    
    
    @Override
    @WrapInTransaction
    public void deleteFailedRecords() throws DotDataException {
        reindexQueueFactory.deleteFailedRecords();
    }

    @Override
    @WrapInTransaction
    public void refreshContentUnderHost(Host host) throws DotDataException {
        reindexQueueFactory.refreshContentUnderHost(host);
    }

    @Override
    @WrapInTransaction
    public void refreshContentUnderFolder(Folder folder) throws DotDataException {
        reindexQueueFactory.refreshContentUnderFolder(folder);
    }

    @Override
    @WrapInTransaction
    public void refreshContentUnderFolderPath(String hostId, String folderPath) throws DotDataException {
        reindexQueueFactory.refreshContentUnderFolderPath(hostId, folderPath);
    }

    @Override
    @CloseDBIfOpened
    public List<ReindexEntry> getFailedReindexRecords() throws DotDataException {
        return reindexQueueFactory.getFailedReindexRecords();
    }

    @WrapInTransaction
    @Override
    public void addIdentifierReindex(final String id) throws DotDataException {

        Logger.info(this, "addIdentifierReindex: " + id);

        this.reindexQueueFactory.addIdentifierReindex(id);
    }

    @WrapInTransaction
    @Override
    public void addIdentifierReindex(final String identifier, int priority) throws DotDataException {

        this.reindexQueueFactory.addIdentifierReindex(identifier, priority);
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
    public int addIdentifierDelete(final Collection<String> ids) throws DotDataException {


        
        return this.reindexQueueFactory.addIdentifierDelete(ids,Priority.NORMAL.dbValue());
        
    }

    @CloseDBIfOpened /*This is a highly concurrent method, in order to avoid table locks we won't use here WrapInTransaction*/
    @Override
    public int addIdentifierDelete(final String id) throws DotDataException {

        return this.addIdentifierDelete(ImmutableList.of(id));
        
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

    @CloseDBIfOpened /*This is a highly concurrent method, in order to avoid table locks we won't use here WrapInTransaction*/
    @Override
    public void deleteReindexEntry(List<ReindexEntry> recordsToDelete) throws DotDataException {
        reindexQueueFactory.deleteReindexEntry(recordsToDelete);
    }

    @Override
    @WrapInTransaction
    public void deleteReindexEntry(String identiferToDelete) throws DotDataException {
        reindexQueueFactory.deleteReindexEntry(identiferToDelete);
    }

    @Override
    @WrapInTransaction
    public void markAsFailed(final ReindexEntry idx, final String cause) throws DotDataException {
        reindexQueueFactory.markAsFailed(idx, UtilMethods.shortenString(cause, 300));
    }

}
