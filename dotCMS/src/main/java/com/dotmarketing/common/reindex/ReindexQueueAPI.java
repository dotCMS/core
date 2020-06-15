package com.dotmarketing.common.reindex;

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
     * @param structureInode
     * @throws DotDataException
     */
    public void addStructureReindexEntries(String structureInode) throws DotDataException;

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
     * returns the list of all failed reindex records
     * 
     * @return
     * @throws DotDataException
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
     * returns if there are any records that have been marked as failed
     * @return
     * @throws DotDataException
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
