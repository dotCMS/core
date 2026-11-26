package com.dotmarketing.common.reindex;

import com.dotcms.contenttype.model.type.ContentType;
import com.liferay.portal.model.User;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.ConfigUtils;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jason Tesser
 * @since 1.7
 *
 */
public interface ReindexQueueAPI {

    /**
     * Will return only the reindex entries for the specific server the code is executed on This method
     * will also delete all entries from the table that are returned in the select
     * 
     * @return
     * @throws DotDataException
     */
    public Map<String, ReindexEntry> findContentToReindex() throws DotDataException;

    public Map<String, ReindexEntry> findContentToReindex(int numberOfRecords) throws DotDataException;

    /**
     * This method will add all content identifier for a structure to the index
     * 
     * @param contentType
     * @throws DotDataException
     */
    public void addStructureReindexEntries(ContentType contentType) throws DotDataException;

    /**
     * Will add reindex enteries for all content on all servers in the cluster including this one It
     * will also add an entry telling other servers to start building a new index
     * 
     * @throws DotDataException
     */
    public void addAllToReindexQueue() throws DotDataException;

    /**
     * Deletes the specific build new index entry for the local server. This is intended to be called
     * after a switch of the index.
     * 
     * @throws DotDataException
     */
    public void deleteReindexEntry(ReindexEntry ijournal) throws DotDataException;

    public void deleteReindexEntry(List<ReindexEntry> recordsToDelete) throws DotDataException;

    /**
     * Will find the number of records left to index
     * 
     * @return
     * @throws DotDataException
     */
    public long recordsInQueue() throws DotDataException;

    public long recordsInQueue(Connection conn) throws DotDataException;

    /**
     * Will determine if either the process table or journal table have any records left for the local
     * server to index
     * 
     * @return
     * @throws DotDataException
     */
    public boolean areRecordsLeftToIndex() throws DotDataException;

    /**
     * @return the serverId
     */
    default String getServerId() {
        return ConfigUtils.getServerId();
    }



    public void deleteReindexAndFailedRecords() throws DotDataException;

    /**
     * Deletes from dist_reindex_journal all failed records
     * @throws DotDataException
     */
    void deleteFailedRecords() throws DotDataException;

    /**
     * Reindexes content under a given host
     * 
     * @param host - Host object
     * @throws DotDataException
     */
    public void refreshContentUnderHost(Host host) throws DotDataException;

    /**
     * Reindexes content under a given folder
     * 
     * @param folder - Folder object
     * @throws DotDataException
     */
    public void refreshContentUnderFolder(Folder folder) throws DotDataException;

    /**
     * Reindexes content under a given folder path
     *
     * @param hostId
     * @param folderPath
     * @throws DotDataException
     */
    public void refreshContentUnderFolderPath(String hostId, String folderPath) throws DotDataException;

    /**
     * Adds an identifier to be reindexed
     * 
     * @param id {@link String} identifier
     * @throws DotDataException
     */
    void addIdentifierReindex(String id) throws DotDataException;

    /**
     * Adds an identifier to be reindexed with the highest priority
     * 
     * @param id {@link String} identifier
     * @throws DotDataException
     */
    void addReindexHighPriority(final String identifier) throws DotDataException;

    /**
     * Adds a list of identifiers to be reindexed
     * 
     * @param ids {@link Set} of identifier
     * @throws DotDataException
     */
    int addIdentifierReindex(final Collection<String> ids) throws DotDataException;

    /**
     * Adds a list of identifiers to be reindexed with the highest priority
     * 
     * @param ids {@link Set} of identifier
     * @throws DotDataException
     */
    int addReindexHighPriority(final Collection<String> ids) throws DotDataException;

    /**
     * Adds a contentlet to be reindexed
     * 
     * @param contentlet {@link Contentlet}
     * @throws DotDataException
     */
    void addContentletReindex(Contentlet contentlet) throws DotDataException;

