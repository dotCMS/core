package com.dotmarketing.common.business.journal;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;

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
public interface DistributedJournalAPI {

    /**
     * Will return only the reindex entries for the specific server the code is executed on This method
     * will also delete all entries from the table that are returned in the select
     * 
     * @return
     * @throws DotDataException
     */
    public Map<String,IndexJournal>  findContentToReindex() throws DotDataException;


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
    public void deleteReindexEntry(IndexJournal ijournal) throws DotDataException;

    public void deleteReindexEntry(Collection<IndexJournal> recordsToDelete) throws DotDataException;

    /**
     * Resets the server id to NULL to a list of failed records, setting the server id to NULL for a
     * record in the dist_reindex_journal means the record will be added back to the queue of record to
     * process.
     *
     * @param recordsToModify
     * @throws DotDataException
     */
    public void resetServerForReindexEntry(Collection<IndexJournal> recordsToModify) throws DotDataException;

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
    public String getServerId();

    public enum DateType {
        DAY("DAY"), MINUTE("MINUTE");

        private String value;

        DateType(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }

        public static DateType getObject(String value) {
            DateType[] ojs = DateType.values();
            for (DateType oj : ojs) {
                if (oj.value.equals(value))
                    return oj;
            }
            return null;
        }
    };

    public void cleanDistReindexJournal() throws DotDataException;

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
     * Adds an {@link Identifier} to be reindex
     * 
     * @param identifier {@link Identifier}
     * @throws DotDataException
     */
    void addIdentifierReindex(Identifier identifier) throws DotDataException;

    void addContentletsReindex(Collection<Contentlet> contentlet) throws DotDataException;

    void resetServersRecords() throws DotDataException;

    void requeStaleReindexRecords(int secondsOld) throws DotDataException;

    void addIdentifierReindex(String id, int prority) throws DotDataException;

    void updateIndexJournalPriority(long id, int priority) throws DotDataException;

    void markAsFailed(IndexJournal idx, String cause) throws DotDataException;

}
