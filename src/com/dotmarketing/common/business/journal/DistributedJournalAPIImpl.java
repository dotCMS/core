/**
 * 
 */
package com.dotmarketing.common.business.journal;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * @author Jason Tesser
 * @since 1.6.5c
 *
 */
public class DistributedJournalAPIImpl<T> implements DistributedJournalAPI<T> {

	private DistributedJournalFactory<T> distFac;
	
	public DistributedJournalAPIImpl() {
		this.distFac = (DistributedJournalFactory<T>)FactoryLocator.getDistributedJournalFactory();
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DistributedJournalAPI#addCacheEntry(java.lang.String)
	 */
	public void addCacheEntry(String key, String group) throws DotDataException {
		distFac.addCacheEntry(key, group);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DistributedJournalAPI#findCacheEntriesToRemove()
	 */
	public List<String> findCacheEntriesToRemove() throws DotDataException {
		return distFac.findCacheEntriesToRemove();
	}
	
	public void addStructureReindexEntries(T structureInode) throws DotDataException {
		distFac.addStructureReindexEntries(structureInode);
	}
	
	public synchronized void addBuildNewIndexEntries() throws DotDataException {
		distFac.addBuildNewIndexEntries();
	}
	
	public List<IndexJournal<T>> findContentReindexEntriesToReindex() throws DotDataException {
		return distFac.findContentReindexEntriesToReindex();
	}
	
	public void processJournalEntries() throws DotDataException {
		distFac.processJournalEntries();
	}
	
	public void deleteReindexEntryForServer(IndexJournal<T> iJournal) throws DotDataException {
		distFac.deleteReindexEntryForServer(iJournal);
	}

	public boolean isIndexationEnabled() {
		return isIndexationEnabled();
	}

	public void setIndexationEnabled(boolean indexationEnabled) {
		setIndexationEnabled(indexationEnabled);
	}
	
	public boolean areRecordsLeftToIndex() throws DotDataException {
		return distFac.areRecordsLeftToIndex();
	}

	public long recordsLeftToIndexForServer() throws DotDataException {
		return distFac.recordsLeftToIndexForServer();
	}
	
	public void deleteLikeJournalRecords(IndexJournal<T> ijournal) throws DotDataException {
		distFac.deleteLikeJournalRecords(ijournal);
	}
	
	public String getServerId(){
		return distFac.getServerId();
	}

	public void distReindexJournalCleanup(int time, boolean add, boolean includeInodeCheck, DateType type) throws DotDataException {
		distFac.distReindexJournalCleanup(time, add, includeInodeCheck, type);
		
	}

	public void cleanDistReindexJournal() throws DotDataException {
		distFac.cleanDistReindexJournal();
	}

	public List<IndexJournal> viewReindexJournalData() throws DotDataException {
		return distFac.viewReindexJournalData();
	}

	public void refreshContentUnderHost(Host host) throws DotDataException {
		distFac.refreshContentUnderHost(host);		
	}	
	
	public void refreshContentUnderFolder(Folder folder) throws DotDataException {
		distFac.refreshContentUnderFolder(folder);		
	}

    public void deleteReindexEntryForServer(List<IndexJournal<T>> recordsToDelete) throws DotDataException {
        distFac.deleteReindexEntryForServer(recordsToDelete);
    }
}