    /**
     * Adds an {@link Identifier} to be reindexed
     * 
     * @param identifier {@link Identifier}
     * @throws DotDataException
     */
    void addIdentifierReindex(Identifier identifier) throws DotDataException;

    void addContentletsReindex(Collection<Contentlet> contentlet) throws DotDataException;

    void addIdentifierReindex(String identifier, int priority) throws DotDataException;


    /**
     * marks a ReindexEntry attempt as failed, increments the number of failures and records the cause
     * 
     * @param idx
     * @param cause
     * @throws DotDataException
     */
    void markAsFailed(ReindexEntry idx, String cause) throws DotDataException;

    /**
     * Counts the journal rows that gave up: their retries ran out and the reindex queue no longer
     * reads them. Unlike {@link #failedRecordCount()}, rows still being retried are not counted,
     * which is what a finished reindex reports as skipped (#37270).
     *
     * @return the number of parked rows
     * @throws DotDataException if the journal cannot be read
     */
    long parkedRecordCount() throws DotDataException;

    /**
     * Sends every parked journal row back to the queue, at the priority it was originally queued
     * at and with a fresh retry budget, so content that gave up is indexed again once the cause
     * has been fixed. A row whose content no longer exists comes back as a removal of its
     * documents. Callers must not run it during a full reindex: requeued reindex rows would land
     * in the band the reindex waits on before switching over (#37270).
     *
     * @param user the user who asked for the retry, recorded in the log
     * @return how many rows were requeued, and how many of them as removals
     * @throws DotDataException if the journal cannot be updated
     */
    ParkedRecordsRetry retryParkedRecords(User user) throws DotDataException;

    /**
     * Deletes the parked journal rows, the ones whose retries ran out, and logs who cleared them,
     * how many and which identifiers. Rows still being retried are kept. This is the operator's
     * clear (Maintenance and {@code DELETE /api/v1/esindex/failed}); a full reindex keeps using
     * {@link #deleteFailedRecords()} (#37270).
     *
     * @param user the user who cleared, recorded in the log
     * @return how many rows were deleted
     * @throws DotDataException if the journal cannot be updated
     */
    int clearParkedRecords(User user) throws DotDataException;

    /**
     * Lists the journal rows that failed at least once, parked or still being retried, whatever
     * band they were queued at. Describes the same rows as {@link #failedRecordCount()} (#37270).
     *
     * @return the failed rows, with their last failure reason
     * @throws DotDataException if the journal cannot be read
     */
    List<ReindexEntry> getFailedReindexRecords() throws DotDataException;

    /**
     * Adds identifiers to be wiped from all indexes by the ReindexThread Queue. All matching
     * identifiers will be wiped out regardless of langauge
     * 
     * @param ids
     * @return
     * @throws DotDataException
     */
    int addIdentifierDelete(Collection<String> ids) throws DotDataException;

    /**
     * Adds identifiers to be wiped from all indexes by the ReindexThread Queue. All matching
     * identifiers will be wiped out regardless of langauge
     * 
     * @param ids
     * @return
     * @throws DotDataException
     */
    int addIdentifierDelete(String id) throws DotDataException;

    
    void deleteReindexEntry(String identiferToDelete) throws DotDataException;

    /**
     * Counts the journal rows that failed at least once, parked or still being retried. Describes
     * the same rows as {@link #getFailedReindexRecords()} (#37270).
     *
     * @return the number of failed rows
     * @throws DotDataException if the journal cannot be read
     */
    long failedRecordCount() throws DotDataException;

    /**
     * This method specifically deletes reindex records in the queue - It does not include structure or
     * host reindex records
     * 
     * @throws DotDataException
     */
    void deleteReindexRecords() throws DotDataException;

    
    /**
     * This method specifically returns if there are reindex records in the queue - meaning a reindex
     * has been fired. It does not include structure or host reindex records
     * 
     * @throws DotDataException
     */
    boolean hasReindexRecords() throws DotDataException;

}
