package com.dotmarketing.common.business.journal;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.common.business.journal.DistributedJournalAPI.DateType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;

/**
 * 
 * @author Jason Tesser
 * @since 1.6.5c
 */
public abstract class DistributedJournalFactory<T> {

	public static final int JOURNAL_TYPE_CONTENTENTINDEX = 1;
	public static final int JOURNAL_TYPE_CACHE = 2;
	public static final int REINDEX_JOURNAL_PRIORITY_NEWINDEX = 30;
	public static final int REINDEX_JOURNAL_PRIORITY_STRUCTURE_REINDEX = 20;
	public static final int REINDEX_JOURNAL_PRIORITY_CONTENT_CAN_WAIT_REINDEX = 15;
	public static final int REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX = 10;
	
	public static final int REINDEX_ACTION_REINDEX_OBJECT = 1;
	public static final int REINDEX_ACTION_DELETE_OBJECT = 2;
	
	protected T reindexJournalObjectToIndexNew;
	
	public DistributedJournalFactory(T newIndexValue) {
		reindexJournalObjectToIndexNew = newIndexValue;
	}
		
	/**
	 * Will return only the entries for the specific server the code is executed on
	 * @return
	 * @throws DotDataException
	 */
	protected abstract List<String> findCacheEntriesToRemove() throws DotDataException;
	
	/**
	 * Will add cache entries for all servers other then himself
	 * @param key
	 * @throws DotDataException
	 */
	protected abstract void addCacheEntry(String key, String group) throws DotDataException; 
	
	/**
	 * Will return only the reindex entries for the specific server the code is executed on
	 * @return
	 * @throws DotDataException
	 */
	protected abstract List<IndexJournal<T>> findContentReindexEntriesToReindex() throws DotDataException;
	
	/**
	 * Will delete all content reindex entries for a specific serverId less then the id passed in 
	 * @param serverId
	 * @param id
	 * @throws DotDataException
	 */
	protected abstract void deleteContentIndexEntries(String serverId, long id) throws DotDataException;
	
	/**
	 * Moves/process records from the dist_process table to the dist_journal table for all servers
	 * @throws DotDataException
	 */
	protected abstract void processJournalEntries() throws DotDataException;

	/**
	 * Return whether distribuited indexation is enabled (records are being inserted in the database)
	 * @return
	 */
	protected abstract boolean isIndexationEnabled();
	

	/**
 	* 
 	* Setsn whether distribuited indexation is enabled or not (records are being inserted in the database)
 	*/
	protected abstract void setIndexationEnabled(boolean indexationEnabled) ;

	/**
	 * Will add reindex enteries for all content on all servers in the cluster including this one
	 * It will also add an entry telling other servers to start building a new index
	 * @throws DotDataException
	 */
	protected abstract void addBuildNewIndexEntries() throws DotDataException;
	
	/**
	 * This method will add all content identifier for a structure to the index
	 * @param structureInode
	 * @throws DotDataException
	 */
	protected abstract void addStructureReindexEntries(T structureInode) throws DotDataException;

	/**
	 * Deletes the specific build new index entry for the local server.  
	 * This is intended to be called after a switch of the index.
	 * @throws DotDataException
	 */
	protected abstract void deleteReindexEntryForServer(IndexJournal<T> iJournal) throws DotDataException;
	protected abstract void deleteReindexEntryForServer(List<IndexJournal<T>> recordsToDelete) throws DotDataException;

	/**
	 * Will delete all records with the same identifer, serverid, and where they exist in the inode table
	 * @param ijournal
	 * @throws DotDataException
	 */
	protected abstract void deleteLikeJournalRecords(IndexJournal<T> ijournal) throws DotDataException;
	
	/**
	 * Will determine if either the process table or journal table have any records left for the local server to index
	 * @return
	 * @throws DotDataException
	 */
	protected abstract boolean areRecordsLeftToIndex() throws DotDataException;

	protected abstract long recordsLeftToIndexForServer() throws DotDataException;
	
	/**
	 * @return the serverId
	 */
	protected abstract String getServerId();
	
	/**
	 *  Deletes Records older or greater than d day(s), minutes(s) from dist_reindex_journal
	 * @param time
	 * @param add
	 * @param includeInodeCheck
	 * @param type
	 * @throws DotDataException
	 */
	protected abstract void distReindexJournalCleanup(int time, boolean add, boolean includeInodeCheck, DateType type) throws DotDataException;
	
	protected abstract void cleanDistReindexJournal() throws DotDataException;
	
	protected abstract List<IndexJournal> viewReindexJournalData() throws DotDataException;
	
	
	/**
	 * Reindexes content under a given host
	 * @param host
	 * @throws DotDataException
	 */
	protected abstract void refreshContentUnderHost(Host host) throws DotDataException;
	
	/**
	 * Reindexes content under a given folder
	 * @param folder 
	 * @throws DotDataException
	 */
	protected abstract void refreshContentUnderFolder(Folder folder) throws DotDataException;
    
}
