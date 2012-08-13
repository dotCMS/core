package com.dotmarketing.common.business.journal;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;

/**
 * @author Jason Tesser
 * @since 1.7
 *
 */
public interface DistributedJournalAPI<T> {
	
	/**
	 * Will return only the entries for the specific server the code is executed on
	 * This method will also delete all entries from the table that are returned in the select 
	 * @return
	 * @throws DotDataException
	 */
	public List<String> findCacheEntriesToRemove() throws DotDataException;
	
	/**
	 * Will add cache entries for all servers other then himself in the process table
	 * @param key
	 * @param group
	 * @throws DotDataException
	 */
	public void addCacheEntry(String key, String group) throws DotDataException; 
	
	/**
	 * Will return only the reindex entries for the specific server the code is executed on
	 * This method will also delete all entries from the table that are returned in the select 
	 * @return 
	 * @throws DotDataException
	 */
	public List<IndexJournal<T>> findContentReindexEntriesToReindex() throws DotDataException;
	
	/**
	 * Moves/process records from the dist_process table to the dist_journal table for all servers
	 * @throws DotDataException
	 */
	public void processJournalEntries() throws DotDataException;
		
	/**
	 * This method will add all content identifier for a structure to the index
	 * @param structureInode
	 * @throws DotDataException
	 */
	public void addStructureReindexEntries(T structureInode) throws DotDataException;
	
	/**
	 * Will add reindex enteries for all content on all servers in the cluster including this one
	 * It will also add an entry telling other servers to start building a new index
	 * @throws DotDataException
	 */
	public void addBuildNewIndexEntries() throws DotDataException;
	
	/**
	 * Deletes the specific build new index entry for the local server.  
	 * This is intended to be called after a switch of the index.
	 * @throws DotDataException
	 */
	public void deleteReindexEntryForServer(IndexJournal<T> ijournal) throws DotDataException;
	public void deleteReindexEntryForServer(List<IndexJournal<T>> recordsToDelete) throws DotDataException;
	
	/**
	 * Will find the number of records left to index on this server
	 * @return
	 * @throws DotDataException
	 */
	public long recordsLeftToIndexForServer() throws DotDataException;
	public long recordsLeftToIndexForServer(Connection conn) throws DotDataException;
	
	/**
	 * Will determine if either the process table or journal table have any records left for the local server to index
	 * @return
	 * @throws DotDataException
	 */
	public boolean areRecordsLeftToIndex() throws DotDataException;
	
	/**
	 * Return whether distribuited indexation is enabled (records are being inserted in the database)
	 * @return
	 */
	public boolean isIndexationEnabled();
	
	/**
	 * Will delete all records with the same identifer, serverid, and where they exist in the inode table
	 * @param ijournal
	 * @throws DotDataException
	 */
	public void deleteLikeJournalRecords(IndexJournal<T> ijournal) throws DotDataException;

	/**
 	* 
 	* Sets whether distribuited indexation is enabled or not (records are being inserted in the database)
 	*/
	public void setIndexationEnabled(boolean indexationEnabled) ;

	/**
	 * @return the serverId
	 */
	public String getServerId();
	
	public enum DateType {
        DAY("DAY"),
        MINUTE("MINUTE");
        
        private String value;
        
        DateType (String value) {
            this.value = value;
        }
        
        public String toString () {
            return value;
        }
            
        public static DateType getObject (String value) {
            DateType[] ojs = DateType.values();
            for (DateType oj : ojs) {
                if (oj.value.equals(value))
                    return oj;
            }
            return null;
        }
    };
	
	/**
	 *  Deletes Records older or greater than d day(s), minutes(s) from dist_reindex_journal
	 * @param time
	 * @param add
	 * @param includeInodeCheck
	 * @param type
	 * @throws DotDataException
	 */
	public void distReindexJournalCleanup(int time, boolean add, boolean includeInodeCheck, DateType type) throws DotDataException;
	
	public void cleanDistReindexJournal() throws DotDataException;
	
	public List<IndexJournal> viewReindexJournalData() throws DotDataException;
	
	/**
	 * Reindexes content under a given host
	 * @param host - Host object
	 * @throws DotDataException
	 */
	public void refreshContentUnderHost(Host host) throws DotDataException;
	
	/**
	 * Reindexes content under a given folder
	 * @param folder - Folder object
	 * @throws DotDataException
	 */
	public void refreshContentUnderFolder(Folder folder) throws DotDataException;
	
}
